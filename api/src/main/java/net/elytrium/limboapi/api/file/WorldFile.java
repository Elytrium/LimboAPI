/*
 * Copyright (C) 2021 - 2023 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.file;

import net.elytrium.limboapi.api.LimboFactory;
import net.elytrium.limboapi.api.chunk.VirtualWorld;
import org.checkerframework.common.value.qual.IntRange;

public interface WorldFile {

  void toWorld(LimboFactory factory, VirtualWorld world, int offsetX, int offsetY, int offsetZ);

  void toWorld(LimboFactory factory, VirtualWorld world, int offsetX, int offsetY, int offsetZ, @IntRange(from = 0, to = 15) int lightLevel);
}
