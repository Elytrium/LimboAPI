/*
 * Copyright (C) 2021 - 2023 Elytrium
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

@Deprecated(forRemoval = true)
public class StructureFile implements WorldFile {

  // private int width;
  // private int height;
  // private int length;
  private ListBinaryTag blocks;
  private ListBinaryTag palette;

  /**
   * @deprecated See {@link LimboFactory#openWorldFile(BuiltInWorldFileType, Path)}
   */
  public StructureFile(Path file) throws IOException {
    CompoundBinaryTag tag = BinaryTagIO.unlimitedReader().read(file, BinaryTagIO.Compression.GZIP);
    this.fromNBT(tag);
  }

  /**
   * @deprecated See {@link LimboFactory#openWorldFile(BuiltInWorldFileType, InputStream)}
   */
  public StructureFile(InputStream stream) throws IOException {
    CompoundBinaryTag tag = BinaryTagIO.unlimitedReader().read(stream, BinaryTagIO.Compression.GZIP);
    this.fromNBT(tag);
  }

  /**
   * @deprecated See {@link LimboFactory#openWorldFile(BuiltInWorldFileType, CompoundBinaryTag)}
   */
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
      world.setBlock(offsetX + posTag.getInt(0), offsetY + posTag.getInt(1), offsetZ + posTag.getInt(2), block);
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
