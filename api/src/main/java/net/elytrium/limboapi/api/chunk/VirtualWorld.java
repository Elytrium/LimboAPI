/*
 * Copyright (C) 2021 - 2023 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.chunk;

import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.value.qual.IntRange;

public interface VirtualWorld {

  @NonNull
  VirtualBlock getBlock(int posX, int posY, int posZ);

  void setBiome2d(int posX, int posZ, @NonNull VirtualBiome biome);

  void setBiome3d(int posX, int posY, int posZ, @NonNull VirtualBiome biome);

  VirtualBiome getBiome(int posX, int posY, int posZ);

  byte getBlockLight(int posX, int posY, int posZ);

  void setBlockLight(int posX, int posY, int posZ, byte light);

  void fillBlockLight(@IntRange(from = 0, to = 15) int level);

  void fillSkyLight(@IntRange(from = 0, to = 15) int level);

  List<VirtualChunk> getChunks();

  @Nullable
  VirtualChunk getChunk(int posX, int posZ);

  VirtualChunk getChunkOrNew(int posX, int posZ);

  @NonNull
  Dimension getDimension();

  double getSpawnX();

  double getSpawnY();

  double getSpawnZ();

  float getYaw();

  float getPitch();

  void setBlock(int posX, int posY, int posZ, @Nullable VirtualBlock block);
}
