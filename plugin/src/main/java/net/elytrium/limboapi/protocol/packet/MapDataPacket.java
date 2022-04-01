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

package net.elytrium.limboapi.protocol.packet;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import net.elytrium.limboapi.api.protocol.packets.data.MapData;

public class MapDataPacket implements MinecraftPacket {

  private final int mapId;
  private final byte scale;
  private final MapData data;

  public MapDataPacket(int mapId, byte scale, MapData data) {
    this.mapId = mapId;
    this.scale = scale;
    this.data = data;
  }

  public MapDataPacket() {
    throw new IllegalStateException();
  }

  @Override
  public void decode(ByteBuf byteBuf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {

  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    ProtocolUtils.writeVarInt(buf, this.mapId);
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_8) >= 0) {
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

      buf.writeByte(this.data.getColumns());
      buf.writeByte(this.data.getRows());
      buf.writeByte(this.data.getX());
      buf.writeByte(this.data.getY());
      ProtocolUtils.writeByteArray(buf, this.data.getData());
    } else {
      buf.writeShort(this.data.getData().length + 3);
      buf.writeByte(0);
      buf.writeByte(this.data.getX());
      buf.writeByte(this.data.getY());
      buf.writeBytes(this.data.getData());
    }
  }

  @Override
  public boolean handle(MinecraftSessionHandler minecraftSessionHandler) {
    return true;
  }

  @Override
  public String toString() {
    return "MapDataPacket{"
        + "mapId=" + this.mapId
        + ", scale=" + this.scale
        + ", data=" + this.data
        + "}";
  }
}
