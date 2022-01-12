/*
 * Copyright (C) 2021 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.chunk;

public enum Dimension {

  OVERWORLD("minecraft:overworld", 0, 0),
  NETHER("minecraft:nether", -1, 2),
  THE_END("minecraft:the_end", 1, 3);

  private final String key;
  private final int legacyId;
  private final int modernId;

  Dimension(String key, int legacyId, int modernId) {
    this.key = key;
    this.legacyId = legacyId;
    this.modernId = modernId;
  }

  public String getKey() {
    return this.key;
  }

  public int getLegacyId() {
    return this.legacyId;
  }

  public int getModernId() {
    return this.modernId;
  }
}
