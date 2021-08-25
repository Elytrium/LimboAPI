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

package net.elytrium.limboapi.protocol.data;

import com.velocitypowered.api.network.ProtocolVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.elytrium.limboapi.api.chunk.VirtualBlock;
import net.elytrium.limboapi.api.chunk.data.BlockStorage;
import net.elytrium.limboapi.api.mcprotocollib.NibbleArray3d;
import net.elytrium.limboapi.server.world.SimpleBlock;
import net.elytrium.limboapi.server.world.chunk.SimpleChunk;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class BlockStorage17 implements BlockStorage {

  private final VirtualBlock[] blocks;
  private int pass = 0;

  public BlockStorage17() {
    this(new VirtualBlock[SimpleChunk.MAX_BLOCKS_PER_SECTION]);
  }

  @Override
  public void set(int x, int y, int z, @NonNull VirtualBlock block) {
    blocks[BlockStorage.index(x, y, z)] = block;
  }

  @SuppressFBWarnings("NP_NONNULL_RETURN_VIOLATION")
  @Override
  public @NotNull VirtualBlock get(int x, int y, int z) {
    VirtualBlock block = blocks[BlockStorage.index(x, y, z)];
    return block == null ? SimpleBlock.AIR : block;
  }

  @Override
  public void write(ByteBuf buf, ProtocolVersion version) {
    if (pass == 0) {
      if (version.compareTo(ProtocolVersion.MINECRAFT_1_8) < 0) {
        writeBlocks17(buf);
      } else {
        writeBlocks18(buf);
      }
      pass++;
    } else if (pass == 1) {
      NibbleArray3d metadata = new NibbleArray3d(16 * 16 * 16);
      for (int i = 0; i < blocks.length; i++) {
        metadata.set(i, blocks[i] == null ? 0 : blocks[i].getData(ProtocolVersion.MINECRAFT_1_7_2));
      }
      buf.writeBytes(metadata.getData());
      pass = 0;
    }
  }

  @Override
  public BlockStorage copy() {
    return new BlockStorage17(Arrays.copyOf(blocks, blocks.length));
  }

  private void writeBlocks17(ByteBuf buf) {
    byte[] raw = new byte[blocks.length];
    for (int i = 0; i < blocks.length; i++) {
      VirtualBlock block = blocks[i];
      raw[i] = (byte) (block == null ? 0 : block.getId(ProtocolVersion.MINECRAFT_1_7_2));
    }
    buf.writeBytes(raw);
  }

  private void writeBlocks18(ByteBuf buf) {
    short[] raw = new short[blocks.length];
    for (int i = 0; i < blocks.length; i++) {
      VirtualBlock block = blocks[i];
      raw[i] = (short) (block == null ? 0
          : (block.getId(ProtocolVersion.MINECRAFT_1_8) << 4 | block.getData(ProtocolVersion.MINECRAFT_1_8)));
    }
    for (Short s : raw) {
      buf.writeShortLE(s);
    }
  }

  @Override
  public int getDataLength(ProtocolVersion version) {
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_8) < 0) {
      return blocks.length + (SimpleChunk.MAX_BLOCKS_PER_SECTION >> 1);
    } else {
      return blocks.length * 2;
    }
  }
}
