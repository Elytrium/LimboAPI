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

@SuppressWarnings("unused")
public class PlayerPositionAndLook implements MinecraftPacket {

  private double x;
  private double y;
  private double z;
  private float yaw;
  private float pitch;
  private int teleportId;
  private boolean onGround;
  private boolean dismountVehicle;

  public PlayerPositionAndLook() {

  }

  public PlayerPositionAndLook(double x, double y, double z, float yaw, float pitch, int teleportId, boolean onGround, boolean dismountVehicle) {
    this.x = x;
    this.y = y;
    this.z = z;
    this.yaw = yaw;
    this.pitch = pitch;
    this.teleportId = teleportId;
    this.onGround = onGround;
    this.dismountVehicle = dismountVehicle;
  }

  public double getX() {
    return this.x;
  }

  public void setX(double x) {
    this.x = x;
  }

  public double getY() {
    return this.y;
  }

  public void setY(double y) {
    this.y = y;
  }

  public double getZ() {
    return this.z;
  }

  public void setZ(double z) {
    this.z = z;
  }

  public float getYaw() {
    return this.yaw;
  }

  public void setYaw(float yaw) {
    this.yaw = yaw;
  }

  public float getPitch() {
    return this.pitch;
  }

  public void setPitch(float pitch) {
    this.pitch = pitch;
  }

  public int getTeleportId() {
    return this.teleportId;
  }

  public void setTeleportId(int teleportId) {
    this.teleportId = teleportId;
  }

  public boolean isOnGround() {
    return this.onGround;
  }

  public void setOnGround(boolean onGround) {
    this.onGround = onGround;
  }

  public boolean isDismountVehicle() {
    return this.dismountVehicle;
  }

  public void setDismountVehicle(boolean dismountVehicle) {
    this.dismountVehicle = dismountVehicle;
  }

  @Override
  public String toString() {
    return "PlayerPositionAndLook{"
        + "x=" + this.getX()
        + ", y=" + this.getY()
        + ", z=" + this.getZ()
        + ", yaw=" + this.getYaw()
        + ", pitch=" + this.getPitch()
        + ", teleportId=" + this.getTeleportId()
        + ", onGround=" + this.isOnGround()
        + ", dismountVehicle=" + this.isDismountVehicle()
        + "}";
  }

  @Override
  public void encode(ByteBuf buf, Direction direction, ProtocolVersion protocolVersion) {
    buf.writeDouble(this.x);
    buf.writeDouble(this.y);
    buf.writeDouble(this.z);
    buf.writeFloat(this.yaw);
    buf.writeFloat(this.pitch);

    if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_8) >= 0) {
      buf.writeByte(0x00);
    }

    if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_9) >= 0) {
      ProtocolUtils.writeVarInt(buf, this.teleportId);
    }

    if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_8) < 0) {
      buf.writeBoolean(this.onGround);
    }

    if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_17) >= 0) {
      buf.writeBoolean(this.dismountVehicle);
    }
  }

  @Override
  public void decode(ByteBuf buf, Direction direction, ProtocolVersion protocolVersion) {
    this.x = buf.readDouble();
    this.y = buf.readDouble();

    if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_8) < 0) {
      buf.readDouble(); //Skip HeadY
    }

    this.z = buf.readDouble();
    this.yaw = buf.readFloat();
    this.pitch = buf.readFloat();
    this.onGround = buf.readBoolean();
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    if (handler instanceof LimboSessionHandlerImpl) {
      return ((LimboSessionHandlerImpl) handler).handle(this);
    }

    return false;
  }
}
