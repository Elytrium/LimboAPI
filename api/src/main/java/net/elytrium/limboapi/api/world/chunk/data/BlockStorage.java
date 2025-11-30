/*
 * Copyright (C) 2021 - 2025 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.world.chunk.data;

import com.velocitypowered.api.network.ProtocolVersion;
import net.elytrium.limboapi.api.world.chunk.block.VirtualBlock;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.common.value.qual.IntRange;

public interface BlockStorage {

  /**
   * @param pass Used only in 1.7-1.8 storage
   */
  void write(Object buf, ProtocolVersion version, int pass);

  void set(@IntRange(from = 0, to = 15) int posX, @IntRange(from = 0, to = 15) int posY, @IntRange(from = 0, to = 15) int posZ, @NonNull VirtualBlock block);

  @NonNull
  VirtualBlock get(@IntRange(from = 0, to = 15) int posX, @IntRange(from = 0, to = 15) int posY, @IntRange(from = 0, to = 15) int posZ);

  int getDataLength(ProtocolVersion version);

  BlockStorage copy();

  static int index(int posX, int posY, int posZ) {
    return posY << 8 | posZ << 4 | posX;
  }
}
