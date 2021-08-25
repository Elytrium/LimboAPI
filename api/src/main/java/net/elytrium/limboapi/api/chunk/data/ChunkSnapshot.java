/*
 * Copyright (C) 2021 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.chunk.data;

import net.elytrium.limboapi.api.chunk.VirtualBiome;
import net.elytrium.limboapi.api.chunk.VirtualBlock;

public interface ChunkSnapshot {
  int getX();
  int getZ();
  boolean isFullChunk();
  BlockSection[] getSections();
  LightSection[] getLight();
  VirtualBiome[] getBiomes();

  public VirtualBlock getBlock(int x, int y, int z);
}
