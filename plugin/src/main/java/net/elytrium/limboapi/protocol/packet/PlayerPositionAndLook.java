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
import com.velocitypowered.proxy.protocol.ProtocolUtils.Direction;
import io.netty.buffer.ByteBuf;
import net.elytrium.limboapi.server.LimboSessionHandlerImpl;

public class PlayerPositionAndLook implements MinecraftPacket {

  private double posX;
  private double posY;
  private double posZ;
  private float yaw;
  private float pitch;
  private int teleportId;
  private boolean onGround;
  private boolean dismountVehicle;

  public PlayerPositionAndLook(double posX, double posY, double posZ, float yaw, float pitch, int teleportId, boolean onGround, boolean dismountVehicle) {
    this.posX = posX;
    this.posY = posY;
    this.posZ = posZ;
    this.yaw = yaw;
    this.pitch = pitch;
    this.teleportId = teleportId;
    this.onGround = onGround;
    this.dismountVehicle = dismountVehicle;
  }

  public PlayerPositionAndLook() {

  }

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

    if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_8) < 0) {
      this.onGround = buf.readBoolean();
    }

    // Ignore other data. (flags, teleportID, dismount vehicle, etc)
    buf.clear();
  }

  @Override
  public void encode(ByteBuf buf, Direction direction, ProtocolVersion protocolVersion) {
    buf.writeDouble(this.posX);
    buf.writeDouble(this.posY);
    buf.writeDouble(this.posZ);
    buf.writeFloat(this.yaw);
    buf.writeFloat(this.pitch);

    if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_8) < 0) {
      buf.writeBoolean(this.onGround);
    } else {
      buf.writeByte(0x00); // Flags.

      if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_9) >= 0) {
        ProtocolUtils.writeVarInt(buf, this.teleportId);
      }

      if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_17) >= 0) {
        buf.writeBoolean(this.dismountVehicle);
      }
    }
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

  public float getYaw() {
    return this.yaw;
  }

  public float getPitch() {
    return this.pitch;
  }

  public boolean isOnGround() {
    return this.onGround;
  }

  @Override
  public String toString() {
    return "PlayerPositionAndLook{"
        + "x=" + this.posX
        + ", y=" + this.posY
        + ", z=" + this.posZ
        + ", yaw=" + this.yaw
        + ", pitch=" + this.pitch
        + ", teleportId=" + this.teleportId
        + ", onGround=" + this.onGround
        + ", dismountVehicle=" + this.dismountVehicle
        + "}";
  }
}
