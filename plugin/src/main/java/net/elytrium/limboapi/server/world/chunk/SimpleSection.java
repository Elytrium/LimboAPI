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

package net.elytrium.limboapi.server.world.chunk;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.network.ProtocolVersion;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.elytrium.limboapi.api.chunk.VirtualBlock;
import net.elytrium.limboapi.api.chunk.data.BlockSection;
import net.elytrium.limboapi.api.chunk.data.BlockStorage;
import net.elytrium.limboapi.protocol.data.BlockStorage19;
import net.elytrium.limboapi.server.world.SimpleBlock;
import org.jetbrains.annotations.Nullable;

@RequiredArgsConstructor
@AllArgsConstructor()
public class SimpleSection implements BlockSection {

  private final BlockStorage blocks;
  @Getter
  private long lastUpdate = System.nanoTime();

  public SimpleSection() {
    this(new BlockStorage19(ProtocolVersion.MINECRAFT_1_17));
  }

  public VirtualBlock getBlockAt(int x, int y, int z) {
    checkIndexes(x, y, z);
    return blocks.get(x, y, z);
  }

  public void setBlockAt(int x, int y, int z, @Nullable VirtualBlock block) {
    checkIndexes(x, y, z);
    blocks.set(x, y, z, block == null ? SimpleBlock.AIR : block);
    lastUpdate = System.nanoTime();
  }

  public SimpleSection getSnapshot() {
    BlockStorage blockStorage = this.blocks.copy();
    return new SimpleSection(blockStorage, lastUpdate);
  }

  private void checkIndexes(int x, int y, int z) {
    Preconditions.checkArgument(checkIndex(x), "x should be between 0 and 15");
    Preconditions.checkArgument(checkIndex(y), "y should be between 0 and 15");
    Preconditions.checkArgument(checkIndex(z), "z should be between 0 and 15");
  }

  private boolean checkIndex(int i) {
    return i >= 0 && i <= 15;
  }
}
