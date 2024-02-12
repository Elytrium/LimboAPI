/*
 * Copyright (C) 2021 - 2024 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.player;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import java.awt.image.BufferedImage;
import java.util.concurrent.ScheduledExecutorService;
import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.material.VirtualItem;
import net.kyori.adventure.nbt.CompoundBinaryTag;

public interface LimboPlayer {

  void writePacket(Object packetObj);

  void writePacketAndFlush(Object packetObj);

  void flushPackets();

  void closeWith(Object packetObj);

  ScheduledExecutorService getScheduledExecutor();

  void sendImage(BufferedImage image);

  void sendImage(BufferedImage image, boolean sendItem);

  void sendImage(int mapID, BufferedImage image);

  void sendImage(int mapID, BufferedImage image, boolean sendItem);

  void sendImage(int mapID, BufferedImage image, boolean sendItem, boolean resize);

  void setInventory(VirtualItem item, int count);

  void setInventory(VirtualItem item, int slot, int count);

  void setInventory(int slot, VirtualItem item, int count, int data, CompoundBinaryTag nbt);

  void setGameMode(GameMode gameMode);

  void teleport(double posX, double posY, double posZ, float yaw, float pitch);

  void disableFalling();

  void enableFalling();

  void disconnect();

  void disconnect(RegisteredServer server);

  void sendAbilities();

  void sendAbilities(int abilities, float flySpeed, float walkSpeed);

  void sendAbilities(byte abilities, float flySpeed, float walkSpeed);

  byte getAbilities();

  GameMode getGameMode();

  Limbo getServer();

  Player getProxyPlayer();

  int getPing();

  void setWorldTime(long ticks);
}
