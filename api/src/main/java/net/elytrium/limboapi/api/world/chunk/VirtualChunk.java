/*
 * Copyright (C) 2021 - 2025 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.world.chunk;

import net.elytrium.limboapi.api.world.chunk.biome.VirtualBiome;
import net.elytrium.limboapi.api.world.chunk.block.VirtualBlock;
import net.elytrium.limboapi.api.world.chunk.blockentity.VirtualBlockEntity;
import net.elytrium.limboapi.api.world.chunk.data.BlockSection;
import net.elytrium.limboapi.api.world.chunk.data.LightSection;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.value.qual.IntRange;

public interface VirtualChunk {

  void setBlock(@IntRange(from = 0, to = 15) int posX, int posY, @IntRange(from = 0, to = 15) int posZ, @Nullable VirtualBlock block);

  void setBlockEntity(int posX, int posY, int posZ, @Nullable CompoundBinaryTag nbt, @Nullable VirtualBlockEntity blockEntity);

  void setBlockEntity(VirtualBlockEntity.Entry blockEntityEntry);

  @NonNull
  VirtualBlock getBlock(@IntRange(from = 0, to = 15) int posX, int posY, @IntRange(from = 0, to = 15) int posZ);

  void setBiome2D(@IntRange(from = 0, to = 15) int posX, @IntRange(from = 0, to = 15) int posZ, @NonNull VirtualBiome biome);

  void setBiome3D(@IntRange(from = 0, to = 15) int posX, int posY, @IntRange(from = 0, to = 15) int posZ, @NonNull VirtualBiome biome);

  @NonNull
  VirtualBiome getBiome(@IntRange(from = 0, to = 15) int posX, int posY, @IntRange(from = 0, to = 15) int posZ);

  void setBlockLight(@IntRange(from = 0, to = 15) int posX, int posY, @IntRange(from = 0, to = 15) int posZ, @IntRange(from = 0, to = 15) byte light);

  byte getBlockLight(@IntRange(from = 0, to = 15) int posX, int posY, @IntRange(from = 0, to = 15) int posZ);

  void setSkyLight(@IntRange(from = 0, to = 15) int posX, int posY, @IntRange(from = 0, to = 15) int posZ, @IntRange(from = 0, to = 15) byte light);

  byte getSkyLight(@IntRange(from = 0, to = 15) int posX, int posY, @IntRange(from = 0, to = 15) int posZ);

  @Deprecated(forRemoval = true)
  default void fillBlockLight(@IntRange(from = 0, to = 15) int level) {
    this.fillBlockLight((byte) level);
  }

  void fillBlockLight(@IntRange(from = 0, to = 15) byte level);

  @Deprecated(forRemoval = true)
  default void fillSkyLight(@IntRange(from = 0, to = 15) int level) {
    this.fillSkyLight((byte) level);
  }

  void fillSkyLight(@IntRange(from = 0, to = 15) byte level);

  int getPosX();

  int getPosZ();

  @Deprecated(forRemoval = true)
  default ChunkSnapshot getFullChunkSnapshot() {
    return this.createSnapshot(true);
  }

  @Deprecated(forRemoval = true)
  default ChunkSnapshot getPartialChunkSnapshot(long previousUpdate) {
    return this.createSnapshot(false);
  }

  ChunkSnapshot createSnapshot(boolean fullChunk);

  BlockSection[] createBlockSectionSnapshot();

  LightSection[] createLightSnapshot();
}
