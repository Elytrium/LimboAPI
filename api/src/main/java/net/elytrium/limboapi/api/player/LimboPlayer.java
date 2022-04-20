/*
 * Copyright (C) 2021 - 2022 Elytrium
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
import net.kyori.adventure.title.Title;

public interface LimboPlayer {

  void writePacket(Object packetObj);

  void writePacketAndFlush(Object packetObj);

  void flushPackets();

  void closeWith(Object packetObj);

  void sendImage(BufferedImage image);

  void sendImage(BufferedImage image, boolean sendItem);

  void sendImage(int mapId, BufferedImage image);

  void sendImage(int mapId, BufferedImage image, boolean sendItem);

  void sendImage(int mapId, BufferedImage image, boolean sendItem, boolean resize);

  void setInventory(VirtualItem item, int count);

  void setInventory(VirtualItem item, int slot, int count);

  void setInventory(int slot, VirtualItem item, int count, int data, CompoundBinaryTag nbt);

  void setGameMode(GameMode gameMode);

  void teleport(double x, double y, double z, float yaw, float pitch);

  /**
   * @deprecated Use {@link Player#showTitle(Title)}
   */
  @Deprecated
  void sendTitle(Component title, Component subtitle, ProtocolVersion version, int fadeIn, int stay, int fadeOut);

  void disableFalling();

  void disconnect();

  void disconnect(RegisteredServer server);

  void sendAbilities();

  void sendAbilities(byte abilities, float flySpeed, float walkSpeed);

  byte getAbilities();

  GameMode getGameMode();

  Limbo getServer();

  Player getProxyPlayer();

  int getPing();
}
