/*
 * Copyright (C) 2021 - 2024 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.chunk;

import com.velocitypowered.api.network.ProtocolVersion;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Set;

public enum BlockEntityVersion {
  LEGACY(EnumSet.range(ProtocolVersion.MINECRAFT_1_7_2, ProtocolVersion.MINECRAFT_1_18_2)),
  MINECRAFT_1_19(EnumSet.of(ProtocolVersion.MINECRAFT_1_19)),
  MINECRAFT_1_19_1(EnumSet.of(ProtocolVersion.MINECRAFT_1_19_1)),
  MINECRAFT_1_19_3(EnumSet.of(ProtocolVersion.MINECRAFT_1_19_3)),
  MINECRAFT_1_19_4(EnumSet.of(ProtocolVersion.MINECRAFT_1_19_4)),
  MINECRAFT_1_20(EnumSet.of(ProtocolVersion.MINECRAFT_1_20)),
  MINECRAFT_1_20_2(EnumSet.of(ProtocolVersion.MINECRAFT_1_20_2)),
  MINECRAFT_1_20_3(EnumSet.of(ProtocolVersion.MINECRAFT_1_20_3)),
  MINECRAFT_1_20_5(EnumSet.of(ProtocolVersion.MINECRAFT_1_20_5)),
  MINECRAFT_1_21(EnumSet.of(ProtocolVersion.MINECRAFT_1_21)),
  MINECRAFT_1_21_2(EnumSet.of(ProtocolVersion.MINECRAFT_1_21_2)),
  MINECRAFT_1_21_4(EnumSet.of(ProtocolVersion.MINECRAFT_1_21_4)),
  MINECRAFT_1_21_5(EnumSet.of(ProtocolVersion.MINECRAFT_1_21_5));

  private static final EnumMap<ProtocolVersion, BlockEntityVersion> MC_VERSION_TO_ITEM_VERSIONS = new EnumMap<>(ProtocolVersion.class);

  private final Set<ProtocolVersion> versions;

  BlockEntityVersion(ProtocolVersion... versions) {
    this.versions = EnumSet.copyOf(Arrays.asList(versions));
  }

  BlockEntityVersion(Set<ProtocolVersion> versions) {
    this.versions = versions;
  }

  public ProtocolVersion getMinSupportedVersion() {
    return this.versions.iterator().next();
  }

  public Set<ProtocolVersion> getVersions() {
    return this.versions;
  }

  static {
    for (BlockEntityVersion version : BlockEntityVersion.values()) {
      for (ProtocolVersion protocolVersion : version.getVersions()) {
        MC_VERSION_TO_ITEM_VERSIONS.put(protocolVersion, version);
      }
    }
  }

  public static BlockEntityVersion parse(String from) {
    return switch (from) {
      case "1.19" -> MINECRAFT_1_19;
      case "1.19.1" -> MINECRAFT_1_19_1;
      case "1.19.3" -> MINECRAFT_1_19_3;
      case "1.19.4" -> MINECRAFT_1_19_4;
      case "1.20" -> MINECRAFT_1_20;
      case "1.20.2" -> MINECRAFT_1_20_2;
      case "1.20.3" -> MINECRAFT_1_20_3;
      case "1.20.5" -> MINECRAFT_1_20_5;
      case "1.21" -> MINECRAFT_1_21;
      case "1.21.2" -> MINECRAFT_1_21_2;
      case "1.21.4" -> MINECRAFT_1_21_4;
      case "1.21.5" -> MINECRAFT_1_21_5;
      default -> LEGACY;
    };
  }

  public static BlockEntityVersion from(ProtocolVersion protocolVersion) {
    return MC_VERSION_TO_ITEM_VERSIONS.get(protocolVersion);
  }
}
