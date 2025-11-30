/*
 * Copyright (C) 2021 - 2025 Elytrium
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

import com.google.gson.internal.LinkedTreeMap;
import com.velocitypowered.api.network.ProtocolVersion;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ShortOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ShortOpenHashMap;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import net.elytrium.limboapi.LimboAPI;
import net.elytrium.limboapi.api.world.chunk.block.VirtualBlock;
import net.elytrium.limboapi.api.world.WorldVersion;
import net.elytrium.limboapi.utils.JsonUtil;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * @param motionBlocking Added in version 1.14
 */
public record SimpleBlock(String modernId, short blockId, short blockStateId, boolean air, boolean solid, boolean motionBlocking) implements VirtualBlock {

  private static final Short2ObjectOpenHashMap<String> MODERN_BLOCK_STATE_PROTOCOL_ID_MAP;
  private static final Object2ObjectOpenHashMap<String, Object2ShortOpenHashMap<ObjectOpenHashSet<String>>> MODERN_BLOCK_STATE_STRING_MAP;
  private static final EnumMap<ProtocolVersion, Short2ShortOpenHashMap> MODERN_BLOCK_STATE_IDS_MAP;
  private static final Object2ShortOpenHashMap<String> MODERN_BLOCK_STRING_MAP;
  private static final Short2ObjectOpenHashMap<EnumMap<WorldVersion, Short>> LEGACY_BLOCK_IDS_MAP;
  private static final Short2ObjectOpenHashMap<SimpleBlock> LEGACY_BLOCK_STATE_IDS_MAP;
  private static final Object2ObjectOpenHashMap<String, Object2ObjectOpenHashMap<String, String>> DEFAULT_PROPERTIES_MAP;
  private static final Object2ObjectOpenHashMap<String, String> MODERN_ID_REMAP;

  public static final SimpleBlock AIR = new SimpleBlock("minecraft:air", (short) 0, (short) 0, true, false, false);

  public SimpleBlock(String modernId, Map<String, String> properties, boolean air, boolean solid, boolean motionBlocking) {
    this(modernId, SimpleBlock.transformId(modernId, properties), air, solid, motionBlocking);
  }

  public SimpleBlock(short blockStateId, boolean air, boolean solid, boolean motionBlocking) {
    this(SimpleBlock.MODERN_BLOCK_STATE_PROTOCOL_ID_MAP.get(blockStateId), blockStateId, air, solid, motionBlocking);
  }

  public SimpleBlock(String modernId, short blockStateId, boolean air, boolean solid, boolean motionBlocking) {
    this(modernId, SimpleBlock.findBlockId(modernId), blockStateId, air, solid, motionBlocking);
  }

  private static short findBlockId(String modernId) {
    int bracketIndex = modernId.indexOf('[');
    short id = SimpleBlock.MODERN_BLOCK_STRING_MAP.getShort(bracketIndex == -1 ? modernId : modernId.substring(0, bracketIndex));
    if (id == Short.MIN_VALUE) {
      throw new IllegalStateException("failed to find local id for specific block: " + modernId);
    }

    return id;
  }

  public SimpleBlock(String modernID, short blockId, Map<String, String> properties, boolean air, boolean solid, boolean motionBlocking) {
    this(modernID, blockId, SimpleBlock.transformId(modernID, properties), air, solid, motionBlocking);
  }

  public SimpleBlock(SimpleBlock block) {
    this(block.modernId, block.blockId, block.blockStateId, block.air, block.solid, block.motionBlocking);
  }

  @Override
  public short blockStateId(ProtocolVersion version) {
    return SimpleBlock.MODERN_BLOCK_STATE_IDS_MAP.get(version).getOrDefault(this.blockStateId, this.blockStateId);
  }

  @Override
  public short blockId(WorldVersion version) {
    return SimpleBlock.LEGACY_BLOCK_IDS_MAP.get(this.blockId).getOrDefault(version, this.blockId);
  }

  @Override
  public short blockId(ProtocolVersion version) {
    return this.blockId(WorldVersion.from(version));
  }

  @Override
  public boolean isSupportedOn(WorldVersion version) {
    return SimpleBlock.LEGACY_BLOCK_IDS_MAP.get(this.blockId).containsKey(version);
  }

  @Override
  public boolean isSupportedOn(ProtocolVersion version) {
    return this.isSupportedOn(WorldVersion.from(version));
  }

  static {
    var blockStates = JsonUtil.<Number>parse(LimboAPI.class.getResourceAsStream("/mappings/block_states.json"));
    MODERN_BLOCK_STATE_PROTOCOL_ID_MAP = new Short2ObjectOpenHashMap<>(blockStates.size());
    MODERN_BLOCK_STATE_STRING_MAP = new Object2ObjectOpenHashMap<>(blockStates.size());
    blockStates.forEach((blockState, number) -> {
      short value = number.shortValue();
      SimpleBlock.MODERN_BLOCK_STATE_PROTOCOL_ID_MAP.put(value, blockState);
      int index = blockState.indexOf('[');
      if (index == -1) {
        SimpleBlock.MODERN_BLOCK_STATE_STRING_MAP.computeIfAbsent(blockState, key -> {
          var map = new Object2ShortOpenHashMap<ObjectOpenHashSet<String>>();
          map.defaultReturnValue(Short.MIN_VALUE);
          return map;
        }).put(null, value);
      } else {
        SimpleBlock.MODERN_BLOCK_STATE_STRING_MAP.computeIfAbsent(blockState.substring(0, index), key -> {
          var map = new Object2ShortOpenHashMap<ObjectOpenHashSet<String>>();
          map.defaultReturnValue(Short.MIN_VALUE);
          return map;
        }).put(new ObjectOpenHashSet<>(blockState.substring(index + 1, blockState.length() - 1).split(",")), value);
      }
    });

    var blocksMappings = JsonUtil.<LinkedTreeMap<String, Number>>parse(LimboAPI.class.getResourceAsStream("/mappings/blocks_mappings.json"));
    MODERN_BLOCK_STRING_MAP = new Object2ShortOpenHashMap<>(blocksMappings.size());
    MODERN_BLOCK_STRING_MAP.defaultReturnValue(Short.MIN_VALUE);
    LEGACY_BLOCK_IDS_MAP = new Short2ObjectOpenHashMap<>(blocksMappings.size());
    String maximumVersionProtocol = Integer.toString(ProtocolVersion.MAXIMUM_VERSION.getProtocol());
    blocksMappings.forEach((modernId, versions) -> {
      short modernProtocolId = versions.get(maximumVersionProtocol).shortValue();
      SimpleBlock.MODERN_BLOCK_STRING_MAP.put(modernId, modernProtocolId);
      EnumMap<WorldVersion, Short> deserializedVersionMap = new EnumMap<>(WorldVersion.class);
      versions.forEach((version, id) -> deserializedVersionMap.put(WorldVersion.from(ProtocolVersion.getProtocolVersion(Integer.parseInt(version))), id.shortValue()));
      SimpleBlock.LEGACY_BLOCK_IDS_MAP.put(modernProtocolId, deserializedVersionMap);
    });

    var blockStatesMappings = JsonUtil.<LinkedTreeMap<String, Number>>parse(LimboAPI.class.getResourceAsStream("/mappings/block_states_mappings.json"));
    MODERN_BLOCK_STATE_IDS_MAP = new EnumMap<>(ProtocolVersion.class);
    blockStatesMappings.forEach((modernId, versions) -> {
      Short id = null;
      for (ProtocolVersion version : ProtocolVersion.SUPPORTED_VERSIONS) {
        id = versions.getOrDefault(Integer.toString(version.getProtocol()), id).shortValue();
        SimpleBlock.MODERN_BLOCK_STATE_IDS_MAP.computeIfAbsent(version, key -> new Short2ShortOpenHashMap()).put(Short.parseShort(modernId), id.shortValue());
      }
    });

    var properties = JsonUtil.<LinkedTreeMap<String, String>>parse(LimboAPI.class.getResourceAsStream("/mappings/default_block_properties.json"));
    DEFAULT_PROPERTIES_MAP = new Object2ObjectOpenHashMap<>(properties.size());
    properties.forEach((key, value) -> SimpleBlock.DEFAULT_PROPERTIES_MAP.put(key, new Object2ObjectOpenHashMap<>(value)));

    MODERN_ID_REMAP = new Object2ObjectOpenHashMap<>(JsonUtil.parse(LimboAPI.class.getResourceAsStream("/mappings/modern_block_id_remap.json")));

    var legacyBlocks = JsonUtil.<Number>parse(LimboAPI.class.getResourceAsStream("/mappings/legacy_blocks.json"));
    LEGACY_BLOCK_STATE_IDS_MAP = new Short2ObjectOpenHashMap<>(legacyBlocks.size() + 1);
    legacyBlocks.forEach((legacy, modern) -> SimpleBlock.LEGACY_BLOCK_STATE_IDS_MAP.put(Short.parseShort(legacy), SimpleBlock.solid(modern.shortValue())));
    SimpleBlock.LEGACY_BLOCK_STATE_IDS_MAP.put((short) 0, SimpleBlock.AIR);

    SimpleBlock.MODERN_BLOCK_STATE_STRING_MAP.trim();
    SimpleBlock.MODERN_BLOCK_STATE_STRING_MAP.values().forEach(Object2ShortOpenHashMap::trim);
    SimpleBlock.MODERN_BLOCK_STATE_IDS_MAP.values().forEach(Short2ShortOpenHashMap::trim);
  }

  public static VirtualBlock fromModernId(String modernId) {
    String[] deserializedModernId = modernId.split("[\\[\\]]");
    if (deserializedModernId.length < 2) {
      return SimpleBlock.fromModernId(modernId, Collections.emptyMap());
    } else {
      Object2ObjectOpenHashMap<String, String> properties = new Object2ObjectOpenHashMap<>();
      for (String property : deserializedModernId[1].split(",")) {
        String[] propertyKeyValue = property.split("=");
        properties.put(propertyKeyValue[0], propertyKeyValue[1]);
      }

      return SimpleBlock.fromModernId(deserializedModernId[0], properties);
    }
  }

  public static VirtualBlock fromModernId(String modernId, Map<String, String> properties) {
    modernId = SimpleBlock.remapModernId(modernId);
    return SimpleBlock.solid(modernId, SimpleBlock.transformId(modernId, properties));
  }

  private static short transformId(String modernId, Map<String, String> properties) {
    var defaultProperties = SimpleBlock.DEFAULT_PROPERTIES_MAP.get(modernId);
    if (defaultProperties == null || defaultProperties.isEmpty()) {
      return SimpleBlock.transformId(modernId, (ObjectOpenHashSet<String>) null);
    } else {
      ObjectOpenHashSet<String> propertiesSet = new ObjectOpenHashSet<>();
      defaultProperties.forEach((key, value) -> {
        if (properties != null) {
          value = properties.getOrDefault(key, value);
        }

        propertiesSet.add(key + "=" + value.toLowerCase(Locale.ROOT));
      });
      return SimpleBlock.transformId(modernId, propertiesSet);
    }
  }

  private static short transformId(String modernId, ObjectOpenHashSet<String> properties) {
    var blockInfo = SimpleBlock.MODERN_BLOCK_STATE_STRING_MAP.get(modernId);
    if (blockInfo == null) {
      LimboAPI.getLogger().warn("Block {} is not supported, and was replaced with air.", modernId);
      return SimpleBlock.AIR.blockStateId;
    }

    short id = properties == null || properties.isEmpty() ? blockInfo.getShort(null) : blockInfo.getShort(properties);
    if (id == Short.MIN_VALUE) {
      LimboAPI.getLogger().warn("Block {} is not supported with {} properties, and was replaced with air.", modernId, properties);
      return SimpleBlock.AIR.blockStateId;
    }

    return id;
  }

  @NonNull
  public static SimpleBlock solid(short blockStateId) {
    return SimpleBlock.solid(SimpleBlock.MODERN_BLOCK_STATE_PROTOCOL_ID_MAP.get(blockStateId), blockStateId, true);
  }

  @NonNull
  public static SimpleBlock solid(String modernId, short blockStateId) {
    return SimpleBlock.solid(SimpleBlock.remapModernId(modernId), blockStateId, true);
  }

  @NonNull
  public static SimpleBlock solid(short blockStateId, boolean motionBlocking) {
    return new SimpleBlock(SimpleBlock.MODERN_BLOCK_STATE_PROTOCOL_ID_MAP.get(blockStateId), blockStateId, false, true, motionBlocking);
  }

  @NonNull
  public static SimpleBlock solid(String modernId, short blockStateId, boolean motionBlocking) {
    return new SimpleBlock(SimpleBlock.remapModernId(modernId), blockStateId, false, true, motionBlocking);
  }

  @NonNull
  public static SimpleBlock nonSolid(short blockStateId) {
    return nonSolid(SimpleBlock.MODERN_BLOCK_STATE_PROTOCOL_ID_MAP.get(blockStateId), blockStateId, true);
  }

  @NonNull
  public static SimpleBlock nonSolid(String modernId, short blockStateId) {
    return nonSolid(SimpleBlock.remapModernId(modernId), blockStateId, true);
  }

  @NonNull
  public static SimpleBlock nonSolid(short blockStateId, boolean motionBlocking) {
    return new SimpleBlock(SimpleBlock.MODERN_BLOCK_STATE_PROTOCOL_ID_MAP.get(blockStateId), blockStateId, false, false, motionBlocking);
  }

  @NonNull
  public static SimpleBlock nonSolid(String modernId, short blockStateId, boolean motionBlocking) {
    return new SimpleBlock(SimpleBlock.remapModernId(modernId), blockStateId, false, false, motionBlocking);
  }

  @NonNull
  public static SimpleBlock fromLegacyId(short legacyId) {
    SimpleBlock block = SimpleBlock.LEGACY_BLOCK_STATE_IDS_MAP.get(legacyId);
    if (block == null) {
      LimboAPI.getLogger().warn("Block #{} is not supported, and was replaced with air", legacyId);
      return SimpleBlock.AIR;
    } else {
      return block;
    }
  }

  private static String remapModernId(String modernId) {
    int index = modernId.indexOf('[');
    String remappedId = SimpleBlock.MODERN_ID_REMAP.get(index == -1 ? modernId : modernId.substring(0, index));
    return remappedId == null
        ? modernId
        : index == -1
            ? remappedId
            : remappedId + modernId.substring(index);
  }
}
