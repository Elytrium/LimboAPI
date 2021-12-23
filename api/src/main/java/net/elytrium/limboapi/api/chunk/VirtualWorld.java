/*
 * Copyright (C) 2021 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.chunk;

import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@SuppressWarnings("unused")
public interface VirtualWorld {

  @NonNull
  VirtualBlock getBlock(int x, int y, int z);

  void setBiome2d(int x, int z, @NonNull VirtualBiome biome);

  void setBiome3d(int x, int y, int z, @NonNull VirtualBiome biome);

  VirtualBiome getBiome(int x, int y, int z);

  byte getBlockLight(int x, int y, int z);

  void setBlockLight(int x, int y, int z, byte light);

  List<VirtualChunk> getChunks();

  @Nullable
  VirtualChunk getChunk(int x, int z);

  VirtualChunk getChunkOrNew(int x, int z);

  @NonNull
  Dimension getDimension();

  double getSpawnX();

  double getSpawnY();

  double getSpawnZ();

  float getYaw();

  float getPitch();

  void setBlock(int x, int y, int z, @Nullable VirtualBlock block);
}
