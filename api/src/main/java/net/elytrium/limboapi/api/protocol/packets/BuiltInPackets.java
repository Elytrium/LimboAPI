/*
 * Copyright (C) 2021 - 2022 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.protocol.packets;

import net.elytrium.java.commons.reflection.ReflectionException;

@Deprecated(forRemoval = true)
public enum BuiltInPackets {

  ChangeGameState("net.elytrium.limboapi.protocol.packets.s2c.ChangeGameStatePacket"),
  ChunkData("net.elytrium.limboapi.protocol.packets.s2c.ChunkDataPacket"),
  DefaultSpawnPosition("net.elytrium.limboapi.protocol.packets.s2c.DefaultSpawnPositionPacket"),
  MapData("net.elytrium.limboapi.protocol.packets.s2c.MapDataPacket"),
  PlayerAbilities("net.elytrium.limboapi.protocol.packets.s2c.PlayerAbilitiesPacket"),
  PlayerPositionAndLook("net.elytrium.limboapi.protocol.packets.s2c.PositionRotationPacket"),
  SetExperience("net.elytrium.limboapi.protocol.packets.s2c.SetExperiencePacket"),
  SetSlot("net.elytrium.limboapi.protocol.packets.s2c.SetSlotPacket"),
  TimeUpdate("net.elytrium.limboapi.protocol.packets.s2c.TimeUpdatePacket"),
  UpdateViewPosition("net.elytrium.limboapi.protocol.packets.s2c.UpdateViewPositionPacket");

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
