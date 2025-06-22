/*
 * Copyright (C) 2021 - 2025 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.utils;

import java.util.HashMap;
import java.util.Map;

public final class EnumUniverse {

  private EnumUniverse() {

  }

  public static <T extends Enum<T>> Map<String, T> createProtocolLookup(T[] values) {
    Map<String, T> lookup = new HashMap<>();
    for (T value : values) {
      if (value.name().startsWith("MINECRAFT_")) {
        lookup.put(value.name().substring("MINECRAFT_".length()).replace("_", "."), value);
      }
    }
    return lookup;
  }
}
