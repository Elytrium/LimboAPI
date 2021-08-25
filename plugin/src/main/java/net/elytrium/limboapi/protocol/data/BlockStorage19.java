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
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import net.elytrium.limboapi.api.chunk.VirtualBlock;
import net.elytrium.limboapi.api.chunk.data.BlockStorage;
import net.elytrium.limboapi.mcprotocollib.BitStorage116;
import net.elytrium.limboapi.mcprotocollib.BitStorage19;
import net.elytrium.limboapi.api.chunk.util.CompactStorage;
import net.elytrium.limboapi.server.world.SimpleBlock;
import net.elytrium.limboapi.server.world.chunk.SimpleChunk;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BlockStorage19 implements BlockStorage {

  private final ProtocolVersion version;
  private List<VirtualBlock> palette = new ArrayList<>();
  private Map<Integer, VirtualBlock> rawToBlock = new HashMap<>();
  private CompactStorage storage;

  public BlockStorage19(ProtocolVersion version) {
    this.version = version;
    this.storage = createStorage(4);
    palette.add(SimpleBlock.AIR);
    rawToBlock.put(toRaw(SimpleBlock.AIR, version), SimpleBlock.AIR);
  }

  public void set(int x, int y, int z, @NonNull VirtualBlock block) {
    int id = getIndex(block);
    storage.set(index(x, y, z), id);
  }

  @SuppressFBWarnings("NP_NONNULL_RETURN_VIOLATION")
  public @NotNull VirtualBlock get(int x, int y, int z) {
    return get(index(x, y, z));
  }

  private VirtualBlock get(int index) {
    int id = storage.get(index);

    if (storage.getBitsPerEntry() > 8) {
      return rawToBlock.get(id);
    } else {
      return palette.get(id);
    }
  }

  @Override
  public void write(ByteBuf buf, ProtocolVersion version) {
    buf.writeByte(storage.getBitsPerEntry());
    if (storage.getBitsPerEntry() > 8) {
      if (this.version.compareTo(ProtocolVersion.MINECRAFT_1_13) < 0) {
        ProtocolUtils.writeVarInt(buf, 0);
      }
    } else {
      ProtocolUtils.writeVarInt(buf, palette.size());
      for (VirtualBlock state : palette) {
        ProtocolUtils.writeVarInt(buf, toRaw(state, this.version));
      }
    }
    storage.write(buf, version);
  }

  @Override
  public int getDataLength(ProtocolVersion version) {
    int length = 1;
    if (storage.getBitsPerEntry() > 8) {
      if (this.version.compareTo(ProtocolVersion.MINECRAFT_1_13) < 0) {
        length += 1;
      }
    } else {
      length += ProtocolUtils.varIntBytes(palette.size());
      for (VirtualBlock state : palette) {
        length += ProtocolUtils.varIntBytes(toRaw(state, this.version));
      }
    }
    return length + storage.getDataLength();
  }


  private int getIndex(VirtualBlock block) {
    if (storage.getBitsPerEntry() > 8) {
      int raw = toRaw(block, version);
      rawToBlock.put(raw, block);
      return raw;
    }
    int id = palette.indexOf(block);
    if (id == -1) {
      if (palette.size() >= (1 << storage.getBitsPerEntry())) {
        resize(storage.getBitsPerEntry() + 1);
        return getIndex(block);
      }
      palette.add(block);
      id = palette.size() - 1;
    }
    return id;
  }

  @Override
  public BlockStorage copy() {
    return new BlockStorage19(version, new ArrayList<>(palette), new HashMap<>(rawToBlock),
        storage.copy());
  }

  private void resize(int newSize) {
    newSize = fixBitsPerEntry(newSize);
    CompactStorage newStorage = createStorage(newSize);

    for (int i = 0; i < SimpleChunk.MAX_BLOCKS_PER_SECTION; i++) {
      int newId = newSize > 8 ? toRaw(palette.get(storage.get(i)), version) : storage.get(i);
      newStorage.set(i, newId);
    }
    this.storage = newStorage;
  }

  private int fixBitsPerEntry(int newSize) {
    if (newSize < 4) {
      return 4;
    } else if (newSize < 9) {
      return newSize;
    } else if (version.compareTo(ProtocolVersion.MINECRAFT_1_13) < 0) {
      return 13;
    } else if (version.compareTo(ProtocolVersion.MINECRAFT_1_16_4) < 0) {
      return 14;
    } else {
      return 15;
    }
  }

  private CompactStorage createStorage(int bits) {
    return version.compareTo(ProtocolVersion.MINECRAFT_1_16) < 0
        ? new BitStorage19(bits, SimpleChunk.MAX_BLOCKS_PER_SECTION)
        : new BitStorage116(bits, SimpleChunk.MAX_BLOCKS_PER_SECTION);
  }

  @Override
  public String toString() {
    return "BlockStorage19{"
        + "version=" + version
        + ", palette=" + palette
        + ", rawToBlock=" + rawToBlock
        + ", storage=" + storage
        + '}';
  }

  private static int toRaw(VirtualBlock state, ProtocolVersion version) {
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_13) < 0) {
      return (state.getId(version) << 4) | (state.getData(version) & 0xF);
    } else {
      return state.getId(version);
    }
  }

  private static int index(int x, int y, int z) {
    return y << 8 | z << 4 | x;
  }
}
