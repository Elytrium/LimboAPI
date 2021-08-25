/*
 * Copyright (C) 2021 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.chunk.data;

import net.elytrium.limboapi.api.mcprotocollib.NibbleArray3d;

public interface LightSection {

  NibbleArray3d getBlockLight();

  NibbleArray3d getSkyLight();

  long getLastUpdate();

  byte getBlockLight(int x, int y, int z);

  byte getSkyLight(int x, int y, int z);

  void setBlockLight(int x, int y, int z, byte light);

  void setSkyLight(int x, int y, int z, byte light);

  LightSection copy();
}
