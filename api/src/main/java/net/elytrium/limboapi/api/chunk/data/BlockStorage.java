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

package net.elytrium.limboapi.api.chunk.data;

import com.velocitypowered.api.network.ProtocolVersion;
import io.netty.buffer.ByteBuf;
import net.elytrium.limboapi.api.chunk.VirtualBlock;
import org.jetbrains.annotations.NotNull;

public interface BlockStorage {

  void set(int x, int y, int z, @NotNull VirtualBlock block);

  @NotNull VirtualBlock get(int x, int y, int z);

  void write(ByteBuf buf, ProtocolVersion version);

  int getDataLength(ProtocolVersion version);

  BlockStorage copy();

  static int index(int x, int y, int z) {
    return y << 8 | z << 4 | x;
  }
}
