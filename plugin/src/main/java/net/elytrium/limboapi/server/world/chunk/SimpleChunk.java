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
import java.util.Arrays;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.Getter;
import lombok.NonNull;
import net.elytrium.limboapi.api.chunk.VirtualBiome;
import net.elytrium.limboapi.api.chunk.VirtualBlock;
import net.elytrium.limboapi.api.chunk.VirtualChunk;
import net.elytrium.limboapi.api.chunk.data.ChunkSnapshot;
import net.elytrium.limboapi.api.chunk.data.LightSection;
import net.elytrium.limboapi.material.Biome;
import net.elytrium.limboapi.server.world.SimpleBlock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressFBWarnings("NP_NONNULL_RETURN_VIOLATION")
public class SimpleChunk implements VirtualChunk {

  public static final int MAX_BLOCKS_PER_SECTION = 16 * 16 * 16;

  @Getter
  private final int x;
  @Getter
  private final int z;
  private final SimpleSection[] sections = new SimpleSection[16];
  private final LightSection[] light = new LightSection[18];
  private final VirtualBiome[] biomes = new VirtualBiome[1024];

  public SimpleChunk(int x, int z) {
    this.x = x;
    this.z = z;
    Arrays.fill(biomes, Biome.PLAINS);
    //Arrays.fill(light, LightSection.DEFAULT);
  }

  public void setBlock(int x, int y, int z, @Nullable VirtualBlock block) {
    SimpleSection section = getSection(y);
    section.setBlockAt(x, y % 16, z, block);
  }

  @NotNull
  public VirtualBlock getBlock(int x, int y, int z) {
    return sectionAction(y, (s) -> s.getBlockAt(x, y % 16, z), () -> SimpleBlock.AIR);
  }

  @NotNull
  public VirtualBiome getBiome(int x, int y, int z) {
    return biomes[getBiomeIndex(x, y, z)];
  }


  public void setBiome2d(int x, int z, @NonNull VirtualBiome biome) {
    for (int y = 0; y < 256; y += 4) {
      setBiome3d(x, y, z, biome);
    }
  }


  public void setBiome3d(int x, int y, int z, @NonNull VirtualBiome biome) {
    this.biomes[getBiomeIndex(x, y, z)] = biome;
  }

  public byte getBlockLight(int x, int y, int z) {
    return getLightSection(y, false).getBlockLight(x, y % 16, z);
  }

  public void setBlockLight(int x, int y, int z, byte light) {
    getLightSection(y, true).setBlockLight(x, y % 16, z, light);
  }

  public byte getSkyLight(int x, int y, int z) {
    return getLightSection(y, false).getSkyLight(x, y % 16, z);
  }

  public void setSkyLight(int x, int y, int z, byte light) {
    getLightSection(y, true).setSkyLight(x, y % 16, z, light);
  }

  public ChunkSnapshot getFullChunkSnapshot() {
    return createSnapshot(true, 0);
  }

  public ChunkSnapshot getPartialChunkSnapshot(long previousUpdate) {
    return createSnapshot(false, previousUpdate);
  }

  private ChunkSnapshot createSnapshot(boolean full, long previousUpdate) {
    SimpleSection[] sectionsSnapshot = new SimpleSection[sections.length];
    for (int i = 0; i < sections.length; i++) {
      if (sections[i] != null && sections[i].getLastUpdate() > previousUpdate) {
        sectionsSnapshot[i] = sections[i].getSnapshot();
      }
    }
    LightSection[] lightSnapshot = new LightSection[18];
    for (int i = 0; i < light.length; i++) {
      if (light[i] == null) {
        lightSnapshot[i] = SimpleLightSection.DEFAULT;
      } else if (light[i].getLastUpdate() > previousUpdate) {
        lightSnapshot[i] = light[i].copy();
      }
    }
    return new SimpleChunkSnapshot(x, z, full, sectionsSnapshot, lightSnapshot,
        Arrays.copyOf(biomes, biomes.length));
  }

  private SimpleSection getSection(int y) {
    int s = getSectionIndex(y);
    SimpleSection section = sections[s];
    if (section == null) {
      sections[s] = (section = new SimpleSection());
    }
    return section;
  }

  private <T> T sectionAction(int y, Function<SimpleSection, T> function, Supplier<T> ifNull) {
    SimpleSection section = sections[getSectionIndex(y)];
    if (section == null) {
      return ifNull.get();
    }
    return function.apply(section);
  }

  private LightSection getLightSection(int y, boolean create) {
    int index = y < 0 ? 0 : getSectionIndex(y) + 1;
    LightSection result = light[index];
    if (create && result == null) {
      light[index] = result = new SimpleLightSection();
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
