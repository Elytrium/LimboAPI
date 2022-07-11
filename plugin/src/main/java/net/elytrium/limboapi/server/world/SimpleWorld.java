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

package net.elytrium.limboapi.server.world;

import com.google.common.collect.ImmutableList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import net.elytrium.limboapi.api.chunk.Dimension;
import net.elytrium.limboapi.api.chunk.VirtualBiome;
import net.elytrium.limboapi.api.chunk.VirtualBlock;
import net.elytrium.limboapi.api.chunk.VirtualChunk;
import net.elytrium.limboapi.api.chunk.VirtualWorld;
import net.elytrium.limboapi.material.Biome;
import net.elytrium.limboapi.server.world.chunk.SimpleChunk;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class SimpleWorld implements VirtualWorld {

  @NonNull
  private final Dimension dimension;
  private final Map<Long, SimpleChunk> chunks = new HashMap<>();

  private final double spawnX;
  private final double spawnY;
  private final double spawnZ;
  private final float yaw;
  private final float pitch;

  public SimpleWorld(@NonNull Dimension dimension, double x, double y, double z, float yaw, float pitch) {
    this.dimension = dimension;

    this.spawnX = x;
    this.spawnY = y;
    this.spawnZ = z;
    this.yaw = yaw;
    this.pitch = pitch;

    // Modern Sodium versions don't load chunks if their "neighbours" are unloaded.
    // We are fixing this problem there by generating all the "neighbours".
    int intX = (int) x >> 4;
    int intZ = (int) z >> 4;
    for (int chunkX = intX - 1; chunkX <= intX + 1; ++chunkX) {
      for (int chunkZ = intZ - 1; chunkZ <= intZ + 1; ++chunkZ) {
        this.getChunkOrNew(chunkX << 4, chunkZ << 4);
      }
    }
  }

  @Override
  public void setBlock(int x, int y, int z, @Nullable VirtualBlock block) {
    this.getChunkOrNew(x, z).setBlock(getChunkCoordinate(x), y, getChunkCoordinate(z), block);
  }

  @NonNull
  @Override
  public VirtualBlock getBlock(int x, int y, int z) {
    return this.chunkAction(x, z, (c) -> c.getBlock(getChunkCoordinate(x), y, getChunkCoordinate(z)), () -> SimpleBlock.AIR);
  }

  @Override
  public void setBiome2d(int x, int z, @NonNull VirtualBiome biome) {
    this.getChunkOrNew(x, z).setBiome2d(getChunkCoordinate(x), getChunkCoordinate(z), biome);
  }

  @Override
  public void setBiome3d(int x, int y, int z, @NonNull VirtualBiome biome) {
    this.getChunkOrNew(x, z).setBiome3d(getChunkCoordinate(x), y, getChunkCoordinate(z), biome);
  }

  @Override
  public VirtualBiome getBiome(int x, int y, int z) {
    return this.chunkAction(x, z, (c) -> c.getBiome(x, y, z), () -> Biome.PLAINS);
  }

  @Override
  public byte getBlockLight(int x, int y, int z) {
    return this.chunkAction(x, z, (c) -> c.getBlockLight(getChunkCoordinate(x), y, getChunkCoordinate(z)), () -> (byte) 0);
  }

  @Override
  public void setBlockLight(int x, int y, int z, byte light) {
    this.getChunkOrNew(x, z).setBlockLight(getChunkCoordinate(x), y, getChunkCoordinate(z), light);
  }

  @Override
  public void fillBlockLight(int level) {
    for (SimpleChunk chunk : this.chunks.values()) {
      chunk.fillBlockLight(level);
    }
  }

  @Override
  public void fillSkyLight(int level) {
    for (SimpleChunk chunk : this.chunks.values()) {
      chunk.fillSkyLight(level);
    }
  }

  @Override
  public List<VirtualChunk> getChunks() {
    return ImmutableList.copyOf(this.chunks.values());
  }

  @Nullable
  @Override
  public SimpleChunk getChunk(int x, int z) {
    return this.chunks.get(getChunkIndex(getChunkXZ(x), getChunkXZ(z)));
  }

  @Override
  public SimpleChunk getChunkOrNew(int x, int z) {
    x = getChunkXZ(x);
    z = getChunkXZ(z);
    long index = getChunkIndex(x, z);
    SimpleChunk simpleChunk = this.chunks.get(index);
    if (simpleChunk == null) {
      this.chunks.put(index, simpleChunk = new SimpleChunk(x, z));
    }

    return simpleChunk;
  }

  @NonNull
  @Override
  public Dimension getDimension() {
    return this.dimension;
  }

  @Override
  public double getSpawnX() {
    return this.spawnX;
  }

  @Override
  public double getSpawnY() {
    return this.spawnY;
  }

  @Override
  public double getSpawnZ() {
    return this.spawnZ;
  }

  @Override
  public float getYaw() {
    return this.yaw;
  }

  @Override
  public float getPitch() {
    return this.pitch;
  }

  private <T> T chunkAction(int x, int z, Function<SimpleChunk, T> function, Supplier<T> ifNull) {
    SimpleChunk chunk = this.getChunk(x, z);
    if (chunk == null) {
      return ifNull.get();
    }

    return function.apply(chunk);
  }

  private static long getChunkIndex(int x, int z) {
    return (((long) x) << 32) | (z & 0xFFFFFFFFL);
  }

  private static int getChunkXZ(int xz) {
    return xz >> 4;
  }

  private static int getChunkCoordinate(int xz) {
    return xz & 15;
  }
}
