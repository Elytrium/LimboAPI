/*
 * Copyright (C) 2021 - 2024 Elytrium
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
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.elytrium.limboapi.LimboAPI;
import net.elytrium.limboapi.api.chunk.VirtualBlock;
import net.elytrium.limboapi.api.material.WorldVersion;
import org.checkerframework.checker.nullness.qual.NonNull;

public class SimpleBlock implements VirtualBlock {

  private static final Gson GSON = new Gson();
  private static final ShortObjectHashMap<SimpleBlock> LEGACY_BLOCK_STATE_IDS_MAP = new ShortObjectHashMap<>();
  private static final Map<ProtocolVersion, ShortObjectMap<Short>> MODERN_BLOCK_STATE_IDS_MAP = new EnumMap<>(ProtocolVersion.class);
  private static final ShortObjectHashMap<String> MODERN_BLOCK_STATE_PROTOCOL_ID_MAP = new ShortObjectHashMap<>();
  private static final Map<String, Map<Set<String>, Short>> MODERN_BLOCK_STATE_STRING_MAP = new HashMap<>();
  private static final Map<String, Short> MODERN_BLOCK_STRING_MAP = new HashMap<>();
  private static final ShortObjectHashMap<Map<WorldVersion, Short>> LEGACY_BLOCK_IDS_MAP = new ShortObjectHashMap<>();
  private static final Map<String, Map<String, String>> DEFAULT_PROPERTIES_MAP = new HashMap<>();
  private static final Map<String, String> MODERN_ID_REMAP = new HashMap<>();

  public static final SimpleBlock AIR = new SimpleBlock(false, true, false, "minecraft:air", (short) 0, (short) 0);

  @SuppressWarnings("unchecked")
  public static void init() {
    LinkedTreeMap<String, String> blocks = GSON.fromJson(
        new InputStreamReader(
            Objects.requireNonNull(LimboAPI.class.getResourceAsStream("/mapping/blocks.json")),
            StandardCharsets.UTF_8
        ),
        LinkedTreeMap.class
    );

    blocks.forEach((modernId, protocolId) -> MODERN_BLOCK_STRING_MAP.put(modernId, Short.valueOf(protocolId)));

    LinkedTreeMap<String, LinkedTreeMap<String, String>> blockVersionMapping = GSON.fromJson(
        new InputStreamReader(
            Objects.requireNonNull(LimboAPI.class.getResourceAsStream("/mapping/blocks_mapping.json")),
            StandardCharsets.UTF_8
        ),
        LinkedTreeMap.class
    );

    blockVersionMapping.forEach((protocolId, versionMap) -> {
      EnumMap<WorldVersion, Short> deserializedVersionMap = new EnumMap<>(WorldVersion.class);
      versionMap.forEach((version, id) -> deserializedVersionMap.put(WorldVersion.parse(version), Short.valueOf(id)));
      LEGACY_BLOCK_IDS_MAP.put(Short.valueOf(protocolId), deserializedVersionMap);
    });

    LinkedTreeMap<String, String> blockStates = GSON.fromJson(
        new InputStreamReader(Objects.requireNonNull(LimboAPI.class.getResourceAsStream("/mapping/blockstates.json")), StandardCharsets.UTF_8),
        LinkedTreeMap.class
    );
    blockStates.forEach((key, value) -> {
      MODERN_BLOCK_STATE_PROTOCOL_ID_MAP.put(Short.valueOf(value), key);

      String[] stringIDArgs = key.split("\\[");
      if (!MODERN_BLOCK_STATE_STRING_MAP.containsKey(stringIDArgs[0])) {
        MODERN_BLOCK_STATE_STRING_MAP.put(stringIDArgs[0], new HashMap<>());
      }

      if (stringIDArgs.length == 1) {
        MODERN_BLOCK_STATE_STRING_MAP.get(stringIDArgs[0]).put(null, Short.valueOf(value));
      } else {
        stringIDArgs[1] = stringIDArgs[1].substring(0, stringIDArgs[1].length() - 1);
        MODERN_BLOCK_STATE_STRING_MAP.get(stringIDArgs[0]).put(new HashSet<>(Arrays.asList(stringIDArgs[1].split(","))), Short.valueOf(value));
      }
    });

    LinkedTreeMap<String, String> legacyBlocks = GSON.fromJson(
        new InputStreamReader(Objects.requireNonNull(LimboAPI.class.getResourceAsStream("/mapping/legacyblocks.json")), StandardCharsets.UTF_8),
        LinkedTreeMap.class
    );
    legacyBlocks.forEach((legacyBlockID, modernID)
        -> LEGACY_BLOCK_STATE_IDS_MAP.put(Short.valueOf(legacyBlockID), solid(Short.parseShort(modernID))));

    LEGACY_BLOCK_STATE_IDS_MAP.put((short) 0, AIR);

    LinkedTreeMap<String, LinkedTreeMap<String, String>> modernMap = GSON.fromJson(
        new InputStreamReader(Objects.requireNonNull(LimboAPI.class.getResourceAsStream("/mapping/blockstates_mapping.json")), StandardCharsets.UTF_8),
        LinkedTreeMap.class
    );

    modernMap.forEach((modernID, versionMap) -> {
      Short id = null;
      for (ProtocolVersion version : ProtocolVersion.SUPPORTED_VERSIONS) {
        id = Short.valueOf(versionMap.getOrDefault(version.toString(), String.valueOf(id)));
        SimpleBlock.MODERN_BLOCK_STATE_IDS_MAP.computeIfAbsent(version, k -> new ShortObjectHashMap<>()).put(Short.parseShort(modernID), id);
      }
    });

    LinkedTreeMap<String, LinkedTreeMap<String, String>> properties = GSON.fromJson(
        new InputStreamReader(
            Objects.requireNonNull(LimboAPI.class.getResourceAsStream("/mapping/defaultblockproperties.json")),
            StandardCharsets.UTF_8
        ),
        LinkedTreeMap.class
    );
    properties.forEach((key, value) -> DEFAULT_PROPERTIES_MAP.put(key, new HashMap<>(value)));

    LinkedTreeMap<String, String> modernIdRemap = GSON.fromJson(
        new InputStreamReader(
            Objects.requireNonNull(LimboAPI.class.getResourceAsStream("/mapping/modern_id_remap.json")),
            StandardCharsets.UTF_8
        ),
        LinkedTreeMap.class
    );
    MODERN_ID_REMAP.putAll(modernIdRemap);
  }

  private final boolean solid;
  private final boolean air;
  private final boolean motionBlocking; // 1.14+
  private final String modernID;
  private final short blockStateID;
  private final short blockID;

  public SimpleBlock(boolean solid, boolean air, boolean motionBlocking, short blockStateID) {
    this(solid, air, motionBlocking, MODERN_BLOCK_STATE_PROTOCOL_ID_MAP.get(blockStateID), blockStateID);
  }

  public SimpleBlock(boolean solid, boolean air, boolean motionBlocking, String modernID, short blockStateID) {
    this(solid, air, motionBlocking, modernID, blockStateID, MODERN_BLOCK_STRING_MAP.get(modernID.split("\\[")[0]));
  }

  public SimpleBlock(boolean solid, boolean air, boolean motionBlocking, String modernID, short blockStateID, short blockID) {
    this.solid = solid;
    this.air = air;
    this.motionBlocking = motionBlocking;
    this.modernID = modernID;
    this.blockStateID = blockStateID;
    this.blockID = blockID;
  }

  public SimpleBlock(boolean solid, boolean air, boolean motionBlocking, String modernID, Map<String, String> properties) {
    this(solid, air, motionBlocking, modernID, transformID(modernID, properties));
  }

  public SimpleBlock(boolean solid, boolean air, boolean motionBlocking, String modernID, Map<String, String> properties, short blockID) {
    this(solid, air, motionBlocking, modernID, transformID(modernID, properties), blockID);
  }

  public SimpleBlock(SimpleBlock block) {
    this.solid = block.solid;
    this.air = block.air;
    this.motionBlocking = block.motionBlocking;
    this.modernID = block.modernID;
    this.blockStateID = block.blockStateID;
    this.blockID = block.blockID;
  }

  @Override
  public short getModernID() {
    return this.blockStateID;
  }

  @Override
  public String getModernStringID() {
    return this.modernID;
  }

  @Override
  public short getID(ProtocolVersion version) {
    return this.getBlockStateID(version);
  }

  @Override
  public short getBlockID(WorldVersion version) {
    return LEGACY_BLOCK_IDS_MAP.get(this.blockID).get(version);
  }

  @Override
  public short getBlockID(ProtocolVersion version) {
    return this.getBlockID(WorldVersion.from(version));
  }

  @Override
  public boolean isSupportedOn(WorldVersion version) {
    return LEGACY_BLOCK_IDS_MAP.get(this.blockID).containsKey(version);
  }

  @Override
  public boolean isSupportedOn(ProtocolVersion version) {
    return this.isSupportedOn(WorldVersion.from(version));
  }

  @Override
  public short getBlockStateID(ProtocolVersion version) {
    return MODERN_BLOCK_STATE_IDS_MAP.get(version).getOrDefault(this.blockStateID, this.blockStateID);
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

  public static VirtualBlock fromModernID(String modernID) {
    String[] deserializedModernId = modernID.split("[\\[\\]]");
    if (deserializedModernId.length < 2) {
      return fromModernID(modernID, Map.of());
    } else {
      Map<String, String> properties = new HashMap<>();
      for (String property : deserializedModernId[1].split(",")) {
        String[] propertyKeyValue = property.split("=");
        properties.put(propertyKeyValue[0], propertyKeyValue[1]);
      }

      return fromModernID(deserializedModernId[0], properties);
    }
  }

  public static VirtualBlock fromModernID(String modernID, Map<String, String> properties) {
    modernID = remapModernID(modernID);
    return solid(modernID, transformID(modernID, properties));
  }

  private static short transformID(String modernID, Map<String, String> properties) {
    Map<String, String> defaultProperties = DEFAULT_PROPERTIES_MAP.get(modernID);
    if (defaultProperties == null || defaultProperties.isEmpty()) {
      return transformID(modernID, (Set<String>) null);
    } else {
      Set<String> propertiesSet = new HashSet<>();
      defaultProperties.forEach((key, value) -> {
        if (properties != null) {
          value = properties.getOrDefault(key, value);
        }

        propertiesSet.add(key + "=" + value.toLowerCase(Locale.ROOT));
      });
      return transformID(modernID, propertiesSet);
    }
  }

  private static short transformID(String modernID, Set<String> properties) {
    Map<Set<String>, Short> blockInfo = MODERN_BLOCK_STATE_STRING_MAP.get(modernID);

    if (blockInfo == null) {
      LimboAPI.getLogger().warn("Block " + modernID + " is not supported, and was replaced with air.");
      return AIR.getModernID();
    }

    Short id;
    if (properties == null || properties.isEmpty()) {
      id = blockInfo.get(null);
    } else {
      id = blockInfo.get(properties);
    }

    if (id == null) {
      LimboAPI.getLogger().warn("Block " + modernID + " is not supported with " + properties + " properties, and was replaced with air.");
      return AIR.getModernID();
    }

    return id;
  }

  private static String remapModernID(String modernID) {
    String strippedID = modernID.split("\\[")[0];
    String remappedID = MODERN_ID_REMAP.get(strippedID);
    if (remappedID != null) {
      modernID = remappedID + modernID.substring(strippedID.length());
    }

    return modernID;
  }

  @NonNull
  public static SimpleBlock solid(short id) {
    return solid(true, MODERN_BLOCK_STATE_PROTOCOL_ID_MAP.get(id), id);
  }

  @NonNull
  public static SimpleBlock solid(String modernID, short id) {
    return solid(true, remapModernID(modernID), id);
  }

  @NonNull
  public static SimpleBlock solid(boolean motionBlocking, short id) {
    return new SimpleBlock(true, false, motionBlocking, MODERN_BLOCK_STATE_PROTOCOL_ID_MAP.get(id), id);
  }

  @NonNull
  public static SimpleBlock solid(boolean motionBlocking, String modernID, short id) {
    return new SimpleBlock(true, false, motionBlocking, remapModernID(modernID), id);
  }

  @NonNull
  public static SimpleBlock nonSolid(short id) {
    return nonSolid(true, MODERN_BLOCK_STATE_PROTOCOL_ID_MAP.get(id), id);
  }

  @NonNull
  public static SimpleBlock nonSolid(String modernID, short id) {
    return nonSolid(true, remapModernID(modernID), id);
  }

  @NonNull
  public static SimpleBlock nonSolid(boolean motionBlocking, short id) {
    return new SimpleBlock(false, false, motionBlocking, MODERN_BLOCK_STATE_PROTOCOL_ID_MAP.get(id), id);
  }

  @NonNull
  public static SimpleBlock nonSolid(boolean motionBlocking, String modernID, short id) {
    return new SimpleBlock(false, false, motionBlocking, remapModernID(modernID), id);
  }

  @NonNull
  public static SimpleBlock fromLegacyID(short id) {
    if (LEGACY_BLOCK_STATE_IDS_MAP.containsKey(id)) {
      return LEGACY_BLOCK_STATE_IDS_MAP.get(id);
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
        + ", id=" + this.blockStateID
        + "}";
  }
}
