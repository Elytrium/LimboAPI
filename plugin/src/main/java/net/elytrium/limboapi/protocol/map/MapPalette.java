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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class MapPalette {

  private static final Color[] colors = new Color[] {
      clr(0, 0, 0), clr(0, 0, 0), clr(0, 0, 0), clr(0, 0, 0),
      clr(89, 125, 39), clr(109, 153, 48), clr(127, 178, 56), clr(67, 94, 29),
      clr(174, 164, 115), clr(213, 201, 140), clr(247, 233, 163), clr(130, 123, 86),
      clr(140, 140, 140), clr(171, 171, 171), clr(199, 199, 199), clr(105, 105, 105),
      clr(180, 0, 0), clr(220, 0, 0), clr(255, 0, 0), clr(135, 0, 0),
      clr(112, 112, 180), clr(138, 138, 220), clr(160, 160, 255), clr(84, 84, 135),
      clr(117, 117, 117), clr(144, 144, 144), clr(167, 167, 167), clr(88, 88, 88),
      clr(0, 87, 0), clr(0, 106, 0), clr(0, 124, 0), clr(0, 65, 0),
      clr(180, 180, 180), clr(220, 220, 220), clr(255, 255, 255), clr(135, 135, 135),
      clr(115, 118, 129), clr(141, 144, 158), clr(164, 168, 184), clr(86, 88, 97),
      clr(106, 76, 54), clr(130, 94, 66), clr(151, 109, 77), clr(79, 57, 40),
      clr(79, 79, 79), clr(96, 96, 96), clr(112, 112, 112), clr(59, 59, 59),
      clr(45, 45, 180), clr(55, 55, 220), clr(64, 64, 255), clr(33, 33, 135),
      clr(100, 84, 50), clr(123, 102, 62), clr(143, 119, 72), clr(75, 63, 38),
      clr(180, 177, 172), clr(220, 217, 211), clr(255, 252, 245), clr(135, 133, 129),
      clr(152, 89, 36), clr(186, 109, 44), clr(216, 127, 51), clr(114, 67, 27),
      clr(125, 53, 152), clr(153, 65, 186), clr(178, 76, 216), clr(94, 40, 114),
      clr(72, 108, 152), clr(88, 132, 186), clr(102, 153, 216), clr(54, 81, 114),
      clr(161, 161, 36), clr(197, 197, 44), clr(229, 229, 51), clr(121, 121, 27),
      clr(89, 144, 17), clr(109, 176, 21), clr(127, 204, 25), clr(67, 108, 13),
      clr(170, 89, 116), clr(208, 109, 142), clr(242, 127, 165), clr(128, 67, 87),
      clr(53, 53, 53), clr(65, 65, 65), clr(76, 76, 76), clr(40, 40, 40),
      clr(108, 108, 108), clr(132, 132, 132), clr(153, 153, 153), clr(81, 81, 81),
      clr(53, 89, 108), clr(65, 109, 132), clr(76, 127, 153), clr(40, 67, 81),
      clr(89, 44, 125), clr(109, 54, 153), clr(127, 63, 178), clr(67, 33, 94),
      clr(36, 53, 125), clr(44, 65, 153), clr(51, 76, 178), clr(27, 40, 94),
      clr(72, 53, 36), clr(88, 65, 44), clr(102, 76, 51), clr(54, 40, 27),
      clr(72, 89, 36), clr(88, 109, 44), clr(102, 127, 51), clr(54, 67, 27),
      clr(108, 36, 36), clr(132, 44, 44), clr(153, 51, 51), clr(81, 27, 27),
      clr(17, 17, 17), clr(21, 21, 21), clr(25, 25, 25), clr(13, 13, 13),
      clr(176, 168, 54), clr(215, 205, 66), clr(250, 238, 77), clr(132, 126, 40),
      clr(64, 154, 150), clr(79, 188, 183), clr(92, 219, 213), clr(48, 115, 112),
      clr(52, 90, 180), clr(63, 110, 220), clr(74, 128, 255), clr(39, 67, 135),
      clr(0, 153, 40), clr(0, 187, 50), clr(0, 217, 58), clr(0, 114, 30),
      clr(91, 60, 34), clr(111, 74, 42), clr(129, 86, 49), clr(68, 45, 25),
      clr(79, 1, 0), clr(96, 1, 0), clr(112, 2, 0), clr(59, 1, 0),
  };

  public static final byte WHITE = 34;

  private static final Map<Color, Byte> colorToIndexMap = new ConcurrentHashMap<>();

  private static Color clr(int r, int g, int b) {
    return new Color(r, g, b);
  }

  private static double getDistance(Color c1, Color c2) {
    double rmean = (double) (c1.getRed() + c2.getRed()) / 2.0D;
    double r = c1.getRed() - c2.getRed();
    double g = c1.getGreen() - c2.getGreen();
    int b = c1.getBlue() - c2.getBlue();
    double weightR = 2.0D + rmean / 256.0D;
    double weightG = 4.0D;
    double weightB = 2.0D + (255.0D - rmean) / 256.0D;

    return weightR * r * r + weightG * g * g + weightB * (double) b * (double) b;
  }

  /**
   * Convert an Image to a byte[] using the palette.
   *
   * @param image The image to convert.
   * @return A byte[] containing the pixels of the image.
   */
  public static int[] imageToBytes(final BufferedImage image) {
    int[] result = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
    for (int i = 0; i < result.length; ++i) {
      result[i] = tryFastMatchColor(result[i]);
    }

    return result;
  }

  public static byte tryFastMatchColor(int rgb) {
    if (getAlpha(rgb) < 128) {
      return WHITE;
    }

    Color color = simplifyRGB(rgb);
    Byte colorId = colorToIndexMap.get(color);
    if (colorId != null) {
      return colorId;
    } else {
      return matchColor(color);
    }
  }

  /**
   * Get the index of the closest matching color in the palette to the given
   * color.
   *
   * @param color The Color to match.
   * @return The index in the palette.
   */
  public static byte matchColor(Color color) {
    int index = 0;
    double best = -1.0D;

    for (int i = 4; i < colors.length; ++i) {
      double distance = getDistance(color, colors[i]);

      if (distance < best || best == -1.0D) {
        best = distance;
        index = i;
      }
    }

    byte matched = (byte) (index < 128 ? index : -129 + (index - 127));
    colorToIndexMap.put(color, matched);

    return matched;
  }

  private static Color simplifyRGB(int rgb) {
    int r = ((rgb & 0xff0000) >>> 16);
    int g = ((rgb & 0xff00) >>> 8);
    int b = (rgb & 0xff);

    return new Color(r, g, b);
  }

  private static int getAlpha(int rgb) {
    return (rgb & 0xff000000) >>> 24;
  }

  public static Color[] getColors() {
    return MapPalette.colors.clone();
  }

  public static class Color {

    private final int red;
    private final int green;
    private final int blue;

    public Color(int red, int green, int blue) {
      this.red = red;
      this.green = green;
      this.blue = blue;
    }

    @Override
    public int hashCode() {
      return (this.red << 16) | (this.green << 8) | (this.blue);
    }

    @Override
    public boolean equals(Object otherColor) {
      if (otherColor instanceof Color) {
        Color checkColor = (Color) otherColor;
        return checkColor.red == this.red && checkColor.blue == this.blue && checkColor.green == this.green;
      } else {
        return false;
      }
    }

    public java.awt.Color toJava() {
      return new java.awt.Color(this.red, this.green, this.blue);
    }

    public int getRed() {
      return this.red;
    }

    public int getGreen() {
      return this.green;
    }

    public int getBlue() {
      return this.blue;
    }
  }
}
