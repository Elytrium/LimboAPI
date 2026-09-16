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

package net.elytrium.limboapi.protocol.packets.s2c;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;

/**
 * Encode-only ClientboundSetEntityDataPacket. Each metadata entry is a pre-encoded byte sequence of
 * {@code [byte index][varint serializer id][value]}; the packet is terminated by {@code 0xFF}.
 */
public class SetEntityDataPacket implements MinecraftPacket {

  private final int entityId;
  private final byte[][] entries;

  public SetEntityDataPacket(int entityId, byte[][] entries) {
    this.entityId = entityId;
    this.entries = entries;
  }

  public SetEntityDataPacket() {
    this(0, new byte[0][]);
    throw new IllegalStateException();
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    throw new IllegalStateException();
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    ProtocolUtils.writeVarInt(buf, this.entityId);
    for (byte[] entry : this.entries) {
      buf.writeBytes(entry);
    }
    buf.writeByte(0xFF); // EOF marker.
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    throw new IllegalStateException();
  }
}
