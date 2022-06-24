/*
 * Copyright (C) 2021 - 2022 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.protocol.packets.data;

import com.google.common.io.ByteStreams;
import com.velocitypowered.api.network.ProtocolVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;

@SuppressFBWarnings("MS_EXPOSE_REP")
public class MapPalette {
  private static final Map<MapVersion, byte[]> REMAP_BUFFERS = new EnumMap<>(MapVersion.class);

  private static final byte[] MAIN_BUFFER = readBuffer("/mapping/colors_main_map");
  /**
   * @deprecated Use {@link java.awt.Color#WHITE} instead.
   */
  @Deprecated
  public static final byte WHITE = 34;

  static {
    for (MapVersion version : MapVersion.values()) {
      REMAP_BUFFERS.put(version, readBuffer("/mapping/colors_" + version.toString().toLowerCase(Locale.ROOT) + "_map"));
    }
  }

  @SuppressWarnings("ConstantConditions")
  @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE")
  private static byte[] readBuffer(String filename) {
    try (InputStream stream = MapPalette.class.getResourceAsStream(filename)) {
      return ByteStreams.toByteArray(stream);
    } catch (IOException e) {
      e.printStackTrace();
    }

    return null;
  }

  /**
   * Convert an Image to a byte[] using the palette.
   * Uses reduced set of colors, to support more colors use {@link MapPalette#imageToBytes(BufferedImage, ProtocolVersion)}
   *
   * @param image The image to convert.
   *
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
   *
   * @return A byte[] containing the pixels of the image.
   */
  public static int[] imageToBytes(BufferedImage image, ProtocolVersion version) {
    int[] result = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
    return imageToBytes(result, version);
  }

  /**
   * Convert an image to a byte[] using the palette.
   *
   * @param image   The image to convert.
   * @param version The ProtocolVersion to support more colors.
   *
   * @return A byte[] containing the pixels of the image.
   */
  public static int[] imageToBytes(int[] image, ProtocolVersion version) {
    for (int i = 0; i < image.length; ++i) {
      image[i] = tryFastMatchColor(image[i], version);
    }

    return image;
  }

  /**
   * Convert an image from MapVersion.MAXIMUM_VERSION to the desired version
   *
   * @param image   The image to convert.
   * @param version The ProtocolVersion to support more colors.
   *
   * @return A byte[] containing the pixels of the image.
   */
  public static int[] convertImage(int[] image, MapVersion version) {
    byte[] remapBuffer = REMAP_BUFFERS.get(version);
    for (int i = 0; i < image.length; ++i) {
      image[i] = remapByte(remapBuffer, image[i]);
    }

    return image;
  }

  /**
   * Get the index of the closest matching color in the palette to the given
   * color. Uses caching and downscaling of color values.
   *
   * @param rgb The Color to match.
   *
   * @return The index in the palette.
   */
  public static byte tryFastMatchColor(int rgb, ProtocolVersion version) {
    if (getAlpha(rgb) < 128) {
      rgb = 0xFFFFFF;
    }

    assert MAIN_BUFFER != null;
    int origIdx = MAIN_BUFFER[rgb & 0xFFFFFF];

    MapVersion mapVersion = MapVersion.fromProtocolVersion(version);
    if (mapVersion == MapVersion.MAXIMUM_VERSION) {
      return (byte) origIdx;
    } else {
      return remapByte(REMAP_BUFFERS.get(mapVersion), origIdx);
    }
  }

  private static byte remapByte(byte[] remapBuffer, int oldByte) {
    if (oldByte < 0) {
      oldByte += 256;
    }

    return remapBuffer[oldByte];
  }

  private static int getAlpha(int rgb) {
    return (rgb & 0xFF000000) >>> 24;
  }

  public enum MapVersion {
    MINIMUM_VERSION(EnumSet.range(ProtocolVersion.MINECRAFT_1_7_2, ProtocolVersion.MINECRAFT_1_7_6)),
    MINECRAFT_1_8(EnumSet.range(ProtocolVersion.MINECRAFT_1_8, ProtocolVersion.MINECRAFT_1_11_1)),
    MINECRAFT_1_12(EnumSet.range(ProtocolVersion.MINECRAFT_1_12, ProtocolVersion.MINECRAFT_1_15_2)),
    MINECRAFT_1_16(EnumSet.range(ProtocolVersion.MINECRAFT_1_16, ProtocolVersion.MINECRAFT_1_16_4)),
    MINECRAFT_1_17(EnumSet.range(ProtocolVersion.MINECRAFT_1_17, ProtocolVersion.MAXIMUM_VERSION));

    public static final MapVersion MAXIMUM_VERSION = MINECRAFT_1_17;

    private static final EnumMap<ProtocolVersion, MapVersion> versionsMap = new EnumMap<>(ProtocolVersion.class);

    private final EnumSet<ProtocolVersion> versions;

    static {
      for (MapVersion value : MapVersion.values()) {
        value.versions.forEach(v -> versionsMap.put(v, value));
      }
    }

    MapVersion(EnumSet<ProtocolVersion> versions) {
      this.versions = versions;
    }

    public static MapVersion fromProtocolVersion(ProtocolVersion version) {
      return versionsMap.get(version);
    }
  }
}
