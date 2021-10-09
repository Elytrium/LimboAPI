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

public class PlayerAbilities implements MinecraftPacket {

  private byte flags;
  private float speed;
  private float field;

  public PlayerAbilities(byte flags, float speed, float field) {
    this.flags = flags;
    this.speed = speed;
    this.field = field;
  }

  public PlayerAbilities() {

  }

  @Override
  public void decode(ByteBuf byteBuf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {

  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    buf.writeByte(this.flags);
    buf.writeFloat(this.speed);
    buf.writeFloat(this.field);
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return false;
  }

  public byte getFlags() {
    return this.flags;
  }

  public float getSpeed() {
    return this.speed;
  }

  public float getField() {
    return this.field;
  }

  public void setFlags(byte flags) {
    this.flags = flags;
  }

  public void setSpeed(float speed) {
    this.speed = speed;
  }

  public void setField(float field) {
    this.field = field;
  }

  @Override
  public String toString() {
    return "PlayerAbilities{"
        + "flags=" + this.getFlags()
        + ", speed=" + this.getSpeed()
        + ", field=" + this.getField()
        + "}";
  }
}
