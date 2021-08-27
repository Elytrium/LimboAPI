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

package net.elytrium.limboapi.server.world.chunk;

import com.google.common.base.Preconditions;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.elytrium.limboapi.api.chunk.data.LightSection;
import net.elytrium.limboapi.api.mcprotocollib.NibbleArray3d;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class SimpleLightSection implements LightSection {

  public static NibbleArray3d NO_LIGHT = new NibbleArray3d(SimpleChunk.MAX_BLOCKS_PER_SECTION);
  public static NibbleArray3d ALL_LIGHT = new NibbleArray3d(SimpleChunk.MAX_BLOCKS_PER_SECTION, 15);
  public static final LightSection DEFAULT = new SimpleLightSection();

  @Getter
  private NibbleArray3d blockLight = NO_LIGHT;
  @Getter
  private NibbleArray3d skyLight = ALL_LIGHT;
  @Getter
  private long lastUpdate = System.nanoTime();

  public byte getBlockLight(int x, int y, int z) {
    this.checkIndexes(x, y, z);
    return (byte) this.blockLight.get(x, y, z);
  }

  public byte getSkyLight(int x, int y, int z) {
    this.checkIndexes(x, y, z);
    return (byte) this.skyLight.get(x, y, z);
  }

  public void setBlockLight(int x, int y, int z, byte light) {
    this.checkIndexes(x, y, z);
    Preconditions.checkArgument(light >= 0 && light <= 15, "light should be between 0 and 15");
    if (this.blockLight == NO_LIGHT && light != 0) {
      this.blockLight = new NibbleArray3d(SimpleChunk.MAX_BLOCKS_PER_SECTION);
    }
    this.blockLight.set(x, y, z, light);
    this.lastUpdate = System.nanoTime();
  }

  public void setSkyLight(int x, int y, int z, byte light) {
    this.checkIndexes(x, y, z);
    Preconditions.checkArgument(light >= 0 && light <= 15, "light should be between 0 and 15");
    if (this.skyLight == ALL_LIGHT && light != 15) {
      this.skyLight = new NibbleArray3d(SimpleChunk.MAX_BLOCKS_PER_SECTION);
    }
    this.skyLight.set(x, y, z, light);
    this.lastUpdate = System.nanoTime();
  }

  private void checkIndexes(int x, int y, int z) {
    Preconditions.checkArgument(this.checkIndex(x), "x should be between 0 and 15");
    Preconditions.checkArgument(this.checkIndex(y), "y should be between 0 and 15");
    Preconditions.checkArgument(this.checkIndex(z), "z should be between 0 and 15");
  }

  private boolean checkIndex(int i) {
    return i >= 0 && i <= 15;
  }

  public SimpleLightSection copy() {
    NibbleArray3d skyLight = this.skyLight == ALL_LIGHT ? ALL_LIGHT : this.skyLight.copy();
    NibbleArray3d blockLight = this.blockLight == NO_LIGHT ? NO_LIGHT : this.blockLight.copy();
    return new SimpleLightSection(blockLight, skyLight, this.lastUpdate);
  }
}
