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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import net.elytrium.limboapi.LimboAPI;
import net.elytrium.limboapi.api.material.WorldVersion;
import net.elytrium.limboapi.protocol.packets.s2c.UpdateTagsPacket;

public class SimpleTagManager {

  private static final Map<String, Integer> FLUIDS = new HashMap<>();
  private static final Map<WorldVersion, UpdateTagsPacket> VERSION_MAP = new EnumMap<>(WorldVersion.class);

  @SuppressWarnings("unchecked")
  public static void init() {
    Gson gson = new Gson();
    LinkedTreeMap<String, String> fluids = gson.fromJson(
        new InputStreamReader(
            Objects.requireNonNull(LimboAPI.class.getResourceAsStream("/mapping/fluids.json")),
            StandardCharsets.UTF_8
        ),
        LinkedTreeMap.class
    );

    fluids.forEach((id, protocolId) -> FLUIDS.put(id, Integer.valueOf(protocolId)));

    LinkedTreeMap<String, LinkedTreeMap<String, List<String>>> tags = gson.fromJson(
        new InputStreamReader(
            Objects.requireNonNull(LimboAPI.class.getResourceAsStream("/mapping/tags.json")),
            StandardCharsets.UTF_8
        ),
        LinkedTreeMap.class
    );

    for (WorldVersion version : WorldVersion.values()) {
      VERSION_MAP.put(version, localGetTagsForVersion(tags, version));
    }
  }

  public static UpdateTagsPacket getUpdateTagsPacket(ProtocolVersion version) {
    return VERSION_MAP.get(WorldVersion.from(version));
  }

  public static UpdateTagsPacket getUpdateTagsPacket(WorldVersion version) {
    return VERSION_MAP.get(version);
  }

  private static UpdateTagsPacket localGetTagsForVersion(LinkedTreeMap<String, LinkedTreeMap<String, List<String>>> defaultTags,
                                                         WorldVersion version) {
    Map<String, Map<String, List<Integer>>> tags = new LinkedTreeMap<>();
    defaultTags.forEach((tagType, defaultTagList) -> {
      LinkedTreeMap<String, List<Integer>> tagList = new LinkedTreeMap<>();
      switch (tagType) {
        case "minecraft:block": {
          defaultTagList.forEach((tagName, blockList) ->
              tagList.put(tagName, blockList.stream()
                  .map(e -> SimpleBlock.fromModernID(e, Map.of()))
                  .filter(e -> e.isSupportedOn(version))
                  .map(e -> (int) e.getBlockID(version))
                  .collect(Collectors.toList())));
          break;
        }
        case "minecraft:fluid": {
          defaultTagList.forEach((tagName, fluidList) ->
              tagList.put(tagName, fluidList.stream().map(FLUIDS::get).collect(Collectors.toList())));
          break;
        }
        case "minecraft:item": {
          defaultTagList.forEach((tagName, itemList) ->
              tagList.put(tagName, itemList.stream()
                  .map(SimpleItem::fromModernID)
                  .filter(item -> item.isSupportedOn(version))
                  .map(item -> (int) item.getID(version))
                  .collect(Collectors.toList())));
          break;
        }
        default: {
          defaultTagList.forEach((tagName, entryList) -> {
            if (!entryList.isEmpty()) {
              throw new IllegalStateException("The " + tagType + " tag type is not supported yet.");
            }

            tagList.put(tagName, List.of());
          });
          break;
        }
      }

      tags.put(tagType, tagList);
    });

    return new UpdateTagsPacket(tags);
  }
}
