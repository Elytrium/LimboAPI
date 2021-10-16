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
import edu.umd.cs.findbugs.annotations.Nullable;
import net.elytrium.limboapi.api.chunk.VirtualBlock;
import net.elytrium.limboapi.api.chunk.data.BlockSection;
import net.elytrium.limboapi.api.chunk.data.BlockStorage;
import net.elytrium.limboapi.protocol.data.BlockStorage19;
import net.elytrium.limboapi.server.world.SimpleBlock;

public class SimpleSection implements BlockSection {

  private final BlockStorage blocks;
  private long lastUpdate = System.nanoTime();

  public SimpleSection() {
    this(new BlockStorage19(ProtocolVersion.MINECRAFT_1_17));
  }

  public SimpleSection(BlockStorage blocks) {
    this.blocks = blocks;
  }

  public SimpleSection(BlockStorage blocks, long lastUpdate) {
    this.blocks = blocks;
    this.lastUpdate = lastUpdate;
  }

  @Override
  public void setBlockAt(int x, int y, int z, @Nullable VirtualBlock block) {
    this.checkIndexes(x, y, z);
    this.blocks.set(x, y, z, block == null ? SimpleBlock.AIR : block);
    this.lastUpdate = System.nanoTime();
  }

  @Override
  public VirtualBlock getBlockAt(int x, int y, int z) {
    this.checkIndexes(x, y, z);
    return this.blocks.get(x, y, z);
  }

  @Override
  public SimpleSection getSnapshot() {
    return new SimpleSection(this.blocks.copy(), this.lastUpdate);
  }

  @Override
  public long getLastUpdate() {
    return this.lastUpdate;
  }

  private void checkIndexes(int x, int y, int z) {
    Preconditions.checkArgument(this.checkIndex(x), "x should be between 0 and 15");
    Preconditions.checkArgument(this.checkIndex(y), "y should be between 0 and 15");
    Preconditions.checkArgument(this.checkIndex(z), "z should be between 0 and 15");
  }

  private boolean checkIndex(int i) {
    return i >= 0 && i <= 15;
  }
}
