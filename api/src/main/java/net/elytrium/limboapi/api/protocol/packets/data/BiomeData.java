/*
 * Copyright (C) 2021 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.protocol.packets.data;

import java.util.HashMap;
import java.util.Map;
import net.elytrium.limboapi.api.chunk.VirtualBiome;
import net.elytrium.limboapi.api.chunk.data.ChunkSnapshot;

/**
 * For ChunkData packet.
 */
public class BiomeData {

  private final byte[] pre115Biomes = new byte[256];
  private final int[] post115Biomes = new int[1024];

  public BiomeData(ChunkSnapshot chunk) {
    VirtualBiome[] biomes = chunk.getBiomes();
    for (int i = 0; i < biomes.length; ++i) {
      this.post115Biomes[i] = biomes[i].getId();
    }

    // Down sample 4x4x4 3d biomes to 2d XZ.
    Map<Integer, Integer> samples = new HashMap<>(64);
    for (int x = 0; x < 16; x += 4) {
      for (int z = 0; z < 16; z += 4) {
        samples.clear();
        for (int y = 0; y < 256; y += 16) {
          VirtualBiome biome = biomes[/*SimpleChunk.getBiomeIndex(x, y, z)*/((y >> 2) & 63) << 4 | ((z >> 2) & 3) << 2 | ((x >> 2) & 3)];
          int curr = samples.getOrDefault(biome.getId(), 0);
          samples.put(biome.getId(), curr + 1);
        }
        int id = samples.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .orElseThrow(RuntimeException::new)
            .getKey();
        for (int xl = x; xl < x + 4; ++xl) {
          for (int zl = z; zl < z + 4; ++zl) {
            this.pre115Biomes[(zl << 4) + xl] = (byte) id;
          }
        }
      }
    }
  }

  public byte[] getPre115Biomes() {
    return this.pre115Biomes;
  }

  public int[] getPost115Biomes() {
    return this.post115Biomes;
  }
}
