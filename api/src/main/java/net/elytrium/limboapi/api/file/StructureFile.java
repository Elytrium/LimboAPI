/*
 * Copyright (C) 2021 - 2022 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.file;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import net.elytrium.limboapi.api.LimboFactory;
import net.elytrium.limboapi.api.chunk.VirtualBlock;
import net.elytrium.limboapi.api.chunk.VirtualWorld;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;

public class StructureFile implements WorldFile {

  // private int width;
  // private int height;
  // private int length;
  private ListBinaryTag blocks;
  private ListBinaryTag palette;

  public StructureFile(Path file) throws IOException {
    CompoundBinaryTag tag = BinaryTagIO.unlimitedReader().read(file, BinaryTagIO.Compression.GZIP);
    this.fromNBT(tag);
  }

  public StructureFile(InputStream stream) throws IOException {
    CompoundBinaryTag tag = BinaryTagIO.unlimitedReader().read(stream, BinaryTagIO.Compression.GZIP);
    this.fromNBT(tag);
  }

  public StructureFile(CompoundBinaryTag tag) {
    this.fromNBT(tag);
  }

  @Override
  public void toWorld(LimboFactory factory, VirtualWorld world, int offsetX, int offsetY, int offsetZ) {
    this.toWorld(factory, world, offsetX, offsetY, offsetZ, 15);
  }

  @Override
  public void toWorld(LimboFactory factory, VirtualWorld world, int offsetX, int offsetY, int offsetZ, int lightLevel) {
    VirtualBlock[] palettedBlocks = new VirtualBlock[this.palette.size()];
    for (int i = 0; i < this.palette.size(); i++) {
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
      int x = posTag.getInt(0);
      int y = posTag.getInt(1);
      int z = posTag.getInt(2);

      int state = blockMap.getInt("state");
      VirtualBlock block = palettedBlocks[state];
      world.setBlock(offsetX + x, offsetY + y, offsetZ + z, block);
    }

    world.fillSkyLight(lightLevel);
  }

  private void fromNBT(CompoundBinaryTag tag) {
    // ListBinaryTag size = tag.getList("size");
    // this.width = size.getInt(0);
    // this.height = size.getInt(1);
    // this.length = size.getInt(2);

    this.blocks = tag.getList("blocks");
    this.palette = tag.getList("palette");
  }
}
