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

package net.elytrium.limboapi.file;

import java.util.HashMap;
import java.util.Map;
import net.elytrium.limboapi.api.LimboFactory;
import net.elytrium.limboapi.api.chunk.VirtualBlock;
import net.elytrium.limboapi.api.chunk.VirtualWorld;
import net.elytrium.limboapi.api.file.WorldFile;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;

public class StructureNbtFile implements WorldFile {

  private final ListBinaryTag blocks;
  private final ListBinaryTag palette;

  public StructureNbtFile(CompoundBinaryTag tag) {
    this.blocks = tag.getList("blocks");
    this.palette = tag.getList("palette");
  }

  @Override
  public void toWorld(LimboFactory factory, VirtualWorld world, int offsetX, int offsetY, int offsetZ, int lightLevel) {
    VirtualBlock[] palettedBlocks = new VirtualBlock[this.palette.size()];
    for (int i = 0; i < this.palette.size(); ++i) {
      CompoundBinaryTag map = this.palette.getCompound(i);

      Map<String, String> propertiesMap = null;
      if (map.keySet().contains("Properties")) {
        propertiesMap = new HashMap<>();
        CompoundBinaryTag properties = map.getCompound("Properties");
        for (String entry : properties.keySet()) {
          propertiesMap.put(entry, properties.getString(entry));
        }
      }

      palettedBlocks[i] = factory.createSimpleBlock(map.getString("Name"), propertiesMap);
    }

    for (BinaryTag binaryTag : this.blocks) {
      CompoundBinaryTag blockMap = (CompoundBinaryTag) binaryTag;
      ListBinaryTag posTag = blockMap.getList("pos");
      VirtualBlock block = palettedBlocks[blockMap.getInt("state")];
      int x = offsetX + posTag.getInt(0);
      int y = offsetY + posTag.getInt(1);
      int z = offsetZ + posTag.getInt(2);
      world.setBlock(x, y, z, block);

      CompoundBinaryTag blockEntityNbt = blockMap.getCompound("nbt");
      if (!blockEntityNbt.keySet().isEmpty()) {
        world.setBlockEntity(x, y, z, blockEntityNbt, factory.getBlockEntity(block.getModernStringID()));
      }
    }

    world.fillSkyLight(lightLevel);
  }
}
