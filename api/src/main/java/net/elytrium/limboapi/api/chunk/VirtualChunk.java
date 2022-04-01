/*
 * Copyright (C) 2021 - 2022 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.chunk;

import net.elytrium.limboapi.api.chunk.data.ChunkSnapshot;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@SuppressWarnings("unused")
public interface VirtualChunk {

  void setBlock(int x, int y, int z, @Nullable VirtualBlock block);

  @NonNull
  VirtualBlock getBlock(int x, int y, int z);

  void setBiome2d(int x, int z, @NonNull VirtualBiome biome);

  void setBiome3d(int x, int y, int z, @NonNull VirtualBiome biome);

  @NonNull
  VirtualBiome getBiome(int x, int y, int z);

  void setBlockLight(int x, int y, int z, byte light);

  byte getBlockLight(int x, int y, int z);

  void setSkyLight(int x, int y, int z, byte light);

  byte getSkyLight(int x, int y, int z);

  int getX();

  int getZ();

  ChunkSnapshot getFullChunkSnapshot();

  ChunkSnapshot getPartialChunkSnapshot(long previousUpdate);
}
