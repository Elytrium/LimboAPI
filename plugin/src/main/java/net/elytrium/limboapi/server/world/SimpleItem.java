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
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
import java.util.EnumMap;
import net.elytrium.limboapi.LimboAPI;
import net.elytrium.limboapi.api.world.item.Item;
import net.elytrium.limboapi.api.world.item.VirtualItem;
import net.elytrium.limboapi.api.world.WorldVersion;
import net.elytrium.limboapi.utils.JsonUtil;

public class SimpleItem implements VirtualItem {

  private static final EnumMap<WorldVersion, Short2ObjectOpenHashMap<SimpleItem>> VERSION_2_ID_MAP = new EnumMap<>(WorldVersion.class);
  private static final Object2ObjectOpenHashMap<String, SimpleItem> MODERN_ID_MAP;
  private static final Short2ObjectOpenHashMap<SimpleItem> LEGACY_ID_MAP;

  private final EnumMap<WorldVersion, Short> versionIds = new EnumMap<>(WorldVersion.class);
  private final String modernId;

  public SimpleItem(String modernId) {
    this.modernId = modernId;
  }

  public String modernId() {
    return this.modernId;
  }

  @Override
  public short itemId(WorldVersion version) {
    Short result = this.versionIds.get(version);
    if (result == null) {
      throw new IllegalArgumentException("Item " + this.modernId + " does not exists on " + version);
    }

    return result;
  }

  @Override
  public short itemId(ProtocolVersion version) {
    return this.itemId(WorldVersion.from(version));
  }

  @Override
  public boolean isSupportedOn(WorldVersion version) {
    return this.versionIds.containsKey(version);
  }

  @Override
  public boolean isSupportedOn(ProtocolVersion version) {
    return this.isSupportedOn(WorldVersion.from(version));
  }

  static {
    var itemsMapping = JsonUtil.<LinkedTreeMap<String, Number>>parse(LimboAPI.class.getResourceAsStream("/mappings/items_mappings.json"));
    var modernIdRemap = JsonUtil.<String>parse(LimboAPI.class.getResourceAsStream("/mappings/modern_item_id_remap.json"));
    MODERN_ID_MAP = new Object2ObjectOpenHashMap<>(itemsMapping.size() + modernIdRemap.size());
    String maxProtocol = Integer.toString(ProtocolVersion.MAXIMUM_VERSION.getProtocol());
    itemsMapping.forEach((modernId, versions) -> {
      SimpleItem simpleItem = new SimpleItem(modernId);
      versions.forEach((version, item) -> {
        WorldVersion worldVersion = WorldVersion.from(ProtocolVersion.getProtocolVersion(Integer.parseInt(version)));
        short id = item.shortValue();
        simpleItem.versionIds.put(worldVersion, id);
        SimpleItem.VERSION_2_ID_MAP.computeIfAbsent(worldVersion, key -> new Short2ObjectOpenHashMap<>(itemsMapping.size())).put(id, simpleItem);
      });
      if (versions.containsKey(maxProtocol)) {
        SimpleItem.MODERN_ID_MAP.put(modernId, simpleItem);

        String remapped = modernIdRemap.get(modernId);
        if (remapped != null) {
          if (SimpleItem.MODERN_ID_MAP.containsKey(remapped)) {
            throw new IllegalStateException("Remapped id " + remapped + " (from " + modernId + ") already exists");
          }

          SimpleItem.MODERN_ID_MAP.put(remapped, simpleItem);
        }
      }
    });
    SimpleItem.MODERN_ID_MAP.trim();
    SimpleItem.VERSION_2_ID_MAP.values().forEach(Short2ObjectOpenHashMap::trim);

    var legacyItems = JsonUtil.<String>parse(LimboAPI.class.getResourceAsStream("/mappings/legacy_items.json"));
    LEGACY_ID_MAP = new Short2ObjectOpenHashMap<>(legacyItems.size());
    legacyItems.forEach((legacyProtocolId, modernId) -> {
      short id = Short.parseShort(legacyProtocolId);
      SimpleItem value = SimpleItem.MODERN_ID_MAP.get(modernId);
      SimpleItem.LEGACY_ID_MAP.put(id, value);
      if (value != null) {
        value.versionIds.put(WorldVersion.LEGACY, id);
      }
    });
  }

  public static SimpleItem fromModernId(String id) {
    return SimpleItem.MODERN_ID_MAP.get(id);
  }

  public static VirtualItem fromId(ProtocolVersion version, short id) {
    return SimpleItem.fromId(WorldVersion.from(version), id);
  }

  public static VirtualItem fromId(WorldVersion version, short id) {
    return SimpleItem.VERSION_2_ID_MAP.get(version).get(id);
  }

  public static SimpleItem fromItem(Item item) {
    return SimpleItem.fromLegacyId(item.getLegacyId());
  }

  public static SimpleItem fromLegacyId(short id) {
    return SimpleItem.LEGACY_ID_MAP.get(id);
  }
}
