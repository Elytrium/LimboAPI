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

package net.elytrium.limboapi.server.world;

import com.google.common.collect.ImmutableList;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
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

    this.getChunkOrNew((int) x, (int) z);
  }

  public void setBlock(int x, int y, int z, @Nullable VirtualBlock block) {
    this.getChunkOrNew(x, z).setBlock(getChunkCoordinate(x), y, getChunkCoordinate(z), block);
  }

  @NonNull
  public VirtualBlock getBlock(int x, int y, int z) {
    return this.chunkAction(x, z, (c) -> c.getBlock(getChunkCoordinate(x), y, getChunkCoordinate(z)), () -> SimpleBlock.AIR);
  }

  public void setBiome2d(int x, int z, @NonNull VirtualBiome biome) {
    this.getChunkOrNew(x, z).setBiome2d(getChunkCoordinate(x), getChunkCoordinate(z), biome);
  }

  public void setBiome3d(int x, int y, int z, @NonNull VirtualBiome biome) {
    this.getChunkOrNew(x, z).setBiome3d(getChunkCoordinate(x), y, getChunkCoordinate(z), biome);
  }

  public VirtualBiome getBiome(int x, int y, int z) {
    return this.chunkAction(x, z, (c) -> c.getBiome(x, y, z), () -> Biome.PLAINS);
  }

  public byte getBlockLight(int x, int y, int z) {
    return this.chunkAction(x, z, (c) -> c.getBlockLight(getChunkCoordinate(x), y, getChunkCoordinate(z)), () -> (byte) 0);
  }

  public void setBlockLight(int x, int y, int z, byte light) {
    this.getChunkOrNew(x, z).setBlockLight(getChunkCoordinate(x), y, getChunkCoordinate(z), light);
  }

  public List<VirtualChunk> getChunks() {
    return ImmutableList.copyOf(this.chunks.values());
  }

  private <T> T chunkAction(int x, int z, Function<SimpleChunk, T> function, Supplier<T> ifNull) {
    SimpleChunk chunk = this.getChunk(x, z);
    if (chunk == null) {
      return ifNull.get();
    }

    return function.apply(chunk);
  }

  @Nullable
  public SimpleChunk getChunk(int x, int z) {
    return this.chunks.get(getChunkIndex(getChunkXZ(x), getChunkXZ(z)));
  }

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

  private static long getChunkIndex(int x, int z) {
    return (((long) x) << 32) | (z & 0xffffffffL);
  }

  private static int getChunkXZ(int xz) {
    return xz >> 4;
  }

  private static int getChunkCoordinate(int xz) {
    xz %= 16;
    if (xz < 0) {
      xz += 16;
    }

    return xz;
  }

  @NonNull
  public Dimension getDimension() {
    return this.dimension;
  }

  public double getSpawnX() {
    return this.spawnX;
  }

  public double getSpawnY() {
    return this.spawnY;
  }

  public double getSpawnZ() {
    return this.spawnZ;
  }

  public float getYaw() {
    return this.yaw;
  }

  public float getPitch() {
    return this.pitch;
  }
}
