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

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Arrays;
import java.util.function.Function;
import java.util.function.Supplier;
import net.elytrium.limboapi.api.chunk.VirtualBiome;
import net.elytrium.limboapi.api.chunk.VirtualBlock;
import net.elytrium.limboapi.api.chunk.VirtualChunk;
import net.elytrium.limboapi.api.chunk.data.ChunkSnapshot;
import net.elytrium.limboapi.api.chunk.data.LightSection;
import net.elytrium.limboapi.material.Biome;
import net.elytrium.limboapi.server.world.SimpleBlock;

public class SimpleChunk implements VirtualChunk {

  public static final int MAX_BLOCKS_PER_SECTION = 16 * 16 * 16;

  private final int x;
  private final int z;

  private final SimpleSection[] sections = new SimpleSection[16];
  private final LightSection[] light = new LightSection[18];
  private final VirtualBiome[] biomes = new VirtualBiome[1024];

  public SimpleChunk(int x, int z) {
    this.x = x;
    this.z = z;
    Arrays.fill(this.biomes, Biome.PLAINS);
    //Arrays.fill(this.light, LightSection.DEFAULT);
  }

  @Override
  public void setBlock(int x, int y, int z, @Nullable VirtualBlock block) {
    SimpleSection section = this.getSection(y);
    section.setBlockAt(x, y % 16, z, block);
  }

  @NonNull
  @Override
  public VirtualBlock getBlock(int x, int y, int z) {
    return this.sectionAction(y, (s) -> s.getBlockAt(x, y % 16, z), () -> SimpleBlock.AIR);
  }

  @Override
  public void setBiome2d(int x, int z, @NonNull VirtualBiome biome) {
    for (int y = 0; y < 256; y += 4) {
      this.setBiome3d(x, y, z, biome);
    }
  }

  @Override
  public void setBiome3d(int x, int y, int z, @NonNull VirtualBiome biome) {
    this.biomes[getBiomeIndex(x, y, z)] = biome;
  }

  @NonNull
  @Override
  public VirtualBiome getBiome(int x, int y, int z) {
    return this.biomes[getBiomeIndex(x, y, z)];
  }

  @Override
  public void setBlockLight(int x, int y, int z, byte light) {
    this.getLightSection(y, true).setBlockLight(x, y % 16, z, light);
  }

  @Override
  public byte getBlockLight(int x, int y, int z) {
    return this.getLightSection(y, false).getBlockLight(x, y % 16, z);
  }

  @Override
  public void setSkyLight(int x, int y, int z, byte light) {
    this.getLightSection(y, true).setSkyLight(x, y % 16, z, light);
  }

  @Override
  public byte getSkyLight(int x, int y, int z) {
    return this.getLightSection(y, false).getSkyLight(x, y % 16, z);
  }

  public int getX() {
    return this.x;
  }

  public int getZ() {
    return this.z;
  }

  @Override
  public ChunkSnapshot getFullChunkSnapshot() {
    return this.createSnapshot(true, 0);
  }

  @Override
  public ChunkSnapshot getPartialChunkSnapshot(long previousUpdate) {
    return this.createSnapshot(false, previousUpdate);
  }

  private ChunkSnapshot createSnapshot(boolean full, long previousUpdate) {
    SimpleSection[] sectionsSnapshot = new SimpleSection[this.sections.length];
    for (int i = 0; i < this.sections.length; i++) {
      if (this.sections[i] != null && this.sections[i].getLastUpdate() > previousUpdate) {
        sectionsSnapshot[i] = this.sections[i].getSnapshot();
      }
    }

    LightSection[] lightSnapshot = new LightSection[18];
    for (int i = 0; i < this.light.length; i++) {
      if (this.light[i] == null) {
        lightSnapshot[i] = SimpleLightSection.DEFAULT;
      } else if (this.light[i].getLastUpdate() > previousUpdate) {
        lightSnapshot[i] = this.light[i].copy();
      }
    }

    return new SimpleChunkSnapshot(this.x, this.z, full, sectionsSnapshot, lightSnapshot, Arrays.copyOf(this.biomes, this.biomes.length));
  }

  private SimpleSection getSection(int y) {
    int s = getSectionIndex(y);
    SimpleSection section = this.sections[s];
    if (section == null) {
      this.sections[s] = (section = new SimpleSection());
    }

    return section;
  }

  private <T> T sectionAction(int y, Function<SimpleSection, T> function, Supplier<T> ifNull) {
    SimpleSection section = this.sections[getSectionIndex(y)];
    if (section == null) {
      return ifNull.get();
    }

    return function.apply(section);
  }

  private LightSection getLightSection(int y, boolean create) {
    int index = y < 0 ? 0 : getSectionIndex(y) + 1;
    LightSection result = this.light[index];
    if (create && result == null) {
      this.light[index] = result = new SimpleLightSection();
    }

    return result == null ? SimpleLightSection.DEFAULT : result;
  }

  private static int getSectionIndex(int y) {
    return y / 16;
  }

  public static int getBiomeIndex(int x, int y, int z) {
    return ((y >> 2) & 63) << 4 | ((z >> 2) & 3) << 2 | ((x >> 2) & 3);
  }
}
