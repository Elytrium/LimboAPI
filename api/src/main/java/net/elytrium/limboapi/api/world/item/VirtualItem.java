/*
 * Copyright (C) 2021 - 2025 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.world.item;

import com.velocitypowered.api.network.ProtocolVersion;
import net.elytrium.limboapi.api.world.WorldVersion;

public interface VirtualItem {

  @Deprecated(forRemoval = true)
  default String getModernID() {
    return this.modernId();
  }

  String modernId();

  @Deprecated(forRemoval = true)
  default short getID(WorldVersion version) {
    return this.itemId(version);
  }

  short itemId(WorldVersion version);

  @Deprecated(forRemoval = true)
  default short getID(ProtocolVersion version) {
    return this.itemId(version);
  }

  short itemId(ProtocolVersion version);

  boolean isSupportedOn(WorldVersion version);

  boolean isSupportedOn(ProtocolVersion version);
}
