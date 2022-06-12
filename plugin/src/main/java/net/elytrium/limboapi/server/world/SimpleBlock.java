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
      String[] stringIdArgs = key.split("\\[");
      if (!MODERN_STRING_MAP.containsKey(stringIdArgs[0])) {
        MODERN_STRING_MAP.put(stringIdArgs[0], new HashMap<>());
      }

      if (stringIdArgs.length == 1) {
        MODERN_STRING_MAP.get(stringIdArgs[0]).put(null, Short.valueOf(value));
      } else {
        stringIdArgs[1] = stringIdArgs[1].substring(0, stringIdArgs[1].length() - 1);
        MODERN_STRING_MAP.get(stringIdArgs[0]).put(new HashSet<>(Arrays.asList(stringIdArgs[1].split(","))), Short.valueOf(value));
      }
    });

    LinkedTreeMap<String, String> blocks = GSON.fromJson(
        new InputStreamReader(Objects.requireNonNull(LimboAPI.class.getResourceAsStream("/mapping/blocks.json")), StandardCharsets.UTF_8),
        LinkedTreeMap.class
    );
    blocks.forEach((legacyBlockId, modernId) -> LEGACY_IDS_MAP.put(Short.valueOf(legacyBlockId), solid(Short.parseShort(modernId))));

    LEGACY_IDS_MAP.put((short) 0, AIR);

    loadModernMap("/mapping/legacyblockdata.json", MODERN_IDS_MAP);
    loadModernMap("/mapping/flatteningblockdata.json", MODERN_IDS_FLATTEN_MAP);

    LinkedTreeMap<String, String> tempLegacyFlattenMap = GSON.fromJson(new InputStreamReader(
        Objects.requireNonNull(LimboAPI.class.getResourceAsStream("/mapping/preflatteningblockdataid.json")),
        StandardCharsets.UTF_8
    ), LinkedTreeMap.class);

    tempLegacyFlattenMap.forEach((k, v) -> LEGACY_IDS_FLATTEN_MAP.put(Short.valueOf(k), Short.valueOf(v)));
  }

  @SuppressWarnings("unchecked")
  private static void loadModernMap(String resource, EnumMap<ProtocolVersion, ShortObjectMap<Short>> map) {
    LinkedTreeMap<String, LinkedTreeMap<String, String>> modernMap = GSON.fromJson(
        new InputStreamReader(Objects.requireNonNull(LimboAPI.class.getResourceAsStream(resource)), StandardCharsets.UTF_8),
        LinkedTreeMap.class
    );

    modernMap.forEach((version, idMap) -> {
      ProtocolVersion protocolVersion = ProtocolVersion.valueOf(version);
      ShortObjectMap<Short> versionIdMap = new ShortObjectHashMap<>();
      idMap.forEach((oldId, newId) -> versionIdMap.put(Short.valueOf(oldId), Short.valueOf(newId)));
      map.put(protocolVersion, versionIdMap);
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

  public SimpleBlock(boolean solid, boolean air, boolean motionBlocking, String modernId, Map<String, String> properties) {
    this.solid = solid;
    this.air = air;
    this.motionBlocking = motionBlocking;
    this.id = transformId(modernId, properties);
  }

  public SimpleBlock(SimpleBlock block) {
    this.solid = block.solid;
    this.air = block.air;
    this.motionBlocking = block.motionBlocking;
    this.id = block.id;
  }

  @Override
  public short getModernId() {
    return this.id;
  }

  @Override
  public short getId(ProtocolVersion version) {
    Short id = MODERN_IDS_MAP.get(version).get(this.id);
    return this.getFlattenId(version, Objects.requireNonNullElse(id, this.id));
  }

  private short getFlattenId(ProtocolVersion version, Short id) {
    Short flattenId;
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_12_2) <= 0) {
      flattenId = LEGACY_IDS_FLATTEN_MAP.get(this.id);
    } else {
      flattenId = MODERN_IDS_FLATTEN_MAP.get(version).get(this.id);
    }

    if (flattenId == null) {
      return id;
    } else {
      return flattenId;
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

  public static VirtualBlock fromModernId(String modernId, Map<String, String> properties) {
    return solid(transformId(modernId, properties));
  }

  private static short transformId(String modernId, Map<String, String> properties) {
    if (properties == null || properties.size() == 0) {
      return transformId(modernId, (Set<String>) null);
    }

    Set<String> propertiesSet = new HashSet<>();
    properties.forEach((k, v) -> propertiesSet.add(k + "=" + v));
    return transformId(modernId, propertiesSet);
  }

  private static short transformId(String modernId, Set<String> properties) {
    Short id;
    if (properties == null || properties.size() == 0) {
      id = MODERN_STRING_MAP.get(modernId).get(null);
    } else {
      id = MODERN_STRING_MAP.get(modernId).get(properties);
    }

    if (id == null) {
      LimboAPI.getLogger().warn("Block " + modernId + " is not supported, and was replaced with air.");
      return AIR.getModernId();
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
  public static SimpleBlock fromLegacyId(short id) {
    if (LEGACY_IDS_MAP.containsKey(id)) {
      return LEGACY_IDS_MAP.get(id);
    } else {
      LimboAPI.getLogger().warn("Block #" + id + " is not supported, and was replaced with air.");
      return AIR;
    }
  }
}
