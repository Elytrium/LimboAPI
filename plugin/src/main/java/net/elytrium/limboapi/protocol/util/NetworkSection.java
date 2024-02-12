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

package net.elytrium.limboapi.protocol.util;

import com.velocitypowered.api.network.ProtocolVersion;
import io.netty.buffer.ByteBuf;
import java.util.EnumMap;
import java.util.Map;
import net.elytrium.limboapi.api.chunk.VirtualBiome;
import net.elytrium.limboapi.api.chunk.VirtualBlock;
import net.elytrium.limboapi.api.chunk.data.BlockSection;
import net.elytrium.limboapi.api.chunk.data.BlockStorage;
import net.elytrium.limboapi.api.mcprotocollib.NibbleArray3D;
import net.elytrium.limboapi.protocol.data.BiomeStorage118;
import net.elytrium.limboapi.protocol.data.BlockStorage17;
import net.elytrium.limboapi.protocol.data.BlockStorage19;
import net.elytrium.limboapi.server.world.chunk.SimpleChunk;

public class NetworkSection {

  private final Map<ProtocolVersion, BlockStorage> storages = new EnumMap<>(ProtocolVersion.class);
  private final Map<ProtocolVersion, BiomeStorage118> biomeStorages = new EnumMap<>(ProtocolVersion.class);
  private final NibbleArray3D blockLight;
  private final NibbleArray3D skyLight;
  private final BlockSection section;
  private final VirtualBiome[] biomes;
  private final int index;

  private int blockCount = -1;

  public NetworkSection(int index, BlockSection section, NibbleArray3D blockLight, NibbleArray3D skyLight, VirtualBiome[] biomes) {
    this.index = index;
    this.section = section;
    this.blockLight = blockLight;
    this.skyLight = skyLight;
    this.biomes = biomes;
  }

  public int getDataLength(ProtocolVersion version) {
    int dataLength = this.ensureStorageCreated(version).getDataLength(version);
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_14) < 0) {
      dataLength += this.blockLight.getData().length;
      if (this.skyLight != null) {
        dataLength += this.skyLight.getData().length;
      }
    }
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_14) >= 0) {
      dataLength += 2; // Block count short.
    }
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_17_1) > 0) {
      dataLength += this.ensure118BiomeCreated(version).getDataLength();
    }

    return dataLength;
  }

  public void writeData(ByteBuf buf, int pass, ProtocolVersion version) {
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_9) < 0) {
      BlockStorage storage = this.ensureStorageCreated(version);
      this.write17Data(buf, storage, version, pass);
    } else if (pass == 0) {
      BlockStorage storage = this.ensureStorageCreated(version);
      if (version.compareTo(ProtocolVersion.MINECRAFT_1_14) < 0) {
        this.write19Data(buf, storage, version, pass);
      } else {
        this.write114Data(buf, storage, version, pass);

        if (version.compareTo(ProtocolVersion.MINECRAFT_1_17_1) > 0) {
          this.write118Biomes(buf, version);
        }
      }
    }
  }

  private BlockStorage ensureStorageCreated(ProtocolVersion version) {
    BlockStorage storage = this.storages.get(version);
    if (storage == null) {
      synchronized (this.storages) {
        BlockStorage blockStorage = this.createStorage(version);
        this.fillBlocks(blockStorage);
        this.storages.put(version, blockStorage);
        storage = blockStorage;
      }
    }

    return storage;
  }

  private BlockStorage createStorage(ProtocolVersion version) {
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_9) < 0) {
      return new BlockStorage17();
    } else {
      return new BlockStorage19(version);
    }
  }

  private void write17Data(ByteBuf buf, BlockStorage storage, ProtocolVersion version, int pass) {
    if (pass == 0 || pass == 1) {
      storage.write(buf, version, pass);
    } else if (pass == 2) {
      buf.writeBytes(this.blockLight.getData());
    } else if (pass == 3 && this.skyLight != null) {
      buf.writeBytes(this.skyLight.getData());
    }
  }

  private void write19Data(ByteBuf buf, BlockStorage storage, ProtocolVersion version, int pass) {
    storage.write(buf, version, pass);
    buf.writeBytes(this.blockLight.getData());
    if (this.skyLight != null) {
      buf.writeBytes(this.skyLight.getData());
    }
  }

  private void write114Data(ByteBuf buf, BlockStorage storage, ProtocolVersion version, int pass) {
    buf.writeShort(this.blockCount);
    storage.write(buf, version, pass);
  }

  private void write118Biomes(ByteBuf buf, ProtocolVersion version) {
    this.ensure118BiomeCreated(version).write(buf, version);
  }

  private BiomeStorage118 ensure118BiomeCreated(ProtocolVersion version) {
    BiomeStorage118 storage = this.biomeStorages.get(version);
    if (storage == null) {
      synchronized (this.biomeStorages) {
        storage = new BiomeStorage118(version);
        int offset = this.index * SimpleChunk.MAX_BIOMES_PER_SECTION;
        for (int biomeIndex = 0, biomeArrayIndex = offset; biomeIndex < SimpleChunk.MAX_BIOMES_PER_SECTION; ++biomeIndex, ++biomeArrayIndex) {
          storage.set(biomeIndex, this.biomes[biomeArrayIndex]);
        }

        this.biomeStorages.put(version, storage);
      }
    }

    return storage;
  }

  private void fillBlocks(BlockStorage storage) {
    int blockCount = 0;
    for (int posX = 0; posX < 16; ++posX) {
      for (int posY = 0; posY < 16; ++posY) {
        for (int posZ = 0; posZ < 16; ++posZ) {
          VirtualBlock block = this.section.getBlockAt(posX, posY, posZ);
          if (!block.isAir()) {
            ++blockCount;
            storage.set(posX, posY, posZ, block);
          }
        }
      }
    }

    if (this.blockCount == -1) {
      this.blockCount = blockCount;
    }
  }
}
