/*
 * Copyright (C) 2021 - 2022 Elytrium
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
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.value.qual.IntRange;

public class SimpleChunk implements VirtualChunk {

  public static final int MAX_BLOCKS_PER_SECTION = 16 * 16 * 16;
  public static final int MAX_BIOMES_PER_SECTION = 4 * 4 * 4;

  private final int posX;
  private final int posZ;

  private final SimpleSection[] sections = new SimpleSection[16];
  private final LightSection[] light = new LightSection[18];
  private final VirtualBiome[] biomes = new VirtualBiome[1024];

  public SimpleChunk(int posX, int posZ) {
    this(posX, posZ, Biome.PLAINS);
  }

  public SimpleChunk(int posX, int posZ, VirtualBiome defaultBiome) {
    this.posX = posX;
    this.posZ = posZ;

    for (int i = 0; i < this.light.length; i++) {
      this.light[i] = new SimpleLightSection();
    }

    Arrays.fill(this.biomes, defaultBiome);
  }

  @Override
  public void setBlock(int x, int y, int z, @Nullable VirtualBlock block) {
    SimpleSection section = this.getSection(y);
    section.setBlockAt(x, y & 15, z, block);
  }

  @NonNull
  @Override
  public VirtualBlock getBlock(int x, int y, int z) {
    return this.sectionAction(y, (s) -> s.getBlockAt(x, y & 15, z), () -> SimpleBlock.AIR);
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
    this.getLightSection(y).setBlockLight(x, y & 15, z, light);
  }

  @Override
  public byte getBlockLight(int x, int y, int z) {
    return this.getLightSection(y).getBlockLight(x, y & 15, z);
  }

  @Override
  public void setSkyLight(int x, int y, int z, byte light) {
    this.getLightSection(y).setSkyLight(x, y & 15, z, light);
  }

  @Override
  public byte getSkyLight(int x, int y, int z) {
    return this.getLightSection(y).getSkyLight(x, y & 15, z);
  }

  @Override
  public void fillBlockLight(@IntRange(from = 0, to = 15) int level) {
    for (LightSection lightSection : this.light) {
      lightSection.getBlockLight().fill(level);
    }
  }

  @Override
  public void fillSkyLight(@IntRange(from = 0, to = 15) int level) {
    for (LightSection lightSection : this.light) {
      lightSection.getSkyLight().fill(level);
    }
  }

  public int getX() {
    return this.posX;
  }

  public int getZ() {
    return this.posZ;
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
    for (int i = 0; i < this.sections.length; ++i) {
      if (this.sections[i] != null && this.sections[i].getLastUpdate() > previousUpdate) {
        sectionsSnapshot[i] = this.sections[i].getSnapshot();
      }
    }

    LightSection[] lightSnapshot = new LightSection[18];
    for (int i = 0; i < this.light.length; ++i) {
      if (this.light[i].getLastUpdate() > previousUpdate) {
        lightSnapshot[i] = this.light[i].copy();
      }
    }

    return new SimpleChunkSnapshot(this.posX, this.posZ, full, sectionsSnapshot, lightSnapshot, Arrays.copyOf(this.biomes, this.biomes.length));
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

  private LightSection getLightSection(int y) {
    int index = y < 0 ? 0 : getSectionIndex(y) + 1;
    return this.light[index];
  }

  private static int getSectionIndex(int y) {
    return y >> 4;
  }

  public static int getBiomeIndex(int x, int y, int z) {
    return ((y >> 2) & 63) << 4 | ((z >> 2) & 3) << 2 | ((x >> 2) & 3);
  }
}
