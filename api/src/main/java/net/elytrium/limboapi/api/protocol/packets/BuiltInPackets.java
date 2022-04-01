/*
 * Copyright (C) 2021 - 2022 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.protocol.packets;

import net.elytrium.java.commons.reflection.ReflectionException;

@SuppressWarnings("unused")
public enum BuiltInPackets {

  ChangeGameState("net.elytrium.limboapi.protocol.packet.ChangeGameState"),
  ChunkData("net.elytrium.limboapi.protocol.packet.world.ChunkData"),
  DefaultSpawnPosition("net.elytrium.limboapi.protocol.packet.DefaultSpawnPosition"),
  MapData("net.elytrium.limboapi.protocol.packet.MapDataPacket"),
  Player("net.elytrium.limboapi.protocol.packet.Player"),
  PlayerAbilities("net.elytrium.limboapi.protocol.packet.PlayerAbilities"),
  PlayerPosition("net.elytrium.limboapi.protocol.packet.PlayerPosition"),
  PlayerPositionAndLook("net.elytrium.limboapi.protocol.packet.PlayerPositionAndLook"),
  SetExperience("net.elytrium.limboapi.protocol.packet.SetExperience"),
  SetSlot("net.elytrium.limboapi.protocol.packet.SetSlot"),
  TeleportConfirm("net.elytrium.limboapi.protocol.packet.TeleportConfirm"),
  UpdateViewPosition("net.elytrium.limboapi.protocol.packet.UpdateViewPosition");

  private final Class<?> packetClass;

  BuiltInPackets(String packetClass) {
    try {
      this.packetClass = Class.forName(packetClass);
    } catch (ClassNotFoundException e) {
      throw new ReflectionException(e);
    }
  }

  public Class<?> getPacketClass() {
    return this.packetClass;
  }
}
