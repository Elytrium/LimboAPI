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

package net.elytrium.limboapi.server.world.chunk;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.network.ProtocolVersion;
import net.elytrium.limboapi.api.chunk.VirtualBlock;
import net.elytrium.limboapi.api.chunk.data.BlockSection;
import net.elytrium.limboapi.api.chunk.data.BlockStorage;
import net.elytrium.limboapi.protocol.data.BlockStorage19;
import net.elytrium.limboapi.server.world.SimpleBlock;
import org.checkerframework.checker.nullness.qual.Nullable;

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
  public void setBlockAt(int posX, int posY, int posZ, @Nullable VirtualBlock block) {
    this.checkIndexes(posX, posY, posZ);
    this.blocks.set(posX, posY, posZ, block == null ? SimpleBlock.AIR : block);
    this.lastUpdate = System.nanoTime();
  }

  @Override
  public VirtualBlock getBlockAt(int posX, int posY, int posZ) {
    this.checkIndexes(posX, posY, posZ);
    return this.blocks.get(posX, posY, posZ);
  }

  private void checkIndexes(int posX, int posY, int posZ) {
    Preconditions.checkArgument(this.checkIndex(posX), "x should be between 0 and 15");
    Preconditions.checkArgument(this.checkIndex(posY), "y should be between 0 and 15");
    Preconditions.checkArgument(this.checkIndex(posZ), "z should be between 0 and 15");
  }

  private boolean checkIndex(int pos) {
    return pos >= 0 && pos <= 15;
  }

  @Override
  public SimpleSection getSnapshot() {
    return new SimpleSection(this.blocks.copy(), this.lastUpdate);
  }

  @Override
  public long getLastUpdate() {
    return this.lastUpdate;
  }
}
