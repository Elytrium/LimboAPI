/*
 * Copyright (C) 2021 - 2022 Elytrium
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.elytrium.limboapi.server.world;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.velocitypowered.api.network.ProtocolVersion;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.elytrium.limboapi.LimboAPI;
import net.elytrium.limboapi.api.material.Item;
import net.elytrium.limboapi.api.material.VirtualItem;

public class SimpleItem implements VirtualItem {

  private static final Gson GSON = new Gson();

  private static final Map<Item, SimpleItem> LEGACY_ID_MAP = new EnumMap<>(Item.class);

  private final Map<Version, Short> versionIDs = new EnumMap<>(Version.class);

  @Override
  public short getID(ProtocolVersion version) {
    return this.getID(Version.map(version));
  }

  public short getID(Version version) {
    return this.versionIDs.get(version);
  }

  @SuppressWarnings("unchecked")
  public static void init() {
    LinkedTreeMap<String, LinkedTreeMap<String, String>> items = GSON.fromJson(
        new InputStreamReader(
            Objects.requireNonNull(LimboAPI.class.getResourceAsStream("/mapping/items.json")), StandardCharsets.UTF_8
        ),
        LinkedTreeMap.class
    );
    for (Item item : Item.values()) {
      SimpleItem simpleItem = new SimpleItem();
      items.get(String.valueOf(item.getID())).forEach((key, value) -> simpleItem.versionIDs.put(Version.parse(key), Short.parseShort(value)));
      LEGACY_ID_MAP.put(item, simpleItem);
    }
  }

  public static SimpleItem fromItem(Item item) {
    return LEGACY_ID_MAP.get(item);
  }

  public enum Version {

    LEGACY(EnumSet.range(ProtocolVersion.MINECRAFT_1_7_2, ProtocolVersion.MINECRAFT_1_12_2)),
    MINECRAFT_1_13(ProtocolVersion.MINECRAFT_1_13),
    MINECRAFT_1_13_2(ProtocolVersion.MINECRAFT_1_13_1, ProtocolVersion.MINECRAFT_1_13_2),
    MINECRAFT_1_14(EnumSet.range(ProtocolVersion.MINECRAFT_1_14, ProtocolVersion.MINECRAFT_1_14_4)),
    MINECRAFT_1_15(EnumSet.range(ProtocolVersion.MINECRAFT_1_15, ProtocolVersion.MINECRAFT_1_15_2)),
    MINECRAFT_1_16(ProtocolVersion.MINECRAFT_1_16, ProtocolVersion.MINECRAFT_1_16_1),
    MINECRAFT_1_16_2(EnumSet.range(ProtocolVersion.MINECRAFT_1_16_2, ProtocolVersion.MINECRAFT_1_16_4)),
    MINECRAFT_1_17(EnumSet.range(ProtocolVersion.MINECRAFT_1_17, ProtocolVersion.MINECRAFT_1_18_2)),
    MINECRAFT_1_19(EnumSet.range(ProtocolVersion.MINECRAFT_1_19, ProtocolVersion.MAXIMUM_VERSION));

    private static final EnumMap<ProtocolVersion, Version> MC_VERSION_TO_ITEM_VERSIONS = new EnumMap<>(ProtocolVersion.class);

    private final Set<ProtocolVersion> versions;

    Version(ProtocolVersion... versions) {
      this.versions = EnumSet.copyOf(Arrays.asList(versions));
    }

    Version(Set<ProtocolVersion> versions) {
      this.versions = versions;
    }

    public Set<ProtocolVersion> getVersions() {
      return this.versions;
    }

    static {
      for (Version version : Version.values()) {
        for (ProtocolVersion protocolVersion : version.getVersions()) {
          MC_VERSION_TO_ITEM_VERSIONS.put(protocolVersion, version);
        }
      }
    }

    public static Version parse(String from) {
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
        default: {
          return LEGACY;
        }
      }
    }

    public static Version map(ProtocolVersion protocolVersion) {
      return MC_VERSION_TO_ITEM_VERSIONS.get(protocolVersion);
    }
  }
}
