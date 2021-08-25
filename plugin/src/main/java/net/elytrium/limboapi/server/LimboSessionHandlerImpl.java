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
import com.velocitypowered.proxy.protocol.packet.Chat;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelPipeline;
import lombok.Getter;
import net.elytrium.limboapi.LimboAPI;
import net.elytrium.limboapi.api.LimboSessionHandler;
import net.elytrium.limboapi.api.player.LimboPlayer;
import net.elytrium.limboapi.config.Settings;
import net.elytrium.limboapi.protocol.cache.PreparedPacketEncoder;
import net.elytrium.limboapi.protocol.packet.Player;
import net.elytrium.limboapi.protocol.packet.PlayerPosition;
import net.elytrium.limboapi.protocol.packet.PlayerPositionAndLook;
import net.elytrium.limboapi.protocol.packet.TeleportConfirm;

@Getter
public class LimboSessionHandlerImpl implements MinecraftSessionHandler {
  private final LimboAPI limboAPI;
  private final ConnectedPlayer player;
  private final LimboSessionHandler callback;
  private final MinecraftSessionHandler originalHandler;
  private RegisteredServer previousServer;

  public LimboSessionHandlerImpl(
      LimboAPI limboAPI,
      ConnectedPlayer player,
      LimboSessionHandler callback,
      MinecraftSessionHandler originalHandler) {
    this.limboAPI = limboAPI;
    this.player = player;
    this.callback = callback;
    this.originalHandler = originalHandler;

    player.getCurrentServer().ifPresent(e -> previousServer = e.getServer());
  }

  public void onSpawn(LimboImpl server, LimboPlayer player) {
    callback.onSpawn(server, player);
  }

  public boolean handle(Player packet) {
    callback.onGround(packet.isOnGround());
    return true;
  }

  public boolean handle(PlayerPosition packet) {
    callback.onMove(packet.getX(), packet.getY(), packet.getZ());
    callback.onGround(packet.isOnGround());
    return true;
  }

  public boolean handle(PlayerPositionAndLook packet) {
    callback.onMove(packet.getX(), packet.getY(), packet.getZ());
    callback.onRotate(packet.getYaw(), packet.getPitch());
    callback.onGround(packet.isOnGround());
    return true;
  }

  public boolean handle(TeleportConfirm packet) {
    callback.onTeleport(packet.getTeleportId());
    return true;
  }

  public boolean handle(Chat packet) {
    callback.onChat(packet.getMessage());
    return true;
  }

  @Override
  public void handleUnknown(ByteBuf packet) {
    if (packet.readableBytes() > 2048) {
      player.getConnection().closeWith(limboAPI.getPackets().getTooBigPacket());
    }
  }

  @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
  public void disconnected() {
    callback.onDisconnect();
    if (Settings.IMP.MAIN.LOGGING_ENABLED) {
      limboAPI.getLogger().info(
          player.getUsername() + " (" + player.getRemoteAddress()
              + ") has disconnected from VirtualServer");
    }
    player.getConnection().setSessionHandler(originalHandler);
    ChannelPipeline pipeline = player.getConnection().getChannel().pipeline();
    if (pipeline.names().contains("prepared-encoder")) {
      pipeline.remove(PreparedPacketEncoder.class);
    }
  }
}
