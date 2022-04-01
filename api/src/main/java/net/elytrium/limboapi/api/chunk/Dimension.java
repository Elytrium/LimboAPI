/*
 * Copyright (C) 2021 - 2022 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.chunk;

public enum Dimension {

  OVERWORLD("minecraft:overworld", 0, 0, 28), // (384 + 64) / 16
  NETHER("minecraft:nether", -1, 2, 16), // 256 / 16
  THE_END("minecraft:the_end", 1, 3, 16); // 256 / 16

  private final String key;
  private final int legacyId;
  private final int modernId;
  private final int maxSections;

  Dimension(String key, int legacyId, int modernId, int maxSections) {
    this.key = key;
    this.legacyId = legacyId;
    this.modernId = modernId;
    this.maxSections = maxSections;
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

  public int getMaxSections() {
    return this.maxSections;
  }
}

