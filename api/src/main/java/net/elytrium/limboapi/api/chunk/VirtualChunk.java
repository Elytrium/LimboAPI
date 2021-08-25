/*
 * Copyright (C) 2021 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.chunk;

import net.elytrium.limboapi.api.chunk.data.ChunkSnapshot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface VirtualChunk {

  int getX();

  int getZ();

  void setBlock(int x, int y, int z, @Nullable VirtualBlock block);

  @NotNull
  VirtualBlock getBlock(int x, int y, int z);

  @NotNull
  VirtualBiome getBiome(int x, int y, int z);

  void setBiome2d(int x, int z, @NotNull VirtualBiome biome);

  void setBiome3d(int x, int y, int z, @NotNull VirtualBiome biome);

  byte getBlockLight(int x, int y, int z);

  void setBlockLight(int x, int y, int z, byte light);

  byte getSkyLight(int x, int y, int z);

  void setSkyLight(int x, int y, int z, byte light);

  ChunkSnapshot getFullChunkSnapshot();

  ChunkSnapshot getPartialChunkSnapshot(long previousUpdate);
}
