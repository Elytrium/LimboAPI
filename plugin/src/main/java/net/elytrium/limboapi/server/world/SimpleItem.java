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
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import net.elytrium.limboapi.LimboAPI;
import net.elytrium.limboapi.api.material.Item;
import net.elytrium.limboapi.api.material.VirtualItem;
import net.elytrium.limboapi.api.material.WorldVersion;

public class SimpleItem implements VirtualItem {

  private static final Gson GSON = new Gson();

  private static final Map<String, SimpleItem> MODERN_ID_MAP = new HashMap<>();
  private static final Map<Integer, SimpleItem> LEGACY_ID_MAP = new HashMap<>();

  private final String modernId;
  private final Map<WorldVersion, Short> versionIDs = new EnumMap<>(WorldVersion.class);

  public SimpleItem(String modernId) {
    this.modernId = modernId;
  }

  @Override
  public short getID(ProtocolVersion version) {
    return this.getID(WorldVersion.from(version));
  }

  @Override
  public short getID(WorldVersion version) {
    return this.versionIDs.get(version);
  }

  @Override
  public boolean isSupportedOn(ProtocolVersion version) {
    return this.isSupportedOn(WorldVersion.from(version));
  }

  @Override
  public boolean isSupportedOn(WorldVersion version) {
    return this.versionIDs.containsKey(version);
  }

  public String getModernID() {
    return this.modernId;
  }

  @SuppressWarnings("unchecked")
  public static void init() {
    LinkedTreeMap<String, LinkedTreeMap<String, String>> itemsMapping = GSON.fromJson(
        new InputStreamReader(
            Objects.requireNonNull(LimboAPI.class.getResourceAsStream("/mapping/items_mapping.json")), StandardCharsets.UTF_8
        ),
        LinkedTreeMap.class
    );

    LinkedTreeMap<String, String> modernItems = GSON.fromJson(
        new InputStreamReader(
            Objects.requireNonNull(LimboAPI.class.getResourceAsStream("/mapping/items.json")), StandardCharsets.UTF_8
        ),
        LinkedTreeMap.class
    );

    LinkedTreeMap<String, String> legacyItems = GSON.fromJson(
        new InputStreamReader(
            Objects.requireNonNull(LimboAPI.class.getResourceAsStream("/mapping/legacyitems.json")), StandardCharsets.UTF_8
        ),
        LinkedTreeMap.class
    );

    modernItems.forEach((modernId, modernProtocolId) -> {
      SimpleItem simpleItem = new SimpleItem(modernId);
      itemsMapping.get(modernProtocolId).forEach((key, value) -> simpleItem.versionIDs.put(WorldVersion.parse(key), Short.parseShort(value)));
      MODERN_ID_MAP.put(modernId, simpleItem);
    });

    legacyItems.forEach((legacyProtocolId, modernId) -> LEGACY_ID_MAP.put(Integer.parseInt(legacyProtocolId), MODERN_ID_MAP.get(modernId)));
  }

  public static SimpleItem fromItem(Item item) {
    return LEGACY_ID_MAP.get(item.getLegacyID());
  }

  public static SimpleItem fromLegacyID(int id) {
    return LEGACY_ID_MAP.get(id);
  }

  public static SimpleItem fromModernID(String id) {
    return MODERN_ID_MAP.get(id);
  }
}
