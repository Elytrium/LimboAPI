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

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.connection.client.LoginSessionHandler;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.packet.title.GenericTitlePacket;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.awt.image.BufferedImage;
import lombok.RequiredArgsConstructor;
import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.material.VirtualItem;
import net.elytrium.limboapi.api.player.LimboPlayer;
import net.elytrium.limboapi.injection.FakeLoginSessionHandler;
import net.elytrium.limboapi.protocol.map.MapPalette;
import net.elytrium.limboapi.protocol.packet.MapDataPacket;
import net.elytrium.limboapi.protocol.packet.PlayerPositionAndLook;
import net.elytrium.limboapi.protocol.packet.SetSlot;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.text.Component;

@RequiredArgsConstructor
public class LimboPlayerImpl implements LimboPlayer {
  private final ConnectedPlayer player;
  private final LimboImpl server;

  @Override
  public void sendImage(int mapId, BufferedImage image) {
    byte[] canvas = new byte[16384];
    int[] toWrite = MapPalette.imageToBytes(image);

    for (int i = 0; i < 16384; i++) {
      canvas[i] = (byte) toWrite[i];
    }

    player.getConnection().write(
        new MapDataPacket(mapId, (byte) 0, new MapDataPacket.MapData(128, 128, 0, 0, canvas))
    );
  }

  @Override
  public void setInventory(int slot, VirtualItem item, int count, int data, CompoundBinaryTag nbt) {
    player.getConnection().write(
        new SetSlot(0, slot, item, count, data, nbt)
    );
  }

  @Override
  public void sendTitle(Component title, Component subtitle, ProtocolVersion version, int fadeIn, int stay, int fadeOut) {
    {
      GenericTitlePacket packet = GenericTitlePacket.constructTitlePacket(GenericTitlePacket.ActionType.SET_TITLE, version);
      packet.setComponent(ProtocolUtils.getJsonChatSerializer(version).serialize(title));
      player.getConnection().write(packet);
    }
    {
      GenericTitlePacket packet = GenericTitlePacket.constructTitlePacket(GenericTitlePacket.ActionType.SET_SUBTITLE, version);
      packet.setComponent(ProtocolUtils.getJsonChatSerializer(version).serialize(subtitle));
      player.getConnection().write(packet);
    }
    {
      GenericTitlePacket packet = GenericTitlePacket.constructTitlePacket(GenericTitlePacket.ActionType.SET_TIMES, version);
      packet.setFadeIn(fadeIn);
      packet.setStay(stay);
      packet.setFadeOut(fadeOut);
      player.getConnection().write(packet);
    }
  }

  @Override
  public void teleport(double x, double y, double z, float yaw, float pitch) {
    player.getConnection().write(
        new PlayerPositionAndLook(x, y, z, yaw, pitch, -133, false, true)
    );
  }

  @Override
  @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
  public void disconnect() {
    LimboSessionHandlerImpl handler = (LimboSessionHandlerImpl) player.getConnection().getSessionHandler();
    handler.disconnected();

    player.getConnection().eventLoop().execute(() -> {
      if (handler.getOriginalHandler() instanceof FakeLoginSessionHandler) {
        ((FakeLoginSessionHandler) handler.getOriginalHandler()).initialize(player);
      } else {
        player.createConnectionRequest(handler.getPreviousServer()).fireAndForget();
      }
    });
  }

  @Override
  @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
  public void disconnect(RegisteredServer server) {
    LimboSessionHandlerImpl handler = (LimboSessionHandlerImpl) player.getConnection().getSessionHandler();
    handler.disconnected();

    player.getConnection().eventLoop().execute(() -> {
      if (handler.getOriginalHandler() instanceof LoginSessionHandler) {
        handler.getLimboAPI().getLogger().error("Cannot send to Registered Server while joining");
        ((FakeLoginSessionHandler) handler.getOriginalHandler()).initialize(player);
      } else {
        player.createConnectionRequest(server).fireAndForget();
      }
    });
  }

  @Override
  public Limbo getServer() {
    return server;
  }

  @Override
  public Player getProxyPlayer() {
    return player;
  }
}
