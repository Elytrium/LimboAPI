/*
 * Copyright (C) 2021 Elytrium
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
    CompoundBinaryTag tag = BinaryTagIO.reader().read(file, BinaryTagIO.Compression.GZIP);
    fromNBT(tag);
  }

  public SchematicFile(InputStream stream) throws IOException {
    CompoundBinaryTag tag = BinaryTagIO.reader().read(stream, BinaryTagIO.Compression.GZIP);
    fromNBT(tag);
  }

  public SchematicFile(CompoundBinaryTag tag) {
    fromNBT(tag);
  }

  @Override
  public void toWorld(LimboFactory factory, VirtualWorld world, int offsetX, int offsetY, int offsetZ) {
    short[] blockIds = new short[blocks.length];

    for (int index = 0; index < blocks.length; index++) {
      if ((index >> 1) >= addBlocks.length) {
        blockIds[index] = (short) (blocks[index] & 0xFF);
      } else {
        if ((index & 1) == 0) {
          blockIds[index] = (short) (((addBlocks[index >> 1] & 0x0F) << 8) + (addBlocks[index] & 0xFF));
        } else {
          blockIds[index] = (short) (((addBlocks[index >> 1] & 0xF0) << 4) + (addBlocks[index] & 0xFF));
        }
      }
    }

    for (int x = 0; x < width; ++x) {
      for (int y = 0; y < height; ++y) {
        for (int z = 0; z < length; ++z) {
          int index = (y * length + z) * width + x;
          world.setBlock(x + offsetX, y + offsetY, z + offsetZ,
              factory.createSimpleBlock(blockIds[index], blocksData[index]));
        }
      }
    }
  }

  private void fromNBT(CompoundBinaryTag tag) {
    width = tag.getShort("Width");
    height = tag.getShort("Height");
    length = tag.getShort("Length");
    blocks = tag.getByteArray("Blocks");
    blocksData = tag.getByteArray("Data");

    if (tag.keySet().contains("AddBlocks")) {
      addBlocks = tag.getByteArray("AddBlocks");
    }
  }
}
