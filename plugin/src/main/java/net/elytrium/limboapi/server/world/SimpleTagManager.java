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
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.elytrium.limboapi.LimboAPI;
import net.elytrium.limboapi.api.world.WorldVersion;
import net.elytrium.limboapi.protocol.packets.s2c.UpdateTagsPacket;
import net.elytrium.limboapi.utils.JsonUtil;

public class SimpleTagManager {

  private static final Object2IntOpenHashMap<String> FLUIDS;
  private static final Map<WorldVersion, UpdateTagsPacket> VERSION_MAP;

  static {
    var fluids = JsonUtil.<Number>parse(LimboAPI.class.getResourceAsStream("/mappings/fluids.json"));
    FLUIDS = new Object2IntOpenHashMap<>(fluids.size());
    fluids.forEach((id, protocolId) -> SimpleTagManager.FLUIDS.put(id, protocolId.intValue()));

    var tags = JsonUtil.<LinkedTreeMap<String, List<String>>>parse(LimboAPI.class.getResourceAsStream("/mappings/tags.json"));
    VERSION_MAP = new EnumMap<>(WorldVersion.class);
    for (WorldVersion version : WorldVersion.values()) {
      SimpleTagManager.VERSION_MAP.put(version, SimpleTagManager.createPacket(tags, version));
    }
  }

  public static UpdateTagsPacket getUpdateTagsPacket(WorldVersion version) {
    return SimpleTagManager.VERSION_MAP.get(version);
  }

  public static UpdateTagsPacket getUpdateTagsPacket(ProtocolVersion version) {
    return SimpleTagManager.VERSION_MAP.get(WorldVersion.from(version));
  }

  private static UpdateTagsPacket createPacket(LinkedTreeMap<String, LinkedTreeMap<String, List<String>>> defaultTags, WorldVersion version) {
    Object2ObjectOpenHashMap<String, Map<String, int[]>> tags = new Object2ObjectOpenHashMap<>();
    defaultTags.forEach((tagType, defaultTagList) -> {
      Object2ObjectOpenHashMap<String, int[]> tagList = new Object2ObjectOpenHashMap<>();
      switch (tagType) {
        case "minecraft:block": {
          defaultTagList.forEach((tagName, blockList) -> tagList.put(tagName, blockList.stream()
              .map(modernId -> SimpleBlock.fromModernId(modernId, Collections.emptyMap()))
              .filter(block -> block.isSupportedOn(version))
              .mapToInt(block -> block.blockId(version))
              .toArray()
          ));
          break;
        }
        case "minecraft:fluid": {
          defaultTagList.forEach((tagName, fluidList) -> tagList.put(tagName, fluidList.stream().mapToInt(SimpleTagManager.FLUIDS::getInt).toArray()));
          break;
        }
        case "minecraft:item": {
          defaultTagList.forEach((tagName, itemList) -> tagList.put(tagName, itemList.stream()
              .map(SimpleItem::fromModernId)
              .filter(item -> item.isSupportedOn(version))
              .mapToInt(item -> item.itemId(version))
              .toArray()
          ));
          break;
        }
        default: {
          defaultTagList.forEach((tagName, entryList) -> {
            if (!entryList.isEmpty()) {
              throw new IllegalStateException("The " + tagType + " tag type is not supported yet.");
            }

            tagList.put(tagName, new int[0]);
          });
          break;
        }
      }

      tags.put(tagType, tagList);
    });
    return new UpdateTagsPacket(tags);
  }
}
