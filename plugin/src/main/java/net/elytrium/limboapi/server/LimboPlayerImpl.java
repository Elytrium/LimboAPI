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
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.packet.title.GenericTitlePacket;
import java.awt.image.BufferedImage;
import net.elytrium.limboapi.LimboAPI;
import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.material.VirtualItem;
import net.elytrium.limboapi.api.player.LimboPlayer;
import net.elytrium.limboapi.api.protocol.map.MapPalette;
import net.elytrium.limboapi.api.protocol.packets.data.MapData;
import net.elytrium.limboapi.protocol.packet.MapDataPacket;
import net.elytrium.limboapi.protocol.packet.PlayerAbilities;
import net.elytrium.limboapi.protocol.packet.PlayerPositionAndLook;
import net.elytrium.limboapi.protocol.packet.SetSlot;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.text.Component;

public class LimboPlayerImpl implements LimboPlayer {

  private final ConnectedPlayer player;
  private final LimboImpl server;

  public LimboPlayerImpl(ConnectedPlayer player, LimboImpl server) {
    this.player = player;
    this.server = server;
  }

  @Override
  public void writePacket(Object packetObj) {
    this.player.getConnection().delayedWrite(packetObj);
  }

  @Override
  public void writePacketAndFlush(Object packetObj) {
    this.player.getConnection().write(packetObj);
  }

  @Override
  public void flushPackets() {
    this.player.getConnection().flush();
  }

  @Override
  public void closeWith(Object packetObj) {
    this.player.getConnection().closeWith(packetObj);
  }

  @Override
  public void sendImage(int mapId, BufferedImage image) {
    // TODO: Check 1.7.x
    // 16384 == 128 * 128 (Map size)
    byte[] canvas = new byte[16384];
    byte[][] canvas17 = new byte[128][128]; // 1.7.x canvas
    int[] toWrite = MapPalette.imageToBytes(image);

    for (int i = 0; i < 16384; ++i) {
      canvas[i] = (byte) toWrite[i];
      canvas17[i & ~128][i >> 7] = (byte) toWrite[i];
    }

    if (this.player.getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_8) < 0) {
      for (int i = 0; i < 128; ++i) {
        this.player.getConnection().write(
            new MapDataPacket(mapId, (byte) 0, new MapData(i, canvas17[i]))
        );
      }
    } else {
      this.player.getConnection().write(new MapDataPacket(mapId, (byte) 0, new MapData(canvas)));
    }
  }

  @Override
  public void setInventory(int slot, VirtualItem item, int count, int data, CompoundBinaryTag nbt) {
    this.player.getConnection().write(new SetSlot(0, slot, item, count, data, nbt));
  }

  @Override
  public void teleport(double x, double y, double z, float yaw, float pitch) {
    this.player.getConnection().write(new PlayerPositionAndLook(x, y, z, yaw, pitch, -133, false, true));
  }

  @Override
  public void sendTitle(Component title, Component subtitle, ProtocolVersion version, int fadeIn, int stay, int fadeOut) {
    {
      GenericTitlePacket packet = GenericTitlePacket.constructTitlePacket(GenericTitlePacket.ActionType.SET_TITLE, version);

      packet.setComponent(ProtocolUtils.getJsonChatSerializer(version).serialize(title));
      this.player.getConnection().write(packet);
    }
    {
      GenericTitlePacket packet = GenericTitlePacket.constructTitlePacket(GenericTitlePacket.ActionType.SET_SUBTITLE, version);

      packet.setComponent(ProtocolUtils.getJsonChatSerializer(version).serialize(subtitle));
      this.player.getConnection().write(packet);
    }
    {
      GenericTitlePacket packet = GenericTitlePacket.constructTitlePacket(GenericTitlePacket.ActionType.SET_TIMES, version);

      packet.setFadeIn(fadeIn);
      packet.setStay(stay);
      packet.setFadeOut(fadeOut);
      this.player.getConnection().write(packet);
    }
  }

  @Override
  public void disableFalling() {
    this.player.getConnection().write(new PlayerAbilities((byte) 6, 0f, 0f));
  }

  @Override
  public void disconnect() {
    LimboSessionHandlerImpl handler = (LimboSessionHandlerImpl) this.player.getConnection().getSessionHandler();
    if (handler != null) {
      handler.disconnected();

      if (LimboAPI.getInstance().hasLoginQueue(this.player)) {
        LimboAPI.getInstance().getLoginQueue(this.player).next();
      } else if (handler.getPreviousServer() != null) {
        this.player.createConnectionRequest(handler.getPreviousServer()).fireAndForget();
      }
    }
  }

  @Override
  public void disconnect(RegisteredServer server) {
    LimboSessionHandlerImpl handler = (LimboSessionHandlerImpl) this.player.getConnection().getSessionHandler();
    if (handler != null) {
      handler.disconnected();

      if (LimboAPI.getInstance().hasLoginQueue(this.player)) {
        throw new IllegalArgumentException("Cannot send to server while login");
      } else {
        this.player.getConnection().eventLoop().execute(this.player.createConnectionRequest(server)::fireAndForget);
      }
    }
  }

  @Override
  public Limbo getServer() {
    return this.server;
  }

  @Override
  public Player getProxyPlayer() {
    return this.player;
  }

  @Override
  public long getPing() {
    LimboSessionHandlerImpl handler = (LimboSessionHandlerImpl) this.player.getConnection().getSessionHandler();
    if (handler != null) {
      return handler.getPing();
    }

    return 0;
  }
}
