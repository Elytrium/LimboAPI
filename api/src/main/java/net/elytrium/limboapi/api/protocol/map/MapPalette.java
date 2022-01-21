/*
 * Copyright (C) 2021 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.protocol.map;

import com.velocitypowered.api.network.ProtocolVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.awt.image.BufferedImage;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.elytrium.limboapi.api.utils.OverlayVanillaMap;

@SuppressWarnings("unused")
@SuppressFBWarnings("MS_EXPOSE_REP")
public class MapPalette {

  private static final Color[] colors = new Color[]{
      clr(0, 0, 0),
      clr(127, 178, 56),
      clr(247, 233, 163),
      clr(199, 199, 199),
      clr(255, 0, 0),
      clr(160, 160, 255),
      clr(167, 167, 167),
      clr(0, 124, 0),
      clr(255, 255, 255),
      clr(164, 168, 184),
      clr(151, 109, 77),
      clr(112, 112, 112),
      clr(64, 64, 255),
      clr(143, 119, 72),
      clr(255, 252, 245),
      clr(216, 127, 51),
      clr(178, 76, 216),
      clr(102, 153, 216),
      clr(229, 229, 51),
      clr(127, 204, 25),
      clr(242, 127, 165),
      clr(76, 76, 76),
      clr(153, 153, 153),
      clr(76, 127, 153),
      clr(127, 63, 178),
      clr(51, 76, 178),
      clr(102, 76, 51),
      clr(102, 127, 51),
      clr(153, 51, 51),
      clr(25, 25, 25),
      clr(250, 238, 77),
      clr(92, 219, 213),
      clr(74, 128, 255),
      clr(0, 217, 58),
      clr(129, 86, 49),
      clr(112, 2, 0, ProtocolVersion.MINECRAFT_1_8),
      clr(209, 177, 161, ProtocolVersion.MINECRAFT_1_12),
      clr(159, 82, 36, ProtocolVersion.MINECRAFT_1_12),
      clr(149, 87, 108, ProtocolVersion.MINECRAFT_1_12),
      clr(112, 108, 138, ProtocolVersion.MINECRAFT_1_12),
      clr(186, 133, 36, ProtocolVersion.MINECRAFT_1_12),
      clr(103, 117, 53, ProtocolVersion.MINECRAFT_1_12),
      clr(160, 77, 78, ProtocolVersion.MINECRAFT_1_12),
      clr(57, 41, 35, ProtocolVersion.MINECRAFT_1_12),
      clr(135, 107, 98, ProtocolVersion.MINECRAFT_1_12),
      clr(87, 92, 92, ProtocolVersion.MINECRAFT_1_12),
      clr(122, 73, 88, ProtocolVersion.MINECRAFT_1_12),
      clr(76, 62, 92, ProtocolVersion.MINECRAFT_1_12),
      clr(76, 50, 35, ProtocolVersion.MINECRAFT_1_12),
      clr(76, 82, 42, ProtocolVersion.MINECRAFT_1_12),
      clr(142, 60, 46, ProtocolVersion.MINECRAFT_1_12),
      clr(37, 22, 16, ProtocolVersion.MINECRAFT_1_12),
      clr(189, 48, 49, ProtocolVersion.MINECRAFT_1_16),
      clr(148, 63, 97, ProtocolVersion.MINECRAFT_1_16),
      clr(92, 25, 29, ProtocolVersion.MINECRAFT_1_16),
      clr(22, 126, 134, ProtocolVersion.MINECRAFT_1_16),
      clr(58, 142, 140, ProtocolVersion.MINECRAFT_1_16),
      clr(86, 44, 62, ProtocolVersion.MINECRAFT_1_16),
      clr(20, 180, 133, ProtocolVersion.MINECRAFT_1_16),
      clr(100, 100, 100, ProtocolVersion.MINECRAFT_1_16),
      clr(216, 175, 147, ProtocolVersion.MINECRAFT_1_17),
      clr(127, 167, 150, ProtocolVersion.MINECRAFT_1_17),
  };

  private static final Map<ProtocolVersion, Map<Color, Byte>> colorToIndexMap = new ConcurrentHashMap<>();
  private static final Map<ProtocolVersion, Map<Color, Byte>> cachedColorToIndexMap = new ConcurrentHashMap<>();
  private static final Set<Color> cachedColors = new HashSet<>();

  @Deprecated
  public static final byte WHITE = 34;

  private static final byte MINECRAFT_MULTIPLIER = 4;

  static {
    Map<Color, Byte> previous = new ConcurrentHashMap<>();
    Map<Color, Byte> previousCached = new ConcurrentHashMap<>();
    for (ProtocolVersion version : EnumSet.range(ProtocolVersion.MINIMUM_VERSION, ProtocolVersion.MAXIMUM_VERSION)) {
      Map<Color, Byte> current = new OverlayVanillaMap<>(previous, new ConcurrentHashMap<>());
      colorToIndexMap.put(version, current);
      previous = current;

      Map<Color, Byte> currentCached = new OverlayVanillaMap<>(previousCached, new ConcurrentHashMap<>());
      cachedColorToIndexMap.put(version, currentCached);
      previousCached = currentCached;
    }

    for (byte i = 0; i < colors.length; i++) {
      Color color = colors[i];

      for (byte j = 0; j < MINECRAFT_MULTIPLIER; ++j) {
        int index = i * MINECRAFT_MULTIPLIER + j;
        colorToIndexMap.get(color.getSince()).put(color.multiplyAndDownscale(j), (byte) (index < 128 ? index : -129 + (index - 127)));
      }
    }
  }

  public static void precache() {
    for (int i = 0; i < 16; ++i) {
      for (int j = 0; j < 16; ++j) {
        for (int k = 0; k < 16; ++k) {
          Color fastColor = clr(i << 4, j << 4, k << 4);
          precacheColor(fastColor);
        }
      }
    }
  }

  private static void precacheColor(Color color) {
    if (cachedColors.contains(color)) {
      return;
    }

    for (ProtocolVersion version : EnumSet.range(ProtocolVersion.MINIMUM_VERSION, ProtocolVersion.MAXIMUM_VERSION)) {
      Map<Color, Byte> current = cachedColorToIndexMap.get(version);
      byte matched = matchColor(color, version);
      if (!current.containsKey(color) || current.get(color) != matched) {
        current.put(color, matched);
      }
    }

    cachedColors.add(color);
  }

  private static Color clr(int r, int g, int b) {
    return new Color(r, g, b);
  }

  private static Color clr(int r, int g, int b, ProtocolVersion since) {
    return new Color(r, g, b, since);
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
   * Uses reduced set of colors, to support more colors use {@link MapPalette#imageToBytes(BufferedImage, ProtocolVersion)}
   *
   * @param image The image to convert.
   * @return A byte[] containing the pixels of the image.
   */
  public static int[] imageToBytes(BufferedImage image) {
    return imageToBytes(image, ProtocolVersion.MINIMUM_VERSION);
  }

  /**
   * Convert an Image to a byte[] using the palette.
   *
   * @param image   The image to convert.
   * @param version The ProtocolVersion to support more colors.
   * @return A byte[] containing the pixels of the image.
   */
  public static int[] imageToBytes(BufferedImage image, ProtocolVersion version) {
    int[] result = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
    for (int i = 0; i < result.length; ++i) {
      result[i] = tryFastMatchColor(result[i], version);
    }

    return result;
  }

  /**
   * Get the index of the closest matching color in the palette to the given
   * color. Uses caching and downscaling of color values.
   *
   * @param rgb The Color to match.
   * @return The index in the palette.
   */
  public static byte tryFastMatchColor(int rgb, ProtocolVersion version) {
    if (getAlpha(rgb) < 128) {
      rgb = 0xFFFFFF;
    }

    Color color = downscaleRGB(rgb);
    precacheColor(color);

    return cachedColorToIndexMap.get(version).get(color);
  }

  /**
   * Get the index of the closest matching color in the palette to the given
   * color.
   *
   * @param color The Color to match.
   * @return The index in the palette.
   */
  public static byte matchColor(Color color, ProtocolVersion version) {
    Color match = colors[0];
    double best = -1.0D;

    for (Color cachedColor : colorToIndexMap.get(version).keySet()) {
      double distance = getDistance(color, cachedColor);

      if (distance < best || best == -1.0D) {
        best = distance;
        match = cachedColor;
      }
    }

    return colorToIndexMap.get(version).get(match);
  }

  private static Color downscaleRGB(int rgb) {
    int r = ((rgb & 0xff0000) >>> 16) & ~15;
    int g = ((rgb & 0xff00) >>> 8) & ~15;
    int b = (rgb & 0xff) & ~15;

    return new Color(r, g, b);
  }

  private static int getAlpha(int rgb) {
    return (rgb & 0xff000000) >>> 24;
  }

  public static Color[] getColors() {
    return MapPalette.colors;
  }

  public static class Color {

    private final int red;
    private final int green;
    private final int blue;
    private final ProtocolVersion since;

    public Color(int red, int green, int blue) {
      this.red = red;
      this.green = green;
      this.blue = blue;
      this.since = ProtocolVersion.MINIMUM_VERSION;
    }

    public Color(int red, int green, int blue, ProtocolVersion since) {
      this.red = red;
      this.green = green;
      this.blue = blue;
      this.since = since;
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

    public Color multiplyAndDownscale(int multiplier) {
      switch (multiplier) {
        case 0: {
          int red = ((this.red * 180) >> 8) & ~15;
          int green = ((this.green * 180) >> 8) & ~15;
          int blue = ((this.blue * 180) >> 8) & ~15;
          return new Color(red, green, blue, this.since);
        }
        case 1: {
          int red = ((this.red * 220) >> 8) & ~15;
          int green = ((this.green * 220) >> 8) & ~15;
          int blue = ((this.blue * 220) >> 8) & ~15;
          return new Color(red, green, blue, this.since);
        }
        case 2: {
          return new Color(this.red & ~15, this.green & ~15, this.blue & ~15, this.since);
        }
        case 3: {
          int red = ((this.red * 135) >> 8) & ~15;
          int green = ((this.green * 135) >> 8) & ~15;
          int blue = ((this.blue * 135) >> 8) & ~15;
          return new Color(red, green, blue, this.since);
        }
        default: {
          return this;
        }
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

    public ProtocolVersion getSince() {
      return this.since;
    }
  }
}
