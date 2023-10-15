/*
 * Copyright (C) 2021 - 2023 Elytrium
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
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.client.AuthSessionHandler;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.network.Connections;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.packet.KeepAlive;
import com.velocitypowered.proxy.protocol.packet.PluginMessage;
import com.velocitypowered.proxy.protocol.packet.chat.keyed.KeyedPlayerChat;
import com.velocitypowered.proxy.protocol.packet.chat.keyed.KeyedPlayerCommand;
import com.velocitypowered.proxy.protocol.packet.chat.legacy.LegacyChat;
import com.velocitypowered.proxy.protocol.packet.chat.session.SessionPlayerChat;
import com.velocitypowered.proxy.protocol.packet.chat.session.SessionPlayerCommand;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.timeout.ReadTimeoutHandler;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import net.elytrium.commons.utils.reflection.ReflectionException;
import net.elytrium.limboapi.LimboAPI;
import net.elytrium.limboapi.Settings;
import net.elytrium.limboapi.api.LimboSessionHandler;
import net.elytrium.limboapi.api.player.LimboPlayer;
import net.elytrium.limboapi.protocol.LimboProtocol;
import net.elytrium.limboapi.protocol.packets.c2s.MoveOnGroundOnlyPacket;
import net.elytrium.limboapi.protocol.packets.c2s.MovePacket;
import net.elytrium.limboapi.protocol.packets.c2s.MovePositionOnlyPacket;
import net.elytrium.limboapi.protocol.packets.c2s.MoveRotationOnlyPacket;
import net.elytrium.limboapi.protocol.packets.c2s.TeleportConfirmPacket;

public class LimboSessionHandlerImpl implements MinecraftSessionHandler {

  private static final MethodHandle TEARDOWN_METHOD;

  private final LimboAPI plugin;
  private final LimboImpl limbo;
  private final ConnectedPlayer player;
  private final LimboSessionHandler callback;
  private final MinecraftSessionHandler originalHandler;
  private final RegisteredServer previousServer;
  private final Supplier<String> limboName;

  private ScheduledFuture<?> keepAliveTask;
  private long keepAliveKey;
  private boolean keepAlivePending;
  private long keepAliveSentTime;
  private int ping = -1;
  private int genericBytes;
  private boolean loaded;
  //private boolean disconnected;

  public LimboSessionHandlerImpl(LimboAPI plugin, LimboImpl limbo, ConnectedPlayer player, LimboSessionHandler callback,
      MinecraftSessionHandler originalHandler, RegisteredServer previousServer, Supplier<String> limboName) {
    this.plugin = plugin;
    this.limbo = limbo;
    this.player = player;
    this.callback = callback;
    this.originalHandler = originalHandler;
    this.previousServer = previousServer;
    this.limboName = limboName;
    this.loaded = player.getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_18_2) < 0;
  }

  public void onSpawn(LimboPlayer player) {
    this.loaded = true;
    this.callback.onSpawn(this.limbo, player);

    Integer serverReadTimeout = this.limbo.getReadTimeout();
    this.keepAliveTask = player.getScheduledExecutor().scheduleAtFixedRate(() -> {
      MinecraftConnection connection = this.player.getConnection();

      // Sometimes there is a bug where player is kicked from the proxy by
      // the limbo and this task is not cancelled in LimboSessionHandlerImpl#disconnected
      // More info in issue #73
      if (connection.isClosed()) {
        this.keepAliveTask.cancel(true);
        return;
      }

      if (this.keepAlivePending) {
        connection.closeWith(this.plugin.getPackets().getTimeOut());
        if (Settings.IMP.MAIN.LOGGING_ENABLED) {
          LimboAPI.getLogger().warn("{} was kicked due to keepalive timeout.", this.player);
        }
      } else {
        this.keepAliveKey = ThreadLocalRandom.current().nextInt();
        KeepAlive keepAlive = new KeepAlive();
        keepAlive.setRandomId(this.keepAliveKey);
        connection.write(keepAlive);
        this.keepAlivePending = true;
        this.keepAliveSentTime = System.currentTimeMillis();
      }
    }, 250, (serverReadTimeout == null ? this.plugin.getServer().getConfiguration().getReadTimeout() : serverReadTimeout) / 2, TimeUnit.MILLISECONDS);
  }

  public boolean handle(MovePacket packet) {
    if (this.loaded) {
      this.callback.onGround(packet.isOnGround());
      this.callback.onMove(packet.getX(), packet.getY(), packet.getZ());
      this.callback.onMove(packet.getX(), packet.getY(), packet.getZ(), packet.getYaw(), packet.getPitch());
      this.callback.onRotate(packet.getYaw(), packet.getPitch());
    }

    return true;
  }

  public boolean handle(MovePositionOnlyPacket packet) {
    if (this.loaded) {
      this.callback.onGround(packet.isOnGround());
      this.callback.onMove(packet.getX(), packet.getY(), packet.getZ());
    }

    return true;
  }

  public boolean handle(MoveRotationOnlyPacket packet) {
    if (this.loaded) {
      this.callback.onGround(packet.isOnGround());
      this.callback.onRotate(packet.getYaw(), packet.getPitch());
    }

    return true;
  }

  public boolean handle(MoveOnGroundOnlyPacket packet) {
    if (this.loaded) {
      this.callback.onGround(packet.isOnGround());
    }

    return true;
  }

  public boolean handle(TeleportConfirmPacket packet) {
    if (this.loaded) {
      this.callback.onTeleport(packet.getTeleportID());
    }

    return true;
  }

  @Override
  public boolean handle(KeepAlive packet) {
    MinecraftConnection connection = this.player.getConnection();
    if (this.keepAlivePending) {
      if (packet.getRandomId() != this.keepAliveKey) {
        connection.closeWith(this.plugin.getPackets().getInvalidPing());
        if (Settings.IMP.MAIN.LOGGING_ENABLED) {
          LimboAPI.getLogger().warn("{} sent an invalid keepalive.", this.player);
        }
        return false;
      } else {
        this.keepAlivePending = false;
        int currentPing = (int) (System.currentTimeMillis() - this.keepAliveSentTime);
        if (this.ping == -1) {
          this.ping = currentPing;
        } else {
          this.ping = (this.ping * 3 + currentPing) / 4;
        }
        return true;
      }
    } else {
      connection.closeWith(this.plugin.getPackets().getInvalidPing());

      if (Settings.IMP.MAIN.LOGGING_ENABLED) {
        LimboAPI.getLogger().warn("{} sent an unexpected keepalive.", this.player);
      }
      return false;
    }
  }

  @Override
  public boolean handle(LegacyChat packet) {
    return this.handleChat(packet.getMessage());
  }

  @Override
  public boolean handle(KeyedPlayerChat packet) {
    return this.handleChat(packet.getMessage());
  }

  @Override
  public boolean handle(KeyedPlayerCommand packet) {
    return this.handleChat("/" + packet.getCommand());
  }

  @Override
  public boolean handle(SessionPlayerChat packet) {
    return this.handleChat(packet.getMessage());
  }

  @Override
  public boolean handle(SessionPlayerCommand packet) {
    return this.handleChat("/" + packet.getCommand());
  }

  private boolean handleChat(String message) {
    int messageLength = message.length();
    if (messageLength > Settings.IMP.MAIN.MAX_CHAT_MESSAGE_LENGTH) {
      this.kickTooBigPacket("chat", messageLength);
    } else {
      this.callback.onChat(message);
    }

    return true;
  }

  @Override
  public void handleUnknown(ByteBuf packet) {
    int readableBytes = packet.readableBytes();
    this.genericBytes += readableBytes;
    if (readableBytes > Settings.IMP.MAIN.MAX_UNKNOWN_PACKET_LENGTH) {
      this.kickTooBigPacket("unknown", readableBytes);
    } else if (this.genericBytes > Settings.IMP.MAIN.MAX_MULTI_GENERIC_PACKET_LENGTH) {
      this.kickTooBigPacket("unknown, multi", this.genericBytes);
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

    if (Settings.IMP.MAIN.LOGGING_ENABLED) {
      LimboAPI.getLogger().warn("{} sent too big packet. (type: {}, length: {})", this.player, type, length);
    }
  }

  @Override
  public void disconnected() {
    //this.disconnected = true;
    if (this.keepAliveTask != null) {
      this.keepAliveTask.cancel(true);
    }

    this.limbo.onDisconnect();
    this.callback.onDisconnect();

    if (Settings.IMP.MAIN.LOGGING_ENABLED) {
      LimboAPI.getLogger().info(
          "{} ({}) has disconnected from the {} Limbo", this.player.getUsername(), this.player.getRemoteAddress(), this.limboName.get()
      );
    }

    MinecraftConnection connection = this.player.getConnection();
    if (connection.isClosed()) {
      try {
        TEARDOWN_METHOD.invokeExact(this.player);
      } catch (Throwable e) {
        throw new ReflectionException(e);
      }

      return;
    }

    if (!(this.originalHandler instanceof AuthSessionHandler) && !(this.originalHandler instanceof LimboSessionHandlerImpl)) {
      connection.eventLoop().execute(() -> connection.setActiveSessionHandler(connection.getState(), this.originalHandler));
    }

    ChannelPipeline pipeline = connection.getChannel().pipeline();

    if (pipeline.get(LimboProtocol.READ_TIMEOUT) != null) {
      pipeline.replace(LimboProtocol.READ_TIMEOUT, Connections.READ_TIMEOUT,
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

  static {
    try {
      TEARDOWN_METHOD = MethodHandles.privateLookupIn(ConnectedPlayer.class, MethodHandles.lookup())
          .findVirtual(ConnectedPlayer.class, "teardown", MethodType.methodType(void.class));
    } catch (NoSuchMethodException | IllegalAccessException e) {
      throw new ReflectionException(e);
    }
  }
}
