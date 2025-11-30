/*
 * Copyright (C) 2021 - 2025 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.world;

import com.velocitypowered.api.network.ProtocolVersion;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Objects;

public enum WorldVersion {

  LEGACY(EnumSet.range(ProtocolVersion.MINECRAFT_1_7_2, ProtocolVersion.MINECRAFT_1_12_2)), // TODO better legacy support
  MINECRAFT_1_13(EnumSet.of(ProtocolVersion.MINECRAFT_1_13)),
  MINECRAFT_1_13_2(EnumSet.of(ProtocolVersion.MINECRAFT_1_13_1, ProtocolVersion.MINECRAFT_1_13_2)),
  MINECRAFT_1_14(EnumSet.range(ProtocolVersion.MINECRAFT_1_14, ProtocolVersion.MINECRAFT_1_14_4)),
  MINECRAFT_1_15(EnumSet.range(ProtocolVersion.MINECRAFT_1_15, ProtocolVersion.MINECRAFT_1_15_2)),
  MINECRAFT_1_16(EnumSet.of(ProtocolVersion.MINECRAFT_1_16, ProtocolVersion.MINECRAFT_1_16_1)),
  MINECRAFT_1_16_2(EnumSet.range(ProtocolVersion.MINECRAFT_1_16_2, ProtocolVersion.MINECRAFT_1_16_4)),
  MINECRAFT_1_17(EnumSet.range(ProtocolVersion.MINECRAFT_1_17, ProtocolVersion.MINECRAFT_1_18_2)),
  MINECRAFT_1_19(EnumSet.range(ProtocolVersion.MINECRAFT_1_19, ProtocolVersion.MINECRAFT_1_19_1)),
  MINECRAFT_1_19_3(EnumSet.of(ProtocolVersion.MINECRAFT_1_19_3)),
  MINECRAFT_1_19_4(EnumSet.range(ProtocolVersion.MINECRAFT_1_19_4, ProtocolVersion.MINECRAFT_1_20)),
  MINECRAFT_1_20(EnumSet.range(ProtocolVersion.MINECRAFT_1_20, ProtocolVersion.MINECRAFT_1_20_2)),
  MINECRAFT_1_20_3(EnumSet.of(ProtocolVersion.MINECRAFT_1_20_3)),
  MINECRAFT_1_20_5(EnumSet.of(ProtocolVersion.MINECRAFT_1_20_5)),
  MINECRAFT_1_21(EnumSet.of(ProtocolVersion.MINECRAFT_1_21)),
  MINECRAFT_1_21_2(EnumSet.of(ProtocolVersion.MINECRAFT_1_21_2)),
  MINECRAFT_1_21_4(EnumSet.of(ProtocolVersion.MINECRAFT_1_21_4)),
  MINECRAFT_1_21_5(EnumSet.of(ProtocolVersion.MINECRAFT_1_21_5)),
  MINECRAFT_1_21_6(EnumSet.of(ProtocolVersion.MINECRAFT_1_21_6)),
  MINECRAFT_1_21_7(EnumSet.of(ProtocolVersion.MINECRAFT_1_21_7)),
  MINECRAFT_1_21_9(EnumSet.of(ProtocolVersion.MINECRAFT_1_21_9));

  private static final EnumMap<ProtocolVersion, WorldVersion> VERSIONS_MAP = new EnumMap<>(ProtocolVersion.class);

  private final EnumSet<ProtocolVersion> versions;

  WorldVersion(EnumSet<ProtocolVersion> versions) {
    this.versions = versions;
  }

  public EnumSet<ProtocolVersion> getVersions() {
    return this.versions;
  }

  static {
    for (WorldVersion version : WorldVersion.values()) {
      version.getVersions().forEach(protocolVersion -> WorldVersion.VERSIONS_MAP.put(protocolVersion, version));
    }
  }

  public static WorldVersion from(ProtocolVersion protocolVersion) {
    return Objects.requireNonNull(WorldVersion.VERSIONS_MAP.get(protocolVersion));
  }
}
