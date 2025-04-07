/*
 * Copyright (C) 2021 - 2025 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.protocol.item;

import com.velocitypowered.api.network.ProtocolVersion;
import java.util.List;

public interface ItemComponentMap {

  <T> ItemComponentMap add(ProtocolVersion version, String name, T value);

  ItemComponentMap remove(ProtocolVersion version, String name);

  List<ItemComponent> getAdded();

  List<ItemComponent> getRemoved();

  void read(ProtocolVersion version, Object buffer);

  void write(ProtocolVersion version, Object buffer);
}
