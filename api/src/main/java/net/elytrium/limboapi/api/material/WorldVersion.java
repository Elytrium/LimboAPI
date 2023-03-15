/*
 * Copyright (C) 2021 - 2023 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.material;

import com.velocitypowered.api.network.ProtocolVersion;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Set;

public enum WorldVersion {
  LEGACY(EnumSet.range(ProtocolVersion.MINECRAFT_1_7_2, ProtocolVersion.MINECRAFT_1_12_2)),
  MINECRAFT_1_13(ProtocolVersion.MINECRAFT_1_13),
  MINECRAFT_1_13_2(ProtocolVersion.MINECRAFT_1_13_1, ProtocolVersion.MINECRAFT_1_13_2),
  MINECRAFT_1_14(EnumSet.range(ProtocolVersion.MINECRAFT_1_14, ProtocolVersion.MINECRAFT_1_14_4)),
  MINECRAFT_1_15(EnumSet.range(ProtocolVersion.MINECRAFT_1_15, ProtocolVersion.MINECRAFT_1_15_2)),
  MINECRAFT_1_16(ProtocolVersion.MINECRAFT_1_16, ProtocolVersion.MINECRAFT_1_16_1),
  MINECRAFT_1_16_2(EnumSet.range(ProtocolVersion.MINECRAFT_1_16_2, ProtocolVersion.MINECRAFT_1_16_4)),
  MINECRAFT_1_17(EnumSet.range(ProtocolVersion.MINECRAFT_1_17, ProtocolVersion.MINECRAFT_1_18_2)),
  MINECRAFT_1_19(EnumSet.range(ProtocolVersion.MINECRAFT_1_19, ProtocolVersion.MINECRAFT_1_19_1)),
  MINECRAFT_1_19_3(ProtocolVersion.MINECRAFT_1_19_3),
  MINECRAFT_1_19_4(EnumSet.range(ProtocolVersion.MINECRAFT_1_19_4, ProtocolVersion.MAXIMUM_VERSION));

  private static final EnumMap<ProtocolVersion, WorldVersion> MC_VERSION_TO_ITEM_VERSIONS = new EnumMap<>(ProtocolVersion.class);

  private final Set<ProtocolVersion> versions;

  WorldVersion(ProtocolVersion... versions) {
    this.versions = EnumSet.copyOf(Arrays.asList(versions));
  }

  WorldVersion(Set<ProtocolVersion> versions) {
    this.versions = versions;
  }

  public ProtocolVersion getMinSupportedVersion() {
    return this.versions.iterator().next();
  }

  public Set<ProtocolVersion> getVersions() {
    return this.versions;
  }

  static {
    for (WorldVersion version : WorldVersion.values()) {
      for (ProtocolVersion protocolVersion : version.getVersions()) {
        MC_VERSION_TO_ITEM_VERSIONS.put(protocolVersion, version);
      }
    }
  }

  public static WorldVersion parse(String from) {
    switch (from) {
      case "1.13": {
        return MINECRAFT_1_13;
      }
      case "1.13.2": {
        return MINECRAFT_1_13_2;
      }
      case "1.14": {
        return MINECRAFT_1_14;
      }
      case "1.15": {
        return MINECRAFT_1_15;
      }
      case "1.16": {
        return MINECRAFT_1_16;
      }
      case "1.16.2": {
        return MINECRAFT_1_16_2;
      }
      case "1.17": {
        return MINECRAFT_1_17;
      }
      case "1.19": {
        return MINECRAFT_1_19;
      }
      case "1.19.3": {
        return MINECRAFT_1_19_3;
      }
      case "1.19.4": {
        return MINECRAFT_1_19_4;
      }
      default: {
        return LEGACY;
      }
    }
  }

  public static WorldVersion from(ProtocolVersion protocolVersion) {
    return MC_VERSION_TO_ITEM_VERSIONS.get(protocolVersion);
  }
}
