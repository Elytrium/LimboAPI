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

package net.elytrium.limboapi.protocol.map;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import net.elytrium.limboapi.protocol.packet.MapDataPacket;

@SuppressFBWarnings("EI_EXPOSE_REP")
public class CraftMapCanvas {

  public static final int MAP_SIZE = 16384; // 128 x 128

  private final byte[] canvas;
  private final byte[][] canvas17; // 1.7.x canvas

  public CraftMapCanvas() {
    this.canvas = new byte[MAP_SIZE];
    Arrays.fill(this.canvas, MapPalette.WHITE);

    this.canvas17 = new byte[128][128];
    for (int x = 0; x < 128; ++x) {
      for (int y = 0; y < 128; ++y) {
        this.canvas17[x][y] = MapPalette.WHITE;
      }
    }
  }

  public CraftMapCanvas(CraftMapCanvas another) {
    byte[] canvasBuf = new byte[MAP_SIZE];
    System.arraycopy(another.getCanvas(), 0, canvasBuf, 0, MAP_SIZE);
    this.canvas = canvasBuf;

    this.canvas17 = Arrays.stream(another.get17Canvas()).map(byte[]::clone).toArray(byte[][]::new);
  }

  public void setPixel(int x, int y, byte color) {
    if (x >= 0 && y >= 0 && x < 128 && y < 128) {
      this.canvas[y * 128 + x] = color;
      this.canvas17[x][y] = color;
    }
  }

  public void drawImage(int x, int y, BufferedImage image) {
    int[] bytes = MapPalette.imageToBytes(image);
    int width = image.getWidth(null);
    int height = image.getHeight(null);

    for (int x2 = 0; x2 < width; ++x2) {
      for (int y2 = 0; y2 < height; ++y2) {
        byte color = (byte) bytes[y2 * width + x2];
        if (color != MapPalette.WHITE) {
          this.setPixel(x + x2, y + y2, color);
        }
      }
    }
  }

  public MapDataPacket.MapData getMapData() {
    return new MapDataPacket.MapData(this.canvas);
  }

  public MapDataPacket.MapData[] get17MapsData() {
    MapDataPacket.MapData[] maps = new MapDataPacket.MapData[128];
    for (int i = 0; i < 128; i++) {
      maps[i] = new MapDataPacket.MapData(i, this.canvas17[i]);
    }

    return maps;
  }

  public byte[] getCanvas() {
    return this.canvas;
  }

  public byte[][] get17Canvas() {
    return this.canvas17;
  }
}
