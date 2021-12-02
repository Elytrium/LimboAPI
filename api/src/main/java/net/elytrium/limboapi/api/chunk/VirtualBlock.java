/*
 * Copyright (C) 2021 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.chunk;

import com.velocitypowered.api.network.ProtocolVersion;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("unused")
public interface VirtualBlock {

  VirtualBlock setData(byte data);

  byte getData(Version version);

  byte getData(ProtocolVersion version);

  Map<Version, BlockInfo> getBlockInfos();

  short getId(ProtocolVersion version);

  short getId(Version version);

  boolean isSolid();

  boolean isAir();

  boolean isMotionBlocking();

  enum Version {

    /*
    MINECRAFT_1_7(ProtocolVersion.MINECRAFT_1_7_2, ProtocolVersion.MINECRAFT_1_7_6),
    MINECRAFT_1_8(ProtocolVersion.MINECRAFT_1_8),
    MINECRAFT_1_9(EnumSet.range(ProtocolVersion.MINECRAFT_1_9, ProtocolVersion.MINECRAFT_1_9_4)),
    MINECRAFT_1_10(ProtocolVersion.MINECRAFT_1_10),
    MINECRAFT_1_11(ProtocolVersion.MINECRAFT_1_11, ProtocolVersion.MINECRAFT_1_11_1),
    */

    LEGACY(EnumSet.range(ProtocolVersion.MINECRAFT_1_7_2, ProtocolVersion.MINECRAFT_1_8)),
    MINECRAFT_1_12(EnumSet.range(ProtocolVersion.MINECRAFT_1_9, ProtocolVersion.MINECRAFT_1_12_2)),
    MINECRAFT_1_13(ProtocolVersion.MINECRAFT_1_13),
    MINECRAFT_1_13_2(ProtocolVersion.MINECRAFT_1_13_1, ProtocolVersion.MINECRAFT_1_13_2),
    MINECRAFT_1_14(EnumSet.range(ProtocolVersion.MINECRAFT_1_14, ProtocolVersion.MINECRAFT_1_14_4)),
    MINECRAFT_1_15(EnumSet.range(ProtocolVersion.MINECRAFT_1_15, ProtocolVersion.MINECRAFT_1_15_2)),
    MINECRAFT_1_16(ProtocolVersion.MINECRAFT_1_16, ProtocolVersion.MINECRAFT_1_16_1),
    MINECRAFT_1_16_2(EnumSet.range(ProtocolVersion.MINECRAFT_1_16_2, ProtocolVersion.MINECRAFT_1_16_4)),
    MINECRAFT_1_17(EnumSet.range(ProtocolVersion.MINECRAFT_1_17, ProtocolVersion.MAXIMUM_VERSION));
    // MINECRAFT_1_18(ProtocolVersion.MAXIMUM_VERSION);

    private static final EnumMap<ProtocolVersion, Version> mcVersionToBlockVersions = new EnumMap<>(ProtocolVersion.class);

    public static Version parse(String from) {
      switch (from) {
        case "1.12": {
          return MINECRAFT_1_12;
        }
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
        // case "1.18": {
        //  return MINECRAFT_1_18;
        // }
        default: {
          return LEGACY;
        }
      }
    }

    static {
      for (Version version : Version.values()) {
        for (ProtocolVersion protocolVersion : version.versions) {
          mcVersionToBlockVersions.put(protocolVersion, version);
        }
      }
    }

    private final Set<ProtocolVersion> versions;

    Version(ProtocolVersion... versions) {
      this.versions = EnumSet.copyOf(Arrays.asList(versions));
    }

    Version(Set<ProtocolVersion> versions) {
      this.versions = versions;
    }

    public boolean isBefore(Version other) {
      return this.compareTo(other) < 0;
    }

    public boolean isBeforeOrEq(Version other) {
      return this.compareTo(other) <= 0;
    }

    public boolean isAfter(Version other) {
      return this.compareTo(other) > 0;
    }

    public boolean isAfterOrEq(Version other) {
      return this.compareTo(other) >= 0;
    }

    public Set<ProtocolVersion> getVersions() {
      return this.versions;
    }

    public static Version map(ProtocolVersion protocolVersion) {
      return mcVersionToBlockVersions.get(protocolVersion);
    }
  }

  class BlockInfo {

    @NonNull
    private final Version version;
    private final short id;
    private byte data;
    private final BlockInfo fallback;

    public BlockInfo(Version version, short id, byte data) {
      this(version, id, data, null);
    }

    public BlockInfo(Version version, @NonNull BlockInfo fallback) {
      this(version, (short) 0, (byte) 0, fallback);
    }

    private BlockInfo(@NonNull Version version, short id, byte data, BlockInfo fallback) {
      this.version = version;
      this.id = id;
      this.data = data;
      this.fallback = fallback;
    }

    @NonNull
    public Version getVersion() {
      return this.version;
    }

    public short getId() {
      return this.fallback == null ? this.id : this.fallback.getId();
    }

    public byte getData() {
      return this.fallback == null ? this.data : this.fallback.getData();
    }

    public void setData(byte data) {
      this.data = data;
    }

    public static BlockInfo info(Version version, int id, int meta) {
      return new BlockInfo(version, (short) id, (byte) meta);
    }

    public static BlockInfo info(Version version, int id) {
      return info(version, id, 0);
    }

    public static BlockInfo fallback(Version version, VirtualBlock fallback) {
      return new BlockInfo(version, fallback.getBlockInfos().get(version));
    }
  }
}
