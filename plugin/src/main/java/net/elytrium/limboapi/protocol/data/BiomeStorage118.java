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

package net.elytrium.limboapi.protocol.data;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.elytrium.limboapi.api.chunk.VirtualBiome;
import net.elytrium.limboapi.api.chunk.util.CompactStorage;
import net.elytrium.limboapi.material.Biome;
import net.elytrium.limboapi.mcprotocollib.BitStorage116;
import net.elytrium.limboapi.server.world.chunk.SimpleChunk;
import org.checkerframework.checker.nullness.qual.NonNull;

public class BiomeStorage118 {

  private final ProtocolVersion version;

  private List<VirtualBiome> palette = new ArrayList<>();
  private Map<Integer, VirtualBiome> rawToBiome = new HashMap<>();
  private CompactStorage storage;

  public BiomeStorage118(ProtocolVersion version) {
    this.version = version;

    for (Biome biome : Biome.values()) {
      this.palette.add(biome);
      this.rawToBiome.put(biome.getID(), biome);
    }

    this.storage = new BitStorage116(3, SimpleChunk.MAX_BIOMES_PER_SECTION);
  }

  private BiomeStorage118(ProtocolVersion version, List<VirtualBiome> palette, Map<Integer, VirtualBiome> rawToBiome, CompactStorage storage) {
    this.version = version;
    this.palette = palette;
    this.rawToBiome = rawToBiome;
    this.storage = storage;
  }

  public void set(int posX, int posY, int posZ, @NonNull VirtualBiome biome) {
    int id = this.getIndex(biome);
    this.storage.set(index(posX, posY, posZ), id);
  }

  public void set(int index, @NonNull VirtualBiome biome) {
    int id = this.getIndex(biome);
    this.storage.set(index, id);
  }

  @NonNull
  public VirtualBiome get(int posX, int posY, int posZ) {
    return this.get(index(posX, posY, posZ));
  }

  private VirtualBiome get(int index) {
    int id = this.storage.get(index);
    if (this.storage.getBitsPerEntry() > 8) {
      return this.rawToBiome.get(id);
    } else {
      return this.palette.get(id);
    }
  }

  public void write(ByteBuf buf, ProtocolVersion version) {
    buf.writeByte(this.storage.getBitsPerEntry());
    if (this.storage.getBitsPerEntry() <= 8) {
      ProtocolUtils.writeVarInt(buf, this.palette.size());
      for (VirtualBiome biome : this.palette) {
        ProtocolUtils.writeVarInt(buf, biome.getID());
      }
    }

    this.storage.write(buf, version);
  }

  public int getDataLength() {
    int length = 1;
    if (this.storage.getBitsPerEntry() <= 8) {
      length += ProtocolUtils.varIntBytes(this.palette.size());
      for (VirtualBiome biome : this.palette) {
        length += ProtocolUtils.varIntBytes(biome.getID());
      }
    }

    return length + this.storage.getDataLength();
  }

  public BiomeStorage118 copy() {
    return new BiomeStorage118(this.version, new ArrayList<>(this.palette), new HashMap<>(this.rawToBiome), this.storage.copy());
  }

  private int getIndex(VirtualBiome biome) {
    if (this.storage.getBitsPerEntry() > 8) {
      int raw = biome.getID();
      this.rawToBiome.put(raw, biome);
      return raw;
    } else {
      int id = this.palette.indexOf(biome);
      if (id == -1) {
        if (this.palette.size() >= (1 << this.storage.getBitsPerEntry())) {
          this.resize(this.storage.getBitsPerEntry() + 1);
          return this.getIndex(biome);
        }

        this.palette.add(biome);
        id = this.palette.size() - 1;
      }

      return id;
    }
  }

  private void resize(int newSize) {
    newSize = StorageUtils.fixBitsPerEntry(this.version, newSize);
    CompactStorage newStorage = new BitStorage116(newSize, SimpleChunk.MAX_BIOMES_PER_SECTION);
    for (int i = 0; i < SimpleChunk.MAX_BIOMES_PER_SECTION; ++i) {
      newStorage.set(i, newSize > 8 ? this.palette.get(this.storage.get(i)).getID() : this.storage.get(i));
    }

    this.storage = newStorage;
  }

  @Override
  public String toString() {
    return "BiomeStorage118{"
        + "version=" + this.version
        + ", palette=" + this.palette
        + ", rawToBiome=" + this.rawToBiome
        + ", storage=" + this.storage
        + "}";
  }

  private static int index(int posX, int posY, int posZ) {
    return posY << 4 | posZ << 2 | posX;
  }
}
