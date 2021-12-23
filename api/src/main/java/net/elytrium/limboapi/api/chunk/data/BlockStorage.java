/*
 * Copyright (C) 2021 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.chunk.data;

import com.velocitypowered.api.network.ProtocolVersion;
import edu.umd.cs.findbugs.annotations.NonNull;
import net.elytrium.limboapi.api.chunk.VirtualBlock;

public interface BlockStorage {

  void set(int x, int y, int z, @NonNull VirtualBlock block);

  @NonNull
  VirtualBlock get(int x, int y, int z);

  void write(Object byteBufObject, ProtocolVersion version);

  int getDataLength(ProtocolVersion version);

  BlockStorage copy();

  static int index(int x, int y, int z) {
    return y << 8 | z << 4 | x;
  }
}
