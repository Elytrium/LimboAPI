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

public class PositionRotationPacket implements MinecraftPacket {

  private final double posX;
  private final double posY;
  private final double posZ;
  private final float yaw;
  private final float pitch;
  private final boolean onGround;
  private final int teleportID;
  private final boolean dismountVehicle;

  @Deprecated(forRemoval = true)
  public PositionRotationPacket(double posX, double posY, double posZ, float yaw, float pitch, int teleportID, boolean onGround, boolean dismountVehicle) {
    this(posX, posY, posZ, yaw, pitch, onGround, teleportID, dismountVehicle);
  }

  public PositionRotationPacket(double posX, double posY, double posZ, float yaw, float pitch, boolean onGround, int teleportID, boolean dismountVehicle) {
    this.posX = posX;
    this.posY = posY;
    this.posZ = posZ;
    this.yaw = yaw;
    this.pitch = pitch;
    this.onGround = onGround;
    this.teleportID = teleportID;
    this.dismountVehicle = dismountVehicle;
  }

  public PositionRotationPacket() {
    throw new IllegalStateException();
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    throw new IllegalStateException();
  }

  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
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
        ProtocolUtils.writeVarInt(buf, this.teleportID);
      }

      if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_17) >= 0 && protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_19_3) <= 0) {
        buf.writeBoolean(this.dismountVehicle);
      }
    }
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return true;
  }

  @Override
  public String toString() {
    return "PlayerPositionAndLook{"
        + "posX=" + this.posX
        + ", posY=" + this.posY
        + ", posZ=" + this.posZ
        + ", yaw=" + this.yaw
        + ", pitch=" + this.pitch
        + ", teleportID=" + this.teleportID
        + ", onGround=" + this.onGround
        + ", dismountVehicle=" + this.dismountVehicle
        + "}";
  }
}
