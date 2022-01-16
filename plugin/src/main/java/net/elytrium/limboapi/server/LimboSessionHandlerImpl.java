/*
 * Copyright (C) 2021 Elytrium
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

import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.connection.client.LoginSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.packet.Chat;
import com.velocitypowered.proxy.protocol.packet.KeepAlive;
import com.velocitypowered.proxy.protocol.packet.PluginMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelPipeline;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import net.elytrium.limboapi.LimboAPI;
import net.elytrium.limboapi.Settings;
import net.elytrium.limboapi.api.LimboSessionHandler;
import net.elytrium.limboapi.api.player.LimboPlayer;
import net.elytrium.limboapi.injection.packet.PreparedPacketEncoder;
import net.elytrium.limboapi.protocol.packet.Player;
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

  private ScheduledTask keepAliveTask;
  private long keepAliveId;
  private long keepAliveLastSend;
  private long ping;
  private int genericBytes = 0;

  static {
    try {
      teardown = ConnectedPlayer.class.getDeclaredMethod("teardown");
      teardown.setAccessible(true);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
  }

  public LimboSessionHandlerImpl(LimboAPI plugin, ConnectedPlayer player, LimboSessionHandler callback,
      MinecraftSessionHandler originalHandler, RegisteredServer previousServer) {
    this.plugin = plugin;
    this.player = player;
    this.callback = callback;
    this.originalHandler = originalHandler;
    this.previousServer = previousServer;
  }

  public void onSpawn(LimboImpl server, LimboPlayer player) {
    this.callback.onSpawn(server, player);

    this.keepAliveTask = this.plugin.getServer().getScheduler().buildTask(this.plugin, () -> {
      KeepAlive keepAlive = new KeepAlive();
      keepAlive.setRandomId(this.keepAliveId = ThreadLocalRandom.current().nextInt());
      this.player.getConnection().write(keepAlive);
      this.keepAliveLastSend = System.currentTimeMillis();
    }).delay(250, TimeUnit.MILLISECONDS) // Fix 1.7.x race condition with switching protocol states.
      .repeat(this.plugin.getServer().getConfiguration().getReadTimeout() / 2, TimeUnit.MILLISECONDS)
      .schedule();
  }

  public boolean handle(Player packet) {
    this.callback.onGround(packet.isOnGround());
    return true;
  }

  public boolean handle(PlayerPosition packet) {
    this.callback.onGround(packet.isOnGround());
    this.callback.onMove(packet.getX(), packet.getY(), packet.getZ());
    return true;
  }

  public boolean handle(PlayerPositionAndLook packet) {
    this.callback.onGround(packet.isOnGround());
    this.callback.onRotate(packet.getYaw(), packet.getPitch());
    this.callback.onMove(packet.getX(), packet.getY(), packet.getZ());
    this.callback.onMove(packet.getX(), packet.getY(), packet.getZ(), packet.getYaw(), packet.getPitch());
    return true;
  }

  public boolean handle(TeleportConfirm packet) {
    this.callback.onTeleport(packet.getTeleportId());
    return true;
  }

  // TODO: Проверять отправил ли клиент его в течении 5 секунд, и вообще проверять отправляет ли он пакеты
  @Override
  public boolean handle(KeepAlive packet) {
    if (!(packet.getRandomId() == this.keepAliveId)) {
      this.player.getConnection().closeWith(this.plugin.getPackets().getInvalidPing());
      this.plugin.getLogger().warn("{} sent an invalid keepalive.", this.player);
      return false;
    }

    this.ping = System.currentTimeMillis() - this.keepAliveLastSend;
    return true;
  }

  @Override
  public boolean handle(Chat packet) {
    String message = packet.getMessage();
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
    this.plugin.getLogger().warn("{} sent too big packet. (type: {}, length: {})", this.player, type, length);
  }

  @Override
  public void disconnected() {
    this.callback.onDisconnect();
    this.keepAliveTask.cancel();
    if (Settings.IMP.MAIN.LOGGING_ENABLED) {
      this.plugin.getLogger().info(
          this.player.getUsername() + " (" + this.player.getRemoteAddress() + ") has disconnected from the "
              + this.callback.getClass().getSimpleName() + " Limbo"
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

    if (!(this.originalHandler instanceof LoginSessionHandler) && !(this.originalHandler instanceof LimboSessionHandlerImpl)) {
      connection.eventLoop().execute(() -> connection.setSessionHandler(this.originalHandler));
    }
    ChannelPipeline pipeline = connection.getChannel().pipeline();
    if (pipeline.names().contains("prepared-encoder")) {
      pipeline.remove(PreparedPacketEncoder.class);
    }
  }

  public RegisteredServer getPreviousServer() {
    return this.previousServer;
  }

  public long getPing() {
    return this.ping;
  }
}
