/*
 * Copyright (C) 2021 - 2025 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.world.chunk.data;

import net.elytrium.limboapi.api.world.chunk.util.NibbleArray3D;
import org.checkerframework.common.value.qual.IntRange;

public interface LightSection {

  void setBlockLight(@IntRange(from = 0, to = 15) int posX, @IntRange(from = 0, to = 15) int posY, @IntRange(from = 0, to = 15) int posZ, @IntRange(from = 0, to = 15) byte light);

  NibbleArray3D getBlockLight();

  byte getBlockLight(@IntRange(from = 0, to = 15) int posX, @IntRange(from = 0, to = 15) int posY, @IntRange(from = 0, to = 15) int posZ);

  void setSkyLight(@IntRange(from = 0, to = 15) int posX, @IntRange(from = 0, to = 15) int posY, @IntRange(from = 0, to = 15) int posZ, @IntRange(from = 0, to = 15) byte light);

  NibbleArray3D getSkyLight();

  byte getSkyLight(@IntRange(from = 0, to = 15) int posX, @IntRange(from = 0, to = 15) int posY, @IntRange(from = 0, to = 15) int posZ);

  @Deprecated(forRemoval = true)
  default long getLastUpdate() {
    return 0;
  }

  LightSection copy();
}
