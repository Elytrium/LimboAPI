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
import io.netty.util.collection.ShortObjectHashMap;
import io.netty.util.collection.ShortObjectMap;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.elytrium.limboapi.LimboAPI;
import net.elytrium.limboapi.api.chunk.VirtualBlock;
import org.checkerframework.checker.nullness.qual.NonNull;

public class SimpleBlock implements VirtualBlock {

  public static final SimpleBlock AIR = new SimpleBlock(false, true, false, (short) 0);

  private static final Gson GSON = new Gson();
  private static final ShortObjectHashMap<SimpleBlock> LEGACY_IDS_MAP = new ShortObjectHashMap<>();
  private static final ShortObjectHashMap<Short> LEGACY_IDS_FLATTEN_MAP = new ShortObjectHashMap<>();
  private static final EnumMap<ProtocolVersion, ShortObjectMap<Short>> MODERN_IDS_MAP = new EnumMap<>(ProtocolVersion.class);
  private static final EnumMap<ProtocolVersion, ShortObjectMap<Short>> MODERN_IDS_FLATTEN_MAP = new EnumMap<>(ProtocolVersion.class);
  private static final HashMap<String, HashMap<Set<String>, Short>> MODERN_STRING_MAP = new HashMap<>();

  @SuppressWarnings("unchecked")
  public static void init() {
    LinkedTreeMap<String, String> blockStates = GSON.fromJson(
        new InputStreamReader(Objects.requireNonNull(LimboAPI.class.getResourceAsStream("/mapping/blockstates.json")), StandardCharsets.UTF_8),
        LinkedTreeMap.class
    );
    blockStates.forEach((key, value) -> {
      String[] stringIDArgs = key.split("\\[");
      if (!MODERN_STRING_MAP.containsKey(stringIDArgs[0])) {
        MODERN_STRING_MAP.put(stringIDArgs[0], new HashMap<>());
      }

      if (stringIDArgs.length == 1) {
        MODERN_STRING_MAP.get(stringIDArgs[0]).put(null, Short.valueOf(value));
      } else {
        stringIDArgs[1] = stringIDArgs[1].substring(0, stringIDArgs[1].length() - 1);
        MODERN_STRING_MAP.get(stringIDArgs[0]).put(new HashSet<>(Arrays.asList(stringIDArgs[1].split(","))), Short.valueOf(value));
      }
    });

    LinkedTreeMap<String, String> blocks = GSON.fromJson(
        new InputStreamReader(Objects.requireNonNull(LimboAPI.class.getResourceAsStream("/mapping/blocks.json")), StandardCharsets.UTF_8),
        LinkedTreeMap.class
    );
    blocks.forEach((legacyBlockID, modernID) -> LEGACY_IDS_MAP.put(Short.valueOf(legacyBlockID), solid(Short.parseShort(modernID))));

    LEGACY_IDS_MAP.put((short) 0, AIR);

    loadModernMap("/mapping/legacyblockdata.json", MODERN_IDS_MAP);
    loadModernMap("/mapping/flatteningblockdata.json", MODERN_IDS_FLATTEN_MAP);

    LinkedTreeMap<String, String> tempLegacyFlattenMap = GSON.fromJson(
        new InputStreamReader(
            Objects.requireNonNull(LimboAPI.class.getResourceAsStream("/mapping/preflatteningblockdataid.json")),
            StandardCharsets.UTF_8
        ),
        LinkedTreeMap.class
    );

    tempLegacyFlattenMap.forEach((key, value) -> LEGACY_IDS_FLATTEN_MAP.put(Short.valueOf(key), Short.valueOf(value)));
  }

  @SuppressWarnings("unchecked")
  private static void loadModernMap(String resource, EnumMap<ProtocolVersion, ShortObjectMap<Short>> map) {
    LinkedTreeMap<String, LinkedTreeMap<String, String>> modernMap = GSON.fromJson(
        new InputStreamReader(Objects.requireNonNull(LimboAPI.class.getResourceAsStream(resource)), StandardCharsets.UTF_8),
        LinkedTreeMap.class
    );

    modernMap.forEach((version, idMap) -> {
      ShortObjectMap<Short> versionIDMap = new ShortObjectHashMap<>();
      idMap.forEach((oldID, newID) -> versionIDMap.put(Short.valueOf(oldID), Short.valueOf(newID)));
      map.put(ProtocolVersion.valueOf(version), versionIDMap);
    });
  }

  private final boolean solid;
  private final boolean air;
  private final boolean motionBlocking; // 1.14+
  private final short id;

  public SimpleBlock(boolean solid, boolean air, boolean motionBlocking, short id) {
    this.solid = solid;
    this.air = air;
    this.motionBlocking = motionBlocking;
    this.id = id;
  }

  public SimpleBlock(boolean solid, boolean air, boolean motionBlocking, String modernID, Map<String, String> properties) {
    this.solid = solid;
    this.air = air;
    this.motionBlocking = motionBlocking;
    this.id = transformID(modernID, properties);
  }

  public SimpleBlock(SimpleBlock block) {
    this.solid = block.solid;
    this.air = block.air;
    this.motionBlocking = block.motionBlocking;
    this.id = block.id;
  }

  @Override
  public short getModernID() {
    return this.id;
  }

  @Override
  public short getID(ProtocolVersion version) {
    Short id = MODERN_IDS_MAP.get(version).get(this.id);
    return this.getFlattenID(version, Objects.requireNonNullElse(id, this.id));
  }

  private short getFlattenID(ProtocolVersion version, Short id) {
    Short flattenID;
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_12_2) <= 0) {
      flattenID = LEGACY_IDS_FLATTEN_MAP.get(this.id);
    } else {
      flattenID = MODERN_IDS_FLATTEN_MAP.get(version).get(this.id);
    }

    if (flattenID == null) {
      return id;
    } else {
      return flattenID;
    }
  }

  @Override
  public boolean isSolid() {
    return this.solid;
  }

  @Override
  public boolean isAir() {
    return this.air;
  }

  @Override
  public boolean isMotionBlocking() {
    return this.motionBlocking;
  }

  public static VirtualBlock fromModernID(String modernID, Map<String, String> properties) {
    return solid(transformID(modernID, properties));
  }

  private static short transformID(String modernID, Map<String, String> properties) {
    if (properties == null || properties.size() == 0) {
      return transformID(modernID, (Set<String>) null);
    } else {
      Set<String> propertiesSet = new HashSet<>();
      properties.forEach((key, value) -> propertiesSet.add(key + "=" + value));
      return transformID(modernID, propertiesSet);
    }
  }

  private static short transformID(String modernID, Set<String> properties) {
    Short id;
    if (properties == null || properties.size() == 0) {
      id = MODERN_STRING_MAP.get(modernID).get(null);
    } else {
      id = MODERN_STRING_MAP.get(modernID).get(properties);
    }

    if (id == null) {
      LimboAPI.getLogger().warn("Block " + modernID + " is not supported, and was replaced with air.");
      return AIR.getModernID();
    } else {
      return id;
    }
  }

  @NonNull
  public static SimpleBlock solid(short id) {
    return solid(true, id);
  }

  @NonNull
  public static SimpleBlock solid(boolean motionBlocking, short id) {
    return new SimpleBlock(true, false, motionBlocking, id);
  }

  @NonNull
  public static SimpleBlock nonSolid(short id) {
    return nonSolid(true, id);
  }

  @NonNull
  public static SimpleBlock nonSolid(boolean motionBlocking, short id) {
    return new SimpleBlock(false, false, motionBlocking, id);
  }

  @NonNull
  public static SimpleBlock fromLegacyID(short id) {
    if (LEGACY_IDS_MAP.containsKey(id)) {
      return LEGACY_IDS_MAP.get(id);
    } else {
      LimboAPI.getLogger().warn("Block #" + id + " is not supported, and was replaced with air.");
      return AIR;
    }
  }

  @Override
  public String toString() {
    return "SimpleBlock{"
        + "solid=" + this.solid
        + ", air=" + this.air
        + ", motionBlocking=" + this.motionBlocking
        + ", id=" + this.id
        + "}";
  }
}
