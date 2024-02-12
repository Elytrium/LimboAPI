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

package net.elytrium.limboapi.protocol.packets.c2s;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.ProtocolUtils.Direction;
import io.netty.buffer.ByteBuf;
import net.elytrium.limboapi.server.LimboSessionHandlerImpl;

public class MovePacket implements MinecraftPacket {

  private double posX;
  private double posY;
  private double posZ;
  private float yaw;
  private float pitch;
  private boolean onGround;

  @Override
  public void decode(ByteBuf buf, Direction direction, ProtocolVersion protocolVersion) {
    this.posX = buf.readDouble();
    if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_8) < 0) {
      buf.skipBytes(8);
    }
    this.posY = buf.readDouble();
    this.posZ = buf.readDouble();
    this.yaw = buf.readFloat();
    this.pitch = buf.readFloat();
    this.onGround = buf.readBoolean();
  }

  @Override
  public void encode(ByteBuf buf, Direction direction, ProtocolVersion protocolVersion) {
    throw new IllegalStateException();
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    if (handler instanceof LimboSessionHandlerImpl) {
      return ((LimboSessionHandlerImpl) handler).handle(this);
    } else {
      return true;
    }
  }

  @Override
  public int expectedMaxLength(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    return version.compareTo(ProtocolVersion.MINECRAFT_1_8) < 0 ? 41 : 33;
  }

  @Override
  public int expectedMinLength(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    return 33;
  }

  @Override
  public String toString() {
    return "PlayerPositionAndLook{"
        + "posX=" + this.posX
        + ", posY=" + this.posY
        + ", posZ=" + this.posZ
        + ", yaw=" + this.yaw
        + ", pitch=" + this.pitch
        + ", onGround=" + this.onGround
        + "}";
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

  public float getYaw() {
    return this.yaw;
  }

  public float getPitch() {
    return this.pitch;
  }

  public boolean isOnGround() {
    return this.onGround;
  }
}
