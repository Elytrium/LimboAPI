/*
 * Copyright (C) 2021 - 2024 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.chunk;

import net.elytrium.limboapi.api.chunk.data.ChunkSnapshot;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.value.qual.IntRange;

public interface VirtualChunk {

  void setBlock(int posX, int posY, int posZ, @Nullable VirtualBlock block);

  void setBlockEntity(int posX, int posY, int posZ, @Nullable CompoundBinaryTag nbt, @Nullable VirtualBlockEntity blockEntity);

  void setBlockEntity(VirtualBlockEntity.Entry blockEntityEntry);

  @NonNull
  VirtualBlock getBlock(int posX, int posY, int posZ);

  void setBiome2D(int posX, int posZ, @NonNull VirtualBiome biome);

  void setBiome3D(int posX, int posY, int posZ, @NonNull VirtualBiome biome);

  @NonNull
  VirtualBiome getBiome(int posX, int posY, int posZ);

  void setBlockLight(int posX, int posY, int posZ, byte light);

  byte getBlockLight(int posX, int posY, int posZ);

  void setSkyLight(int posX, int posY, int posZ, byte light);

  byte getSkyLight(int posX, int posY, int posZ);

  void fillBlockLight(@IntRange(from = 0, to = 15) int level);

  void fillSkyLight(@IntRange(from = 0, to = 15) int level);

  int getPosX();

  int getPosZ();

  ChunkSnapshot getFullChunkSnapshot();

  ChunkSnapshot getPartialChunkSnapshot(long previousUpdate);
}
