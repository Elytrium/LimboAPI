/*
 * Copyright (C) 2021 - 2024 Elytrium
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

package net.elytrium.limboapi.protocol.packets.s2c;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import net.elytrium.limboapi.api.protocol.packets.data.MapData;

public class MapDataPacket implements MinecraftPacket {

  private final int mapID;
  private final byte scale;
  private final MapData mapData;

  public MapDataPacket(int mapID, byte scale, MapData mapData) {
    this.mapID = mapID;
    this.scale = scale;
    this.mapData = mapData;
  }

  public MapDataPacket() {
    throw new IllegalStateException();
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    throw new IllegalStateException();
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    ProtocolUtils.writeVarInt(buf, this.mapID);
    byte[] data = this.mapData.getData();
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_8) < 0) {
      buf.writeShort(data.length + 3);
      buf.writeByte(0);
      buf.writeByte(this.mapData.getX());
      buf.writeByte(this.mapData.getY());

      buf.writeBytes(data);
    } else {
      buf.writeByte(this.scale);

      if (version.compareTo(ProtocolVersion.MINECRAFT_1_9) >= 0 && version.compareTo(ProtocolVersion.MINECRAFT_1_17) < 0) {
        buf.writeBoolean(false);
      }

      if (version.compareTo(ProtocolVersion.MINECRAFT_1_14) >= 0) {
        buf.writeBoolean(false);
      }

      if (version.compareTo(ProtocolVersion.MINECRAFT_1_17) >= 0) {
        buf.writeBoolean(false);
      } else {
        ProtocolUtils.writeVarInt(buf, 0);
      }

      buf.writeByte(this.mapData.getColumns());
      buf.writeByte(this.mapData.getRows());
      buf.writeByte(this.mapData.getX());
      buf.writeByte(this.mapData.getY());

      ProtocolUtils.writeByteArray(buf, data);
    }
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return true;
  }

  @Override
  public String toString() {
    return "MapDataPacket{"
        + "mapID=" + this.mapID
        + ", scale=" + this.scale
        + ", mapData=" + this.mapData
        + "}";
  }
}
