/*
 * Copyright (C) 2021 - 2022 Elytrium
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.elytrium.limboapi.server;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.network.Connections;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.packet.KeepAlive;
import com.velocitypowered.proxy.protocol.packet.PlayerListItem;
import com.velocitypowered.proxy.protocol.packet.PluginMessage;
import com.velocitypowered.proxy.protocol.packet.chat.LegacyChat;
import com.velocitypowered.proxy.protocol.packet.chat.PlayerChat;
import com.velocitypowered.proxy.protocol.packet.chat.PlayerCommand;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.timeout.ReadTimeoutHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import net.elytrium.java.commons.reflection.ReflectionException;
import net.elytrium.limboapi.LimboAPI;
import net.elytrium.limboapi.Settings;
import net.elytrium.limboapi.api.LimboSessionHandler;
import net.elytrium.limboapi.api.player.LimboPlayer;
import net.elytrium.limboapi.injection.login.LoginListener;
import net.elytrium.limboapi.injection.packet.PreparedPacketEncoder;
import net.elytrium.limboapi.protocol.LimboProtocol;
import net.elytrium.limboapi.protocol.packet.Player;
import net.elytrium.limboapi.protocol.packet.PlayerLook;
import net.elytrium.limboapi.protocol.packet.PlayerPosition;
import net.elytrium.limboapi.protocol.packet.PlayerPositionAndLook;
import net.elytrium.limboapi.protocol.packet.TeleportConfirm;

public class LimboSessionHandlerImpl implements MinecraftSessionHandler {

  private static final Method teardown;

  private final LimboAPI plugin;
  private final ConnectedPlayer player;
  private final LimboSessionHandler callback;
  private final MinecraftSessionHandler originalHandler;
  private final RegisteredServer previousServer;
  private final Supplier<String> limboName;

  private ScheduledTask keepAliveTask;
  private long keepAliveKey;
  private long keepAliveSentTime;
  private boolean keepAlivePending;
  private int ping;
  private int genericBytes;
  private boolean loaded;

  static {
    try {
      teardown = ConnectedPlayer.class.getDeclaredMethod("teardown");
      teardown.setAccessible(true);
    } catch (NoSuchMethodException e) {
      throw new ReflectionException(e);
    }
  }

  public LimboSessionHandlerImpl(LimboAPI plugin, ConnectedPlayer player, LimboSessionHandler callback,
      MinecraftSessionHandler originalHandler, RegisteredServer previousServer, Supplier<String> limboName) {
    this.plugin = plugin;
    this.player = player;
    this.callback = callback;
    this.originalHandler = originalHandler;
    this.previousServer = previousServer;
    this.limboName = limboName;
    this.loaded = player.getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_18_2) < 0;
  }

  public void onSpawn(LimboImpl server, LimboPlayer player) {
    this.loaded = true;
    this.callback.onSpawn(server, player);

    Integer serverReadTimeout = server.getReadTimeout();
    this.keepAliveTask = this.plugin.getServer().getScheduler().buildTask(this.plugin, () -> {
      MinecraftConnection connection = this.player.getConnection();
      if (this.keepAlivePending) {
        connection.closeWith(this.plugin.getPackets().getTimeOut());
        LimboAPI.getLogger().warn("{} was kicked due to keepalive timeout.", this.player);
      } else {
        this.keepAliveKey = ThreadLocalRandom.current().nextInt();
        KeepAlive keepAlive = new KeepAlive();
        keepAlive.setRandomId(this.keepAliveKey);
        connection.write(keepAlive);
        this.keepAliveSentTime = System.currentTimeMillis();
        this.keepAlivePending = true;
      }
    }).delay(250, TimeUnit.MILLISECONDS) // Fix 1.7.x race condition with switching protocol states.
      .repeat((serverReadTimeout == null ? this.plugin.getServer().getConfiguration().getReadTimeout() : serverReadTimeout) / 2, TimeUnit.MILLISECONDS)
      .schedule();
  }

  public boolean handle(Player packet) {
    if (this.loaded) {
      this.callback.onGround(packet.isOnGround());
    }

    return true;
  }

  public boolean handle(PlayerPosition packet) {
    if (this.loaded) {
      this.callback.onGround(packet.isOnGround());
      this.callback.onMove(packet.getX(), packet.getY(), packet.getZ());
    }

    return true;
  }

  public boolean handle(PlayerLook packet) {
    if (this.loaded) {
      this.callback.onGround(packet.isOnGround());
      this.callback.onRotate(packet.getYaw(), packet.getPitch());
    }

    return true;
  }

  public boolean handle(PlayerPositionAndLook packet) {
    if (this.loaded) {
      this.callback.onGround(packet.isOnGround());
      this.callback.onRotate(packet.getYaw(), packet.getPitch());
      this.callback.onMove(packet.getX(), packet.getY(), packet.getZ());
      this.callback.onMove(packet.getX(), packet.getY(), packet.getZ(), packet.getYaw(), packet.getPitch());
    }

    return true;
  }

  public boolean handle(TeleportConfirm packet) {
    if (this.loaded) {
      this.callback.onTeleport(packet.getTeleportId());
    }

    return true;
  }

  @Override
  public boolean handle(KeepAlive packet) {
    MinecraftConnection connection = this.player.getConnection();
    if (this.keepAlivePending) {
      if (packet.getRandomId() != this.keepAliveKey) {
        connection.closeWith(this.plugin.getPackets().getInvalidPing());
        LimboAPI.getLogger().warn("{} sent an invalid keepalive.", this.player);
        return false;
      } else {
        this.keepAlivePending = false;
        this.ping = (this.ping * 3 + (int) (System.currentTimeMillis() - this.keepAliveSentTime)) / 4;
        connection.write(
            new PlayerListItem(
                PlayerListItem.UPDATE_LATENCY,
                List.of(new PlayerListItem.Item(this.player.getUniqueId()).setLatency(this.ping))
            )
        );
        return true;
      }
    } else {
      connection.closeWith(this.plugin.getPackets().getInvalidPing());
      LimboAPI.getLogger().warn("{} sent an unexpected keepalive.", this.player);
      return false;
    }
  }

  @Override
  public boolean handle(LegacyChat packet) {
    return this.handleChat(packet.getMessage());
  }

  @Override
  public boolean handle(PlayerChat packet) {
    return this.handleChat(packet.getMessage());
  }

  @Override
  public boolean handle(PlayerCommand packet) {
    return this.handleChat("/" + packet.getCommand());
  }

  private boolean handleChat(String message) {
    int messageLength = message.length();
    if (messageLength > Settings.IMP.MAIN.MAX_CHAT_MESSAGE_LENGTH) {
      this.kickTooBigPacket("chat", messageLength);
      return true;
    }

    this.callback.onChat(message);
    return true;
  }

  @Override
  public void handleUnknown(ByteBuf packet) {
    int readableBytes = packet.readableBytes();
    if (readableBytes > Settings.IMP.MAIN.MAX_UNKNOWN_PACKET_LENGTH) {
      this.kickTooBigPacket("unknown", readableBytes);
    }
  }

  @Override
  public void handleGeneric(MinecraftPacket packet) {
    if (packet instanceof PluginMessage) {
      PluginMessage pluginMessage = (PluginMessage) packet;
      int singleLength = pluginMessage.content().readableBytes() + pluginMessage.getChannel().length() * 4;
      this.genericBytes += singleLength;
      if (singleLength > Settings.IMP.MAIN.MAX_SINGLE_GENERIC_PACKET_LENGTH) {
        this.kickTooBigPacket("generic (PluginMessage packet (custom payload)), single", singleLength);
        return;
      } else if (this.genericBytes > Settings.IMP.MAIN.MAX_MULTI_GENERIC_PACKET_LENGTH) {
        this.kickTooBigPacket("generic (PluginMessage packet (custom payload)), multi", this.genericBytes);
        return;
      }
    }

    this.callback.onGeneric(packet);
  }

  private void kickTooBigPacket(String type, int length) {
    this.player.getConnection().closeWith(this.plugin.getPackets().getTooBigPacket());
    LimboAPI.getLogger().warn("{} sent too big packet. (type: {}, length: {})", this.player, type, length);
  }

  @Override
  public void disconnected() {
    this.callback.onDisconnect();
    if (this.keepAliveTask != null) {
      this.keepAliveTask.cancel();
    }
    if (Settings.IMP.MAIN.LOGGING_ENABLED) {
      LimboAPI.getLogger().info(
          this.player.getUsername() + " (" + this.player.getRemoteAddress() + ") has disconnected from the " + this.limboName.get() + " Limbo"
      );
    }

    MinecraftConnection connection = this.player.getConnection();
    if (connection.isClosed()) {
      try {
        teardown.invoke(this.player);
      } catch (IllegalAccessException | InvocationTargetException e) {
        e.printStackTrace();
      }

      return;
    }

    if (!(LoginListener.LOGIN_CLASS.isInstance(this.originalHandler)) && !(this.originalHandler instanceof LimboSessionHandlerImpl)) {
      connection.eventLoop().execute(() -> connection.setSessionHandler(this.originalHandler));
    }
    ChannelPipeline pipeline = connection.getChannel().pipeline();
    if (pipeline.names().contains(LimboProtocol.PREPARED_ENCODER)) {
      pipeline.remove(PreparedPacketEncoder.class);
    }

    if (pipeline.names().contains(LimboProtocol.READ_TIMEOUT)) {
      pipeline.replace(
          LimboProtocol.READ_TIMEOUT,
          Connections.READ_TIMEOUT,
          new ReadTimeoutHandler(this.plugin.getServer().getConfiguration().getReadTimeout(), TimeUnit.MILLISECONDS)
      );
    }
  }

  public RegisteredServer getPreviousServer() {
    return this.previousServer;
  }

  public int getPing() {
    return this.ping;
  }
}

