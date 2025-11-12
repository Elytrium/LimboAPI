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
import net.elytrium.limboapi.server.item.codec.data.BlockPosCodec;

public record DefaultSpawnPositionPacket(String dimension, int posX, int posY, int posZ, float yaw, float pitch) implements MinecraftPacket {

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    throw new IllegalStateException();
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    boolean v1_21_9 = protocolVersion.noLessThan(ProtocolVersion.MINECRAFT_1_21_9);
    if (v1_21_9) {
      ProtocolUtils.writeString(buf, this.dimension);
    }
    BlockPosCodec.encode(buf, protocolVersion, this.posX, this.posY, this.posZ);
    if (protocolVersion.noLessThan(ProtocolVersion.MINECRAFT_1_17)) {
      buf.writeFloat(this.yaw);
    }
    if (v1_21_9) {
      buf.writeFloat(this.pitch);
    }
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    throw new IllegalStateException();
  }
}
