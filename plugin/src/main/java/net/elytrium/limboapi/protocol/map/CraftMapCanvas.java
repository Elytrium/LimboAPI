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

import java.awt.image.BufferedImage;
import java.util.Arrays;
import lombok.Getter;
import net.elytrium.limboapi.protocol.packet.MapDataPacket;

@Getter
public class CraftMapCanvas {

  private final byte[] canvas;
  private static final int MAP_SIZE = 16384; //128x128

  public CraftMapCanvas() {
    this.canvas = new byte[MAP_SIZE];
    Arrays.fill(this.canvas, MapPalette.WHITE);
  }

  public CraftMapCanvas(byte[] canvas) {
    final byte[] canvasBuf = new byte[MAP_SIZE];
    System.arraycopy(canvas, 0, canvasBuf, 0, MAP_SIZE);
    this.canvas = canvasBuf;
  }

  public void setPixel(int x, int y, byte color) {
    if (x >= 0 && y >= 0 && x < 128 && y < 128) {
      this.canvas[y * 128 + x] = color;
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
    return new MapDataPacket.MapData(128, 128, 0, 0, this.canvas);
  }
}
