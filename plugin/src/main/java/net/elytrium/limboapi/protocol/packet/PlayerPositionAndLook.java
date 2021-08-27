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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import net.elytrium.limboapi.server.LimboSessionHandlerImpl;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class PlayerPositionAndLook implements MinecraftPacket {

  private double x;
  private double y;
  private double z;
  private float yaw;
  private float pitch;
  private int teleportId;
  private boolean onGround;
  private boolean dismountVehicle;

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
