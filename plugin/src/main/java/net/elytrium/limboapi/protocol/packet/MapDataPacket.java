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

package net.elytrium.limboapi.protocol.packet;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;

public class MapDataPacket implements MinecraftPacket {

  private int mapId;
  private byte scale;
  private MapData data;

  public MapDataPacket(int mapId, byte scale, MapData data) {
    this.mapId = mapId;
    this.scale = scale;
    this.data = data;
  }

  public MapDataPacket() {

  }

  @Override
  public void decode(ByteBuf byteBuf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {

  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    ProtocolUtils.writeVarInt(buf, this.mapId);
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
    buf.ensureWritable(3 + this.data.getData().length);
    ProtocolUtils.writeByteArray(buf, this.data.getData());
  }

  @Override
  public boolean handle(MinecraftSessionHandler minecraftSessionHandler) {
    return false;
  }

  public int getMapId() {
    return this.mapId;
  }

  public byte getScale() {
    return this.scale;
  }

  public MapData getData() {
    return this.data;
  }

  public void setMapId(int mapId) {
    this.mapId = mapId;
  }

  public void setScale(byte scale) {
    this.scale = scale;
  }

  public void setData(MapData data) {
    this.data = data;
  }

  @Override
  public String toString() {
    return "MapDataPacket{"
        + "mapId=" + this.getMapId()
        + ", scale=" + this.getScale()
        + ", data=" + this.getData()
        + "}";
  }

  public static class MapData {

    private final int columns;
    private final int rows;
    private final int x;
    private final int y;
    private final byte[] data;

    public MapData(int columns, int rows, int x, int y, byte[] data) {
      this.columns = columns;
      this.rows = rows;
      this.x = x;
      this.y = y;
      this.data = data.clone();
    }

    public int getColumns() {
      return this.columns;
    }

    public int getRows() {
      return this.rows;
    }

    public int getX() {
      return this.x;
    }

    public int getY() {
      return this.y;
    }

    public byte[] getData() {
      return this.data.clone();
    }
  }
}
