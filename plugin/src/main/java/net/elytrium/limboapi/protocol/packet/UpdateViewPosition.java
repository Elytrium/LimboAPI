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

public class UpdateViewPosition implements MinecraftPacket {

  private int x;
  private int z;

  public UpdateViewPosition(int x, int z) {
    this.x = x;
    this.z = z;
  }

  public UpdateViewPosition() {

  }

  @Override
  public void decode(ByteBuf byteBuf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {

  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    ProtocolUtils.writeVarInt(buf, this.x);
    ProtocolUtils.writeVarInt(buf, this.z);
  }

  @Override
  public boolean handle(MinecraftSessionHandler minecraftSessionHandler) {
    return false;
  }

  public int getX() {
    return this.x;
  }

  public int getZ() {
    return this.z;
  }

  public void setX(int x) {
    this.x = x;
  }

  public void setZ(int z) {
    this.z = z;
  }

  @Override
  public String toString() {
    return "UpdateViewPosition{"
        + "x=" + this.getX()
        + ", z=" + this.getZ()
        + "}";
  }
}
