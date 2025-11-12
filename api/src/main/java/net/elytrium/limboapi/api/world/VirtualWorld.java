/*
 * Copyright (C) 2021 - 2025 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.world;

import java.util.List;
import net.elytrium.limboapi.api.world.chunk.Dimension;
import net.elytrium.limboapi.api.world.chunk.VirtualChunk;
import net.elytrium.limboapi.api.world.chunk.biome.VirtualBiome;
import net.elytrium.limboapi.api.world.chunk.block.VirtualBlock;
import net.elytrium.limboapi.api.world.chunk.blockentity.VirtualBlockEntity;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.value.qual.IntRange;
import org.jetbrains.annotations.NotNull;

public interface VirtualWorld {

  void setBlockEntity(int posX, int posY, int posZ, @Nullable CompoundBinaryTag nbt, @Nullable VirtualBlockEntity blockEntity);

  void setBlock(int posX, int posY, int posZ, @Nullable VirtualBlock block);

  /**
   * @return The block at the specified coordinates, or {@code AIR} if no block exists
   */
  @NonNull
  VirtualBlock getBlock(int posX, int posY, int posZ);

  @Deprecated(forRemoval = true)
  default void setBiome2d(int posX, int posZ, @NonNull VirtualBiome biome) {
    this.setBiome2D(posX, posZ, biome);
  }

  void setBiome2D(int posX, int posZ, @NonNull VirtualBiome biome);

  @Deprecated(forRemoval = true)
  default void setBiome3d(int posX, int posY, int posZ, @NonNull VirtualBiome biome) {
    this.setBiome3D(posX, posY, posZ, biome);
  }

  void setBiome3D(int posX, int posY, int posZ, @NonNull VirtualBiome biome);

  /**
   * @return The biome at the specified coordinates, or {@code PLAINS} if no biome exists
   */
  @NotNull
  VirtualBiome getBiome(int posX, int posY, int posZ);

  void setBlockLight(int posX, int posY, int posZ, byte light);

  byte getBlockLight(int posX, int posY, int posZ);

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

  List<VirtualChunk> getChunks();

  List<List<VirtualChunk>> getOrderedChunks();

  @Nullable
  VirtualChunk getChunk(int posX, int posZ);

  @NonNull
  VirtualChunk getChunkOrNew(int posX, int posZ);

  @NonNull
  Dimension getDimension();

  double getSpawnX();

  double getSpawnY();

  double getSpawnZ();

  float getYaw();

  float getPitch();
}
