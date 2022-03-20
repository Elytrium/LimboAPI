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

public class DefaultSpawnPosition implements MinecraftPacket {

  private final int posX;
  private final int posY;
  private final int posZ;
  private final float angle;

  public DefaultSpawnPosition(int posX, int posY, int posZ, float angle) {
    this.posX = posX;
    this.posY = posY;
    this.posZ = posZ;
    this.angle = angle;
  }

  public DefaultSpawnPosition() {
    throw new IllegalStateException();
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {

  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    long location;

    if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_14) < 0) {
      location = ((this.posX & 0x3FFFFFFL) << 38) | ((this.posY & 0xFFFL) << 26) | (this.posZ & 0x3FFFFFFL);
    } else {
      location = ((this.posX & 0x3FFFFFFL) << 38) | ((this.posZ & 0x3FFFFFFL) << 12) | (this.posY & 0xFFFL);
    }

    buf.writeLong(location);

    if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_17) >= 0) {
      buf.writeFloat(this.angle);
    }
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return true;
  }

  @Override
  public String toString() {
    return "DefaultSpawnPosition{"
        + "x=" + this.posX
        + ", y=" + this.posY
        + ", z=" + this.posZ
        + ", angle=" + this.angle
        + "}";
  }
}
