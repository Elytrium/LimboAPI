/*
 * Copyright (C) 2021 - 2024 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.protocol.map;

import com.velocitypowered.api.network.ProtocolVersion;
import java.awt.image.BufferedImage;

@Deprecated(forRemoval = true)
public class MapPalette {

  public static int[] imageToBytes(BufferedImage image) {
    return net.elytrium.limboapi.api.protocol.packets.data.MapPalette.imageToBytes(image);
  }

  public static int[] imageToBytes(BufferedImage image, ProtocolVersion version) {
    return net.elytrium.limboapi.api.protocol.packets.data.MapPalette.imageToBytes(image, version);
  }

  public static byte tryFastMatchColor(int rgb, ProtocolVersion version) {
    return net.elytrium.limboapi.api.protocol.packets.data.MapPalette.tryFastMatchColor(rgb, version);
  }
}
