/*
 * Copyright (C) 2021 - 2024 Elytrium
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

package net.elytrium.limboapi.protocol.packets;

import com.velocitypowered.api.network.ProtocolVersion;
import java.util.List;
import java.util.Map;
import net.elytrium.limboapi.api.chunk.Dimension;
import net.elytrium.limboapi.api.chunk.data.ChunkSnapshot;
import net.elytrium.limboapi.api.material.VirtualItem;
import net.elytrium.limboapi.api.material.WorldVersion;
import net.elytrium.limboapi.api.protocol.packets.PacketFactory;
import net.elytrium.limboapi.api.protocol.packets.data.MapData;
import net.elytrium.limboapi.protocol.packets.s2c.ChangeGameStatePacket;
import net.elytrium.limboapi.protocol.packets.s2c.ChunkDataPacket;
import net.elytrium.limboapi.protocol.packets.s2c.DefaultSpawnPositionPacket;
import net.elytrium.limboapi.protocol.packets.s2c.MapDataPacket;
import net.elytrium.limboapi.protocol.packets.s2c.PlayerAbilitiesPacket;
import net.elytrium.limboapi.protocol.packets.s2c.PositionRotationPacket;
import net.elytrium.limboapi.protocol.packets.s2c.SetExperiencePacket;
import net.elytrium.limboapi.protocol.packets.s2c.SetSlotPacket;
import net.elytrium.limboapi.protocol.packets.s2c.TimeUpdatePacket;
import net.elytrium.limboapi.protocol.packets.s2c.UpdateTagsPacket;
import net.elytrium.limboapi.protocol.packets.s2c.UpdateViewPositionPacket;
import net.elytrium.limboapi.server.world.SimpleTagManager;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.checkerframework.checker.nullness.qual.Nullable;

public class PacketFactoryImpl implements PacketFactory {

  @Override
  public Object createChangeGameStatePacket(int reason, float value) {
    return new ChangeGameStatePacket(reason, value);
  }

  @Override
  public Object createChunkDataPacket(ChunkSnapshot chunkSnapshot, boolean legacySkyLight, int maxSections) {
    return new ChunkDataPacket(chunkSnapshot, legacySkyLight, maxSections);
  }

  @Override
  public Object createChunkDataPacket(ChunkSnapshot chunkSnapshot, Dimension dimension) {
    return new ChunkDataPacket(chunkSnapshot, dimension.hasLegacySkyLight(), dimension.getMaxSections());
  }

  @Override
  public Object createDefaultSpawnPositionPacket(int posX, int posY, int posZ, float angle) {
    return new DefaultSpawnPositionPacket(posX, posY, posZ, angle);
  }

  @Override
  public Object createMapDataPacket(int mapID, byte scale, MapData mapData) {
    return new MapDataPacket(mapID, scale, mapData);
  }

  @Override
  public Object createPlayerAbilitiesPacket(int flags, float flySpeed, float walkSpeed) {
    return new PlayerAbilitiesPacket((byte) flags, flySpeed, walkSpeed);
  }

  @Override
  public Object createPlayerAbilitiesPacket(byte flags, float flySpeed, float walkSpeed) {
    return new PlayerAbilitiesPacket(flags, flySpeed, walkSpeed);
  }

  @Override
  public Object createPositionRotationPacket(double posX, double posY, double posZ, float yaw, float pitch,
      boolean onGround, int teleportID, boolean dismountVehicle) {
    return new PositionRotationPacket(posX, posY, posZ, yaw, pitch, onGround, teleportID, dismountVehicle);
  }

  @Override
  public Object createSetExperiencePacket(float expBar, int level, int totalExp) {
    return new SetExperiencePacket(expBar, level, totalExp);
  }

  @Override
  public Object createSetSlotPacket(int windowID, int slot, VirtualItem item, int count, int data, @Nullable CompoundBinaryTag nbt) {
    return new SetSlotPacket(windowID, slot, item, count, data, nbt);
  }

  @Override
  public Object createTimeUpdatePacket(long worldAge, long timeOfDay) {
    return new TimeUpdatePacket(worldAge, timeOfDay);
  }

  @Override
  public Object createUpdateViewPositionPacket(int posX, int posZ) {
    return new UpdateViewPositionPacket(posX, posZ);
  }

  @Override
  public Object createUpdateTagsPacket(WorldVersion version) {
    return SimpleTagManager.getUpdateTagsPacket(version);
  }

  @Override
  public Object createUpdateTagsPacket(ProtocolVersion version) {
    return SimpleTagManager.getUpdateTagsPacket(version);
  }

  @Override
  public Object createUpdateTagsPacket(Map<String, Map<String, List<Integer>>> tags) {
    return new UpdateTagsPacket(tags);
  }
}
