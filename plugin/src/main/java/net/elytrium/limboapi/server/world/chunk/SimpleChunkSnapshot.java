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

import java.util.List;
import net.elytrium.limboapi.api.chunk.VirtualBiome;
import net.elytrium.limboapi.api.chunk.VirtualBlock;
import net.elytrium.limboapi.api.chunk.VirtualBlockEntity;
import net.elytrium.limboapi.api.chunk.data.ChunkSnapshot;
import net.elytrium.limboapi.api.chunk.data.LightSection;
import net.elytrium.limboapi.server.world.SimpleBlock;

public class SimpleChunkSnapshot implements ChunkSnapshot {

  private final int posX;
  private final int posZ;
  private final boolean fullChunk;
  private final SimpleSection[] sections;
  private final LightSection[] light;
  private final VirtualBiome[] biomes;
  private final List<VirtualBlockEntity.Entry> blockEntityEntries;

  public SimpleChunkSnapshot(int posX, int posZ, boolean fullChunk, SimpleSection[] sections, LightSection[] light,
                             VirtualBiome[] biomes, List<VirtualBlockEntity.Entry> blockEntityEntries) {
    this.posX = posX;
    this.posZ = posZ;
    this.fullChunk = fullChunk;
    this.sections = sections;
    this.light = light;
    this.biomes = biomes;
    this.blockEntityEntries = blockEntityEntries;
  }

  @Override
  public VirtualBlock getBlock(int posX, int posY, int posZ) {
    SimpleSection section = this.sections[posY >> 4];
    return section == null ? SimpleBlock.AIR : section.getBlockAt(posX, posY & 15, posZ);
  }

  @Override
  public int getPosX() {
    return this.posX;
  }

  @Override
  public int getPosZ() {
    return this.posZ;
  }

  @Override
  public boolean isFullChunk() {
    return this.fullChunk;
  }

  @Override
  public SimpleSection[] getSections() {
    return this.sections;
  }

  @Override
  public LightSection[] getLight() {
    return this.light;
  }

  @Override
  public VirtualBiome[] getBiomes() {
    return this.biomes;
  }

  @Override
  public List<VirtualBlockEntity.Entry> getBlockEntityEntries() {
    return this.blockEntityEntries;
  }
}
