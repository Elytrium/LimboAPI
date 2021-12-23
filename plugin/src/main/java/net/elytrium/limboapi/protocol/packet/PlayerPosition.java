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
import net.elytrium.limboapi.server.LimboSessionHandlerImpl;

public class PlayerPosition implements MinecraftPacket {

  private double posX;
  private double posY;
  private double posZ;
  private boolean onGround;

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    this.posX = buf.readDouble();
    if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_8) < 0) {
      buf.skipBytes(8);
    }
    this.posY = buf.readDouble();
    this.posZ = buf.readDouble();
    this.onGround = buf.readBoolean();
  }

  @Override
  public void encode(ByteBuf byteBuf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {

  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    if (handler instanceof LimboSessionHandlerImpl) {
      return ((LimboSessionHandlerImpl) handler).handle(this);
    }

    return true;
  }

  public double getX() {
    return this.posX;
  }

  public double getY() {
    return this.posY;
  }

  public double getZ() {
    return this.posZ;
  }

  public boolean isOnGround() {
    return this.onGround;
  }

  @Override
  public String toString() {
    return "PlayerPosition{"
        + "x=" + this.posX
        + ", y=" + this.posY
        + ", z=" + this.posZ
        + ", onGround=" + this.onGround
        + "}";
  }
}
