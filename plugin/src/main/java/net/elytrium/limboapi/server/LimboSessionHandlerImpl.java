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
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.connection.client.LoginSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.packet.Chat;
import com.velocitypowered.proxy.protocol.packet.PluginMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelPipeline;
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

  private final LimboAPI limboAPI;
  private final ConnectedPlayer player;
  private final LimboSessionHandler callback;
  private final MinecraftSessionHandler originalHandler;
  private RegisteredServer previousServer;

  private int genericBytes = 0;

  public LimboSessionHandlerImpl(LimboAPI limboAPI, ConnectedPlayer player, LimboSessionHandler callback, MinecraftSessionHandler originalHandler) {
    this.limboAPI = limboAPI;
    this.player = player;
    this.callback = callback;
    this.originalHandler = originalHandler;

    this.player.getCurrentServer().ifPresent(e -> this.previousServer = e.getServer());
  }

  public void onSpawn(LimboImpl server, LimboPlayer player) {
    this.callback.onSpawn(server, player);
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
    return true;
  }

  public boolean handle(TeleportConfirm packet) {
    this.callback.onTeleport(packet.getTeleportId());
    return true;
  }

  @Override
  public boolean handle(Chat packet) {
    String message = packet.getMessage();
    if (message.length() > Settings.IMP.MAIN.MAX_CHAT_MESSAGE_LENGTH) {
      this.kickTooBigPacket("chat");
      return true;
    }

    this.callback.onChat(message);
    return true;
  }

  @Override
  public void handleUnknown(ByteBuf packet) {
    if (packet.readableBytes() > Settings.IMP.MAIN.MAX_UNKNOWN_PACKET_LENGTH) {
      this.kickTooBigPacket("unknown");
    }
  }

  @Override
  public void handleGeneric(MinecraftPacket packet) {
    if (packet instanceof PluginMessage) {
      PluginMessage pluginMessage = (PluginMessage) packet;
      int singleLength = pluginMessage.content().readableBytes() + pluginMessage.getChannel().length() * 4;
      this.genericBytes += singleLength;
      if (singleLength > Settings.IMP.MAIN.MAX_SINGLE_GENERIC_PACKET_LENGTH || this.genericBytes > Settings.IMP.MAIN.MAX_MULTI_GENERIC_PACKET_LENGTH) {
        this.kickTooBigPacket("generic");
        return;
      }
    }

    this.callback.onGeneric(packet);
  }

  private void kickTooBigPacket(String type) {
    this.player.getConnection().closeWith(this.limboAPI.getPackets().getTooBigPacket());
    this.limboAPI.getLogger().warn("{} sent too big packet ({})", this.player, type);
  }

  @Override
  public void disconnected() {
    this.callback.onDisconnect();
    if (Settings.IMP.MAIN.LOGGING_ENABLED) {
      this.limboAPI.getLogger().info(
          this.player.getUsername() + " (" + this.player.getRemoteAddress() + ") has disconnected from the "
              + this.callback.getClass().getSimpleName() + " Limbo"
      );
    }

    if (!(this.originalHandler instanceof LoginSessionHandler) && !(this.originalHandler instanceof LimboSessionHandlerImpl)) {
      this.player.getConnection().setSessionHandler(this.originalHandler);
    }
    ChannelPipeline pipeline = this.player.getConnection().getChannel().pipeline();
    if (pipeline.names().contains("prepared-encoder")) {
      pipeline.remove(PreparedPacketEncoder.class);
    }
  }

  public ConnectedPlayer getPlayer() {
    return this.player;
  }

  public RegisteredServer getPreviousServer() {
    return this.previousServer;
  }
}
