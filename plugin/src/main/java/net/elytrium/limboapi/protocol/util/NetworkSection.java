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

package net.elytrium.limboapi.protocol.util;

import com.velocitypowered.api.network.ProtocolVersion;
import io.netty.buffer.ByteBuf;
import java.util.EnumMap;
import java.util.Map;
import lombok.NonNull;
import net.elytrium.limboapi.api.chunk.VirtualBlock;
import net.elytrium.limboapi.api.chunk.data.BlockSection;
import net.elytrium.limboapi.api.chunk.data.BlockStorage;
import net.elytrium.limboapi.api.mcprotocollib.NibbleArray3d;
import net.elytrium.limboapi.protocol.data.BlockStorage17;
import net.elytrium.limboapi.protocol.data.BlockStorage19;

public class NetworkSection {

  private final Map<ProtocolVersion, BlockStorage> storages = new EnumMap<>(ProtocolVersion.class);
  private final NibbleArray3d blockLight;
  private final NibbleArray3d skyLight;
  private final BlockSection section;
  private int blockCount = -1;

  public NetworkSection(BlockSection section, @NonNull NibbleArray3d blockLight, NibbleArray3d skyLight) {
    this.section = section;
    this.blockLight = blockLight;
    this.skyLight = skyLight;
  }

  public BlockStorage ensureCreated(ProtocolVersion version) {
    BlockStorage storage = this.storages.get(version);
    if (storage == null) {
      synchronized (this.storages) {
        VirtualBlock.Version bVersion = VirtualBlock.Version.map(version);
        BlockStorage blockStorage = this.create(version);
        this.fillBlocks(blockStorage);
        for (ProtocolVersion pVersion : bVersion.getVersions()) {
          this.storages.put(pVersion, blockStorage);
        }
        storage = blockStorage;
      }
    }
    return storage;
  }

  public int getDataLength(ProtocolVersion version) {
    BlockStorage blockStorage = this.ensureCreated(version);

    int dataLength = blockStorage.getDataLength(version);

    if (version.compareTo(ProtocolVersion.MINECRAFT_1_14) < 0) {
      dataLength += this.blockLight.getData().length;
      if (this.skyLight != null) {
        dataLength += this.skyLight.getData().length;
      }
    }
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_14) >= 0) {
      dataLength += 2; //Block count short
    }

    return dataLength;
  }

  public void writeData(ByteBuf data, int pass, ProtocolVersion version) {
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_9) < 0) {
      BlockStorage storage = this.ensureCreated(version);
      if (version.compareTo(ProtocolVersion.MINECRAFT_1_8) < 0) {
        this.write17Data(data, pass, storage);
      } else {
        this.write18Data(data, pass, storage);
      }
    } else if (pass == 0) {
      BlockStorage storage = this.ensureCreated(version);
      if (version.compareTo(ProtocolVersion.MINECRAFT_1_14) < 0) {
        this.write19Data(data, storage, version);
      } else {
        this.write114Data(data, storage, version);
      }
    }
  }

  private void write17Data(ByteBuf data, int pass, BlockStorage storage) {
    if (pass == 0) {
      storage.write(data, ProtocolVersion.MINECRAFT_1_7_2);
    } else if (pass == 1) {
      storage.write(data, ProtocolVersion.MINECRAFT_1_7_2);
    } else if (pass == 2) {
      data.writeBytes(blockLight.getData());
    } else if (pass == 3 && skyLight != null) {
      data.writeBytes(skyLight.getData());
    }
  }

  private void write18Data(ByteBuf data, int pass, BlockStorage storage) {
    if (pass == 0) {
      storage.write(data, ProtocolVersion.MINECRAFT_1_8);
    } else if (pass == 1) {
      data.writeBytes(blockLight.getData());
    } else if (pass == 2 && skyLight != null) {
      data.writeBytes(skyLight.getData());
    }
  }

  private void write19Data(ByteBuf data, BlockStorage storage, ProtocolVersion version) {
    storage.write(data, version);
    data.writeBytes(blockLight.getData());
    if (skyLight != null) {
      data.writeBytes(skyLight.getData());
    }
  }

  private void write114Data(ByteBuf data, BlockStorage storage, ProtocolVersion version) {
    data.writeShort(blockCount);
    storage.write(data, version);
  }

  private void fillBlocks(BlockStorage storage) {
    int blockCount = 0;
    for (int y = 0; y < 16; y++) {
      for (int x = 0; x < 16; x++) {
        for (int z = 0; z < 16; z++) {
          VirtualBlock block = section.getBlockAt(x, y, z);
          if (block.isAir()) {
            continue;
          }
          blockCount++;
          storage.set(x, y, z, block);
        }
      }
    }
    if (this.blockCount == -1) {
      this.blockCount = blockCount;
    }
  }

  private BlockStorage create(ProtocolVersion version) {
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_9) < 0) {
      return new BlockStorage17();
    } else {
      return new BlockStorage19(version);
    }
  }
}
