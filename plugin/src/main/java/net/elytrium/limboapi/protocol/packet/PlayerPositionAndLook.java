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

  private double x;
  private double y;
  private double z;
  private float yaw;
  private float pitch;
  private int teleportId;
  private boolean onGround;
  private boolean dismountVehicle;

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

  public PlayerPositionAndLook() {

  }

  @Override
  public void decode(ByteBuf buf, Direction direction, ProtocolVersion protocolVersion) {
    this.x = buf.readDouble();
    if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_8) < 0) {
      buf.skipBytes(8);
    }
    this.y = buf.readDouble();
    this.z = buf.readDouble();
    this.yaw = buf.readFloat();
    this.pitch = buf.readFloat();

    if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_8) < 0) {
      this.onGround = buf.readBoolean();
    }

    // Ignore other data (flags, teleportID, dismount vehicle, etc)
    buf.clear();
  }

  @Override
  public void encode(ByteBuf buf, Direction direction, ProtocolVersion protocolVersion) {
    buf.writeDouble(this.x);
    buf.writeDouble(this.y);
    buf.writeDouble(this.z);
    buf.writeFloat(this.yaw);
    buf.writeFloat(this.pitch);

    if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_8) < 0) {
      buf.writeBoolean(this.onGround);
    } else {
      buf.writeByte(0x00);

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
    return this.x;
  }

  public double getY() {
    return this.y;
  }

  public double getZ() {
    return this.z;
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
        + "x=" + this.x
        + ", y=" + this.y
        + ", z=" + this.z
        + ", yaw=" + this.yaw
        + ", pitch=" + this.pitch
        + ", teleportId=" + this.teleportId
        + ", onGround=" + this.onGround
        + ", dismountVehicle=" + this.dismountVehicle
        + "}";
  }
}
