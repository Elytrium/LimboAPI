/*
 * Copyright (C) 2021 - 2024 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.material;

import com.velocitypowered.api.network.ProtocolVersion;

public interface VirtualItem {

  short getID(ProtocolVersion version);

  short getID(WorldVersion version);

  boolean isSupportedOn(ProtocolVersion version);

  boolean isSupportedOn(WorldVersion version);

  String getModernID();
}
