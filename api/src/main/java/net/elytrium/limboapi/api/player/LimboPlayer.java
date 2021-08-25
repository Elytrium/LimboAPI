/*
 * Copyright (C) 2021 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.player;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import java.awt.image.BufferedImage;
import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.material.VirtualItem;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.text.Component;

public interface LimboPlayer {
  void sendImage(int mapId, BufferedImage image);

  void setInventory(int slot, VirtualItem item, int count, int data, CompoundBinaryTag nbt);

  void teleport(double x, double y, double z, float yaw, float pitch);

  void sendTitle(Component title, Component subtitle, ProtocolVersion version, int fadeIn, int stay, int fadeOut);

  void disconnect();

  void disconnect(RegisteredServer server);

  Limbo getServer();

  Player getProxyPlayer();
}
