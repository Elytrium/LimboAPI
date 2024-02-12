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

package net.elytrium.limboapi.protocol.packets.s2c;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class UpdateTagsPacket implements MinecraftPacket {

  private final Map<String, Map<String, List<Integer>>> tags;

  public UpdateTagsPacket() {
    throw new IllegalStateException();
  }

  public UpdateTagsPacket(Map<String, Map<String, List<Integer>>> tags) {
    this.tags = tags;
  }

  public Map<String, Map<String, int[]>> toVelocityTags() {
    Map<String, Map<String, int[]>> newTags = new HashMap<>();
    for (Entry<String, Map<String, List<Integer>>> entry : this.tags.entrySet()) {
      Map<String, int[]> tagRegistry = new HashMap<>();

      for (Entry<String, List<Integer>> tagEntry : entry.getValue().entrySet()) {
        tagRegistry.put(tagEntry.getKey(),
            tagEntry.getValue().stream().mapToInt(Integer::intValue).toArray());
      }

      newTags.put(entry.getKey(), tagRegistry);
    }

    return newTags;
  }

  @Override
  public void decode(ByteBuf byteBuf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    throw new IllegalStateException();
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_17) >= 0) {
      ProtocolUtils.writeVarInt(buf, this.tags.size());
      this.tags.forEach((tagType, tagList) -> {
        ProtocolUtils.writeString(buf, tagType);
        writeTagList(buf, tagList);
      });
    } else {
      writeTagList(buf, this.tags.get("minecraft:block"));
      writeTagList(buf, this.tags.get("minecraft:item"));
      writeTagList(buf, this.tags.get("minecraft:fluid"));
      if (version.compareTo(ProtocolVersion.MINECRAFT_1_14) >= 0) {
        writeTagList(buf, this.tags.get("minecraft:entity_type"));
      }
    }
  }

  private static void writeTagList(ByteBuf buf, Map<String, List<Integer>> tagList) {
    ProtocolUtils.writeVarInt(buf, tagList.size());
    tagList.forEach((tagId, blockList) -> {
      ProtocolUtils.writeString(buf, tagId);
      ProtocolUtils.writeVarInt(buf, blockList.size());
      blockList.forEach(blockId -> ProtocolUtils.writeVarInt(buf, blockId));
    });
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return true;
  }
}
