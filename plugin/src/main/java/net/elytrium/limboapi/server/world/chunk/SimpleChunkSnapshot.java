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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.elytrium.limboapi.api.chunk.VirtualBiome;
import net.elytrium.limboapi.api.chunk.VirtualBlock;
import net.elytrium.limboapi.api.chunk.data.ChunkSnapshot;
import net.elytrium.limboapi.api.chunk.data.LightSection;
import net.elytrium.limboapi.server.world.SimpleBlock;

@SuppressFBWarnings({"EI_EXPOSE_REP2", "EI_EXPOSE_REP"})
public class SimpleChunkSnapshot implements ChunkSnapshot {

  private final int x;
  private final int z;
  private final boolean fullChunk;
  private final SimpleSection[] sections;
  private final LightSection[] light;
  private final VirtualBiome[] biomes;

  public SimpleChunkSnapshot(int x, int z, boolean fullChunk, SimpleSection[] sections, LightSection[] light, VirtualBiome[] biomes) {
    this.x = x;
    this.z = z;
    this.fullChunk = fullChunk;
    this.sections = sections;
    this.light = light;
    this.biomes = biomes;
  }

  @Override
  public VirtualBlock getBlock(int x, int y, int z) {
    SimpleSection section = this.sections[y / 16];
    return section == null ? SimpleBlock.AIR : section.getBlockAt(x, y % 16, z);
  }

  @Override
  public int getX() {
    return this.x;
  }

  @Override
  public int getZ() {
    return this.z;
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
}
