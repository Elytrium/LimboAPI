/*
 * Copyright (C) 2021 - 2022 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.chunk.data;

import net.elytrium.limboapi.api.chunk.VirtualBiome;
import net.elytrium.limboapi.api.chunk.VirtualBlock;

public interface ChunkSnapshot {

  VirtualBlock getBlock(int x, int y, int z);

  int getX();

  int getZ();

  boolean isFullChunk();

  BlockSection[] getSections();

  LightSection[] getLight();

  VirtualBiome[] getBiomes();
}
