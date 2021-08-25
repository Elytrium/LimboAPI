/*
 * Copyright (C) 2021 Elytrium
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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import lombok.Getter;
import net.elytrium.limboapi.server.world.SimpleBlock;
import net.elytrium.limboapi.server.world.SimpleWorld;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.CompoundBinaryTag;

@Getter
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
  public void toWorld(SimpleWorld world, int offsetX, int offsetY, int offsetZ) {
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
              SimpleBlock.fromLegacyId(blockIds[index])
                  .setData(blocksData[index]));
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
