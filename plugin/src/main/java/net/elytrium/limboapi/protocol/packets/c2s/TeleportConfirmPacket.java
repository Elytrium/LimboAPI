/*
 * Copyright (C) 2021 - 2025 Elytrium
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
import io.netty.buffer.ByteBuf;
import net.elytrium.limboapi.server.LimboSessionHandlerImpl;

public class TeleportConfirmPacket implements MinecraftPacket {

  private int teleportID;
  private double posX;
  private double posY;
  private double posZ;
  private float yaw;
  private float pitch;

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    this.teleportID = ProtocolUtils.readVarInt(buf);
    if (protocolVersion.noLessThan(ProtocolVersion.MINECRAFT_26_3)) {
      this.posX = buf.readDouble();
      this.posY = buf.readDouble();
      this.posZ = buf.readDouble();
      this.yaw = buf.readFloat();
      this.pitch = buf.readFloat();
    }
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
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
  public int decodeExpectedMaxLength(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    return 5 + (version.noLessThan(ProtocolVersion.MINECRAFT_26_3) ? 32 : 0);
  }

  @Override
  public int decodeExpectedMinLength(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    return 1 + (version.noLessThan(ProtocolVersion.MINECRAFT_26_3) ? 32 : 0);
  }

  @Override
  public String toString() {
    return "TeleportConfirmPacket{"
        + "teleportID=" + this.teleportID
        + ", posX=" + this.posX
        + ", posY=" + this.posY
        + ", posZ=" + this.posZ
        + ", yaw=" + this.yaw
        + ", pitch=" + this.pitch
        + '}';
  }

  public int getTeleportID() {
    return this.teleportID;
  }

  public double getPosX() {
    return this.posX;
  }

  public double getPosY() {
    return this.posY;
  }

  public double getPosZ() {
    return this.posZ;
  }

  public float getYaw() {
    return this.yaw;
  }

  public float getPitch() {
    return this.pitch;
  }
}
