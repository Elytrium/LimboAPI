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

public class PlayerAbilitiesPacket implements MinecraftPacket {

  private final byte flags;
  private final float walkSpeed;
  private final float flySpeed;

  public PlayerAbilitiesPacket(byte flags, float flySpeed, float walkSpeed) {
    this.flags = flags;
    this.flySpeed = flySpeed;
    this.walkSpeed = walkSpeed;
  }

  public PlayerAbilitiesPacket() {
    throw new IllegalStateException();
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    throw new IllegalStateException();
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    buf.writeByte(this.flags);
    buf.writeFloat(this.flySpeed);
    buf.writeFloat(this.walkSpeed);
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return true;
  }

  @Override
  public String toString() {
    return "PlayerAbilities{"
        + "flags=" + this.flags
        + ", flySpeed=" + this.flySpeed
        + ", walkSpeed=" + this.walkSpeed
        + "}";
  }
}
