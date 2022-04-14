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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.elytrium.limboapi.LimboAPI;
import net.elytrium.limboapi.api.chunk.VirtualBlock;
import org.checkerframework.checker.nullness.qual.NonNull;

@SuppressWarnings("unused")
public class SimpleBlock implements VirtualBlock {

  public static final SimpleBlock AIR = air((short) 0);

  private static final Gson gson = new Gson();
  private static final HashMap<Short, SimpleBlock> legacyIdsMap = new HashMap<>();
  private static final EnumMap<ProtocolVersion, HashMap<Short, Short>> modernIdsMap = new EnumMap<>(ProtocolVersion.class);
  private static final HashMap<String, HashMap<Set<String>, Short>> modernStringMap = new HashMap<>();

  @SuppressWarnings("unchecked")
  public static void init() {
    LinkedTreeMap<String, String> tempModernStringMap = gson.fromJson(
        new InputStreamReader(
            Objects.requireNonNull(LimboAPI.class.getResourceAsStream("/mapping/blockstates.json")),
            StandardCharsets.UTF_8
        ), LinkedTreeMap.class
    );

    tempModernStringMap.forEach((k, v) -> {
      String[] stringIdArgs = k.split("\\[");
      if (!modernStringMap.containsKey(stringIdArgs[0])) {
        modernStringMap.put(stringIdArgs[0], new HashMap<>());
      }

      if (stringIdArgs.length == 1) {
        modernStringMap.get(stringIdArgs[0]).put(null, Short.valueOf(v));
      } else {
        stringIdArgs[1] = stringIdArgs[1].substring(0, stringIdArgs[1].length() - 1);
        Set<String> props = new HashSet<>(Arrays.asList(stringIdArgs[1].split(",")));
        modernStringMap.get(stringIdArgs[0]).put(props, Short.valueOf(v));
      }
    });

    LinkedTreeMap<String, String> map = gson.fromJson(
        new InputStreamReader(
            Objects.requireNonNull(LimboAPI.class.getResourceAsStream("/mapping/blocks.json")),
            StandardCharsets.UTF_8
        ), LinkedTreeMap.class
    );

    map.forEach((legacyBlockId, modernId) -> {
      legacyIdsMap.put(Short.valueOf(legacyBlockId), solid(Short.parseShort(modernId)));
    });

    legacyIdsMap.put((short) 0, AIR);

    LinkedTreeMap<String, LinkedTreeMap<String, Double>> modernMap = gson.fromJson(
        new InputStreamReader(
            Objects.requireNonNull(LimboAPI.class.getResourceAsStream("/mapping/legacyblockdata.json")),
            StandardCharsets.UTF_8
        ), LinkedTreeMap.class
    );

    modernMap.forEach((version, idMap) -> {
      ProtocolVersion protocolVersion = ProtocolVersion.valueOf(version);
      HashMap<Short, Short> versionIdMap = new HashMap<>();
      idMap.forEach((oldId, newId) -> versionIdMap.put(Short.valueOf(oldId), newId.shortValue()));
      modernIdsMap.put(protocolVersion, versionIdMap);
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
    Short id = modernIdsMap.get(version).get(this.id);
    if (id == null) {
      return this.id;
    } else {
      return id;
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
    if (legacyIdsMap.containsKey(id)) {
      return legacyIdsMap.get(id);
    } else {
      LimboAPI.getLogger().warn("Block #" + id + " is not supported, and was replaced with air.");
      return AIR;
    }
  }

  public static VirtualBlock fromModernId(String modernId, Map<String, String> properties) {
    return solid(transformId(modernId, properties));
  }

  public static short transformId(String modernId, Map<String, String> properties) {
    if (properties == null || properties.size() == 0) {
      return transformId(modernId, (Set<String>) null);
    }

    Set<String> propertiesSet = new HashSet<>();
    properties.forEach((k, v) -> propertiesSet.add(k + "=" + v));
    return transformId(modernId, propertiesSet);
  }

  public static short transformId(String modernId, Set<String> properties) {
    Short id;

    if (properties == null || properties.size() == 0) {
      id = modernStringMap.get(modernId).get(null);
    } else {
      id = modernStringMap.get(modernId).get(properties);
    }

    if (id == null) {
      LimboAPI.getLogger().warn("Block " + modernId + " is not supported, and was replaced with air.");
      return AIR.getModernId();
    } else {
      return id;
    }
  }

  @NonNull
  public static SimpleBlock air(short id) {
    return new SimpleBlock(false, true, false, id);
  }
}
