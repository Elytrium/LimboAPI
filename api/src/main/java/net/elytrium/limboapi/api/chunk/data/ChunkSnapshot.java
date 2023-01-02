/*
 * Copyright (C) 2021 - 2023 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.chunk.data;

import net.elytrium.limboapi.api.chunk.VirtualBiome;
import net.elytrium.limboapi.api.chunk.VirtualBlock;

public interface ChunkSnapshot {

  VirtualBlock getBlock(int posX, int posY, int posZ);

  int getPosX();

  int getPosZ();

  boolean isFullChunk();

  BlockSection[] getSections();

  LightSection[] getLight();

  VirtualBiome[] getBiomes();
}
