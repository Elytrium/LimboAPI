/*
 * Copyright (C) 2021 - 2023 Elytrium
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
import io.netty.buffer.ByteBufOutputStream;
import io.netty.handler.codec.EncoderException;
import java.io.IOException;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.CompoundBinaryTag;

// TODO: contribute to the Velocity?
public class NbtUtils {
  public static void writeCompoundTag(ByteBuf buf, CompoundBinaryTag tag, ProtocolVersion version) {
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_20_2) >= 0) {
      try {
        buf.writeByte(BinaryTagTypes.COMPOUND.id());
        BinaryTagTypes.COMPOUND.write(tag, new ByteBufOutputStream(buf));
      } catch (IOException exception) {
        throw new EncoderException("Unable to encode NBT CompoundTag", exception);
      }
    } else {
      ProtocolUtils.writeBinaryTag(buf, version, tag);
    }
  }
}
