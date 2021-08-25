/*
 * Copyright (C) 2021 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.chunk.data;

import net.elytrium.limboapi.api.chunk.VirtualBlock;
import org.jetbrains.annotations.Nullable;

public interface BlockSection {

  long getLastUpdate();

  VirtualBlock getBlockAt(int x, int y, int z);

  void setBlockAt(int x, int y, int z, @Nullable VirtualBlock block);

  BlockSection getSnapshot();

}
