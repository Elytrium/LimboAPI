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
import net.elytrium.limboapi.api.LimboFactory;
import net.elytrium.limboapi.api.chunk.VirtualWorld;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.CompoundBinaryTag;

public class SchematicFile implements WorldFile {

  private short width;
  private short height;
  private short length;
  private byte[] blocks;
  private byte[] addBlocks = new byte[0];
  private byte[] blocksData;

  public SchematicFile(Path file) throws IOException {
    CompoundBinaryTag tag = BinaryTagIO.unlimitedReader().read(file, BinaryTagIO.Compression.GZIP);
    this.fromNBT(tag);
  }

  public SchematicFile(InputStream stream) throws IOException {
    CompoundBinaryTag tag = BinaryTagIO.unlimitedReader().read(stream, BinaryTagIO.Compression.GZIP);
    this.fromNBT(tag);
  }

  public SchematicFile(CompoundBinaryTag tag) {
    this.fromNBT(tag);
  }

  @Override
  public void toWorld(LimboFactory factory, VirtualWorld world, int offsetX, int offsetY, int offsetZ) {
    short[] blockIds = new short[this.blocks.length];

    for (int index = 0; index < this.blocks.length; ++index) {
      if ((index >> 1) >= this.addBlocks.length) {
        blockIds[index] = (short) (this.blocks[index] & 0xFF);
      } else {
        if ((index & 1) == 0) {
          blockIds[index] = (short) (((this.addBlocks[index >> 1] & 0x0F) << 8) + (this.addBlocks[index] & 0xFF));
        } else {
          blockIds[index] = (short) (((this.addBlocks[index >> 1] & 0xF0) << 4) + (this.addBlocks[index] & 0xFF));
        }
      }
    }

    for (int x = 0; x < this.width; ++x) {
      for (int y = 0; y < this.height; ++y) {
        for (int z = 0; z < this.length; ++z) {
          int index = (y * this.length + z) * this.width + x;
          world.setBlock(x + offsetX, y + offsetY, z + offsetZ, factory.createSimpleBlock(blockIds[index], this.blocksData[index]));
        }
      }
    }
  }

  private void fromNBT(CompoundBinaryTag tag) {
    this.width = tag.getShort("Width");
    this.height = tag.getShort("Height");
    this.length = tag.getShort("Length");
    this.blocks = tag.getByteArray("Blocks");
    this.blocksData = tag.getByteArray("Data");

    if (tag.keySet().contains("AddBlocks")) {
      this.addBlocks = tag.getByteArray("AddBlocks");
    }
  }
}
