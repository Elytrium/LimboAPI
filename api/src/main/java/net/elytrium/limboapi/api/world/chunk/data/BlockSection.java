/*
 * Copyright (C) 2021 - 2025 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.world.chunk.data;

import net.elytrium.limboapi.api.world.chunk.block.VirtualBlock;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.value.qual.IntRange;

public interface BlockSection {

  void setBlockAt(@IntRange(from = 0, to = 15) int posX, @IntRange(from = 0, to = 15) int posY, @IntRange(from = 0, to = 15) int posZ, @Nullable VirtualBlock block);

  VirtualBlock getBlockAt(@IntRange(from = 0, to = 15) int posX, @IntRange(from = 0, to = 15) int posY, @IntRange(from = 0, to = 15) int posZ);

  @Deprecated(forRemoval = true)
  default BlockSection getSnapshot() {
    return this.copy();
  }

  BlockSection copy();

  @Deprecated(forRemoval = true)
  default long getLastUpdate() {
    return 0;
  }
}
