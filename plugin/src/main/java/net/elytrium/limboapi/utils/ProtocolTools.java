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

package net.elytrium.limboapi.utils;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;

public class ProtocolTools {

  public static void writeContainerId(ByteBuf buf, ProtocolVersion version, int id) {
    if (version.noLessThan(ProtocolVersion.MINECRAFT_1_21_2)) {
      ProtocolUtils.writeVarInt(buf, id);
    } else {
      buf.writeByte(id);
    }
  }

  public static int readContainerId(ByteBuf buf, ProtocolVersion version) {
    if (version.noLessThan(ProtocolVersion.MINECRAFT_1_21_2)) {
      return ProtocolUtils.readVarInt(buf);
    } else {
      return buf.readUnsignedByte();
    }
  }
}
