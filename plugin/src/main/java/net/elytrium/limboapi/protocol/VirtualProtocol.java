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
 *
 * This file contains some parts of Velocity, licensed under the AGPLv3 License (AGPLv3).
 *
 * Copyright (C) 2018 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.elytrium.limboapi.protocol;

import static com.google.common.collect.Iterables.getLast;
import static com.velocitypowered.api.network.ProtocolVersion.MINIMUM_VERSION;
import static com.velocitypowered.api.network.ProtocolVersion.SUPPORTED_VERSIONS;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.Chat;
import com.velocitypowered.proxy.protocol.packet.ClientSettings;
import com.velocitypowered.proxy.protocol.packet.Disconnect;
import com.velocitypowered.proxy.protocol.packet.JoinGame;
import com.velocitypowered.proxy.protocol.packet.PluginMessage;
import com.velocitypowered.proxy.protocol.packet.Respawn;
import com.velocitypowered.proxy.protocol.packet.title.LegacyTitlePacket;
import com.velocitypowered.proxy.protocol.packet.title.TitleActionbarPacket;
import com.velocitypowered.proxy.protocol.packet.title.TitleClearPacket;
import com.velocitypowered.proxy.protocol.packet.title.TitleSubtitlePacket;
import com.velocitypowered.proxy.protocol.packet.title.TitleTextPacket;
import com.velocitypowered.proxy.protocol.packet.title.TitleTimesPacket;
import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import lombok.SneakyThrows;
import net.elytrium.limboapi.protocol.packet.MapDataPacket;
import net.elytrium.limboapi.protocol.packet.Player;
import net.elytrium.limboapi.protocol.packet.PlayerAbilities;
import net.elytrium.limboapi.protocol.packet.PlayerPosition;
import net.elytrium.limboapi.protocol.packet.PlayerPositionAndLook;
import net.elytrium.limboapi.protocol.packet.SetExp;
import net.elytrium.limboapi.protocol.packet.SetSlot;
import net.elytrium.limboapi.protocol.packet.TeleportConfirm;
import net.elytrium.limboapi.protocol.packet.UpdateViewPosition;
import net.elytrium.limboapi.protocol.packet.world.ChunkData;
import org.checkerframework.checker.nullness.qual.Nullable;

public class VirtualProtocol {

  public final static PacketRegistry clientbound = new PacketRegistry(ProtocolUtils.Direction.CLIENTBOUND);
  public final static PacketRegistry serverbound = new PacketRegistry(ProtocolUtils.Direction.SERVERBOUND);

  public static void init() {
    serverbound.register(
        TeleportConfirm.class, TeleportConfirm::new,
        map(0x00, ProtocolVersion.MINECRAFT_1_9, true));
    serverbound.register(
        PlayerPositionAndLook.class, PlayerPositionAndLook::new,
        map(0x06, ProtocolVersion.MINECRAFT_1_7_2, false),
        map(0x06, ProtocolVersion.MINECRAFT_1_8, false),
        map(0x0D, ProtocolVersion.MINECRAFT_1_9, false),
        map(0x0F, ProtocolVersion.MINECRAFT_1_12, false),
        map(0x0E, ProtocolVersion.MINECRAFT_1_12_1, false),
        map(0x11, ProtocolVersion.MINECRAFT_1_13, false),
        map(0x12, ProtocolVersion.MINECRAFT_1_14, false),
        map(0x13, ProtocolVersion.MINECRAFT_1_16, false),
        map(0x12, ProtocolVersion.MINECRAFT_1_17, false));
    serverbound.register(
        PlayerPosition.class, PlayerPosition::new,
        map(0x0B, ProtocolVersion.MINECRAFT_1_7_2, false),
        map(0x04, ProtocolVersion.MINECRAFT_1_7_6, false),
        map(0x04, ProtocolVersion.MINECRAFT_1_8, false),
        map(0x0C, ProtocolVersion.MINECRAFT_1_9, false),
        map(0x0E, ProtocolVersion.MINECRAFT_1_12, false),
        map(0x0D, ProtocolVersion.MINECRAFT_1_12_1, false),
        map(0x10, ProtocolVersion.MINECRAFT_1_13, false),
        map(0x11, ProtocolVersion.MINECRAFT_1_14, false),
        map(0x12, ProtocolVersion.MINECRAFT_1_16, false),
        map(0x11, ProtocolVersion.MINECRAFT_1_17, false));
    serverbound.register(
        Player.class, Player::new,
        map(0x03, ProtocolVersion.MINECRAFT_1_7_2, false),
        map(0x03, ProtocolVersion.MINECRAFT_1_8, false),
        map(0x0F, ProtocolVersion.MINECRAFT_1_9, false),
        map(0x0D, ProtocolVersion.MINECRAFT_1_12, false),
        map(0x0C, ProtocolVersion.MINECRAFT_1_12_1, false),
        map(0x0F, ProtocolVersion.MINECRAFT_1_13, false),
        map(0x14, ProtocolVersion.MINECRAFT_1_14, false),
        map(0x15, ProtocolVersion.MINECRAFT_1_16, false),
        map(0x14, ProtocolVersion.MINECRAFT_1_17, false));
    clientbound.register(
        PlayerPositionAndLook.class, PlayerPositionAndLook::new,
        map(0x08, ProtocolVersion.MINECRAFT_1_7_2, true),
        map(0x2E, ProtocolVersion.MINECRAFT_1_9, true),
        map(0x2F, ProtocolVersion.MINECRAFT_1_12_1, true),
        map(0x32, ProtocolVersion.MINECRAFT_1_13, true),
        map(0x35, ProtocolVersion.MINECRAFT_1_14, true),
        map(0x36, ProtocolVersion.MINECRAFT_1_15, true),
        map(0x35, ProtocolVersion.MINECRAFT_1_16, true),
        map(0x34, ProtocolVersion.MINECRAFT_1_16_2, true),
        map(0x38, ProtocolVersion.MINECRAFT_1_17, true));
    clientbound.register(
        ChunkData.class, ChunkData::new,
        map(0x21, ProtocolVersion.MINECRAFT_1_7_2, true),
        map(0x21, ProtocolVersion.MINECRAFT_1_8, true),
        map(0x20, ProtocolVersion.MINECRAFT_1_9, true),
        map(0x22, ProtocolVersion.MINECRAFT_1_13, true),
        map(0x21, ProtocolVersion.MINECRAFT_1_14, true),
        map(0x22, ProtocolVersion.MINECRAFT_1_15, true),
        map(0x21, ProtocolVersion.MINECRAFT_1_16, true),
        map(0x20, ProtocolVersion.MINECRAFT_1_16_2, true),
        map(0x22, ProtocolVersion.MINECRAFT_1_17, true));
    clientbound.register(
        SetSlot.class, SetSlot::new,
        map(0x2F, ProtocolVersion.MINECRAFT_1_7_2, true),
        map(0x2F, ProtocolVersion.MINECRAFT_1_8, true),
        map(0x16, ProtocolVersion.MINECRAFT_1_9, true),
        map(0x17, ProtocolVersion.MINECRAFT_1_13, true),
        map(0x16, ProtocolVersion.MINECRAFT_1_14, true),
        map(0x17, ProtocolVersion.MINECRAFT_1_15, true),
        map(0x16, ProtocolVersion.MINECRAFT_1_16, true),
        map(0x15, ProtocolVersion.MINECRAFT_1_16_2, true),
        map(0x16, ProtocolVersion.MINECRAFT_1_17, true));
    clientbound.register(
        MapDataPacket.class, MapDataPacket::new,
        map(0x34, ProtocolVersion.MINECRAFT_1_7_2, true),
        map(0x34, ProtocolVersion.MINECRAFT_1_8, true),
        map(0x24, ProtocolVersion.MINECRAFT_1_9, true),
        map(0x26, ProtocolVersion.MINECRAFT_1_13, true),
        map(0x26, ProtocolVersion.MINECRAFT_1_14, true),
        map(0x27, ProtocolVersion.MINECRAFT_1_15, true),
        map(0x26, ProtocolVersion.MINECRAFT_1_16, true),
        map(0x25, ProtocolVersion.MINECRAFT_1_16_2, true),
        map(0x27, ProtocolVersion.MINECRAFT_1_17, true));
    clientbound.register(
        PlayerAbilities.class, PlayerAbilities::new,
        map(0x39, ProtocolVersion.MINECRAFT_1_7_2, true),
        map(0x39, ProtocolVersion.MINECRAFT_1_8, true),
        map(0x2B, ProtocolVersion.MINECRAFT_1_9, true),
        map(0x2C, ProtocolVersion.MINECRAFT_1_12_1, true),
        map(0x2E, ProtocolVersion.MINECRAFT_1_13, true),
        map(0x31, ProtocolVersion.MINECRAFT_1_14, true),
        map(0x32, ProtocolVersion.MINECRAFT_1_15, true),
        map(0x31, ProtocolVersion.MINECRAFT_1_16, true),
        map(0x30, ProtocolVersion.MINECRAFT_1_16_2, true),
        map(0x32, ProtocolVersion.MINECRAFT_1_17, true));
    clientbound.register(
        SetExp.class, SetExp::new,
        map(0x2B, ProtocolVersion.MINECRAFT_1_7_2, true),
        map(0x1F, ProtocolVersion.MINECRAFT_1_8, true),
        map(0x3D, ProtocolVersion.MINECRAFT_1_9, true),
        map(0x3F, ProtocolVersion.MINECRAFT_1_12, true),
        map(0x40, ProtocolVersion.MINECRAFT_1_12_1, true),
        map(0x43, ProtocolVersion.MINECRAFT_1_13, true),
        map(0x47, ProtocolVersion.MINECRAFT_1_14, true),
        map(0x48, ProtocolVersion.MINECRAFT_1_15, true),
        map(0x51, ProtocolVersion.MINECRAFT_1_17, true));
    clientbound.register(
        UpdateViewPosition.class, UpdateViewPosition::new,
        map(0x40, ProtocolVersion.MINECRAFT_1_7_2, true),
        map(0x40, ProtocolVersion.MINECRAFT_1_14, true),
        map(0x41, ProtocolVersion.MINECRAFT_1_15, true),
        map(0x40, ProtocolVersion.MINECRAFT_1_16_1, true),
        map(0x49, ProtocolVersion.MINECRAFT_1_17, true));

    serverbound.register(
        Chat.class, Chat::new,
        getMappingsForPacket(StateRegistry.PLAY.serverbound, Chat.class, false));
    serverbound.register(
        ClientSettings.class, ClientSettings::new,
        getMappingsForPacket(StateRegistry.PLAY.serverbound, ClientSettings.class, false));
    serverbound.register(
        PluginMessage.class, PluginMessage::new,
        getMappingsForPacket(StateRegistry.PLAY.serverbound, PluginMessage.class, false));
    clientbound.register(
        Chat.class, Chat::new,
        getMappingsForPacket(StateRegistry.PLAY.clientbound, Chat.class, true));
    clientbound.register(
        JoinGame.class, JoinGame::new,
        getMappingsForPacket(StateRegistry.PLAY.clientbound, JoinGame.class, true));
    clientbound.register(
        Disconnect.class, Disconnect::new,
        getMappingsForPacket(StateRegistry.PLAY.clientbound, Disconnect.class, true));
    clientbound.register(
        PluginMessage.class, PluginMessage::new,
        getMappingsForPacket(StateRegistry.PLAY.clientbound, PluginMessage.class, true));
    clientbound.register(
        Respawn.class, Respawn::new,
        getMappingsForPacket(StateRegistry.PLAY.clientbound, Respawn.class, true));
    clientbound.register(
        TitleActionbarPacket.class, TitleActionbarPacket::new,
        getMappingsForPacket(StateRegistry.PLAY.clientbound, ProtocolVersion.MINECRAFT_1_17, TitleActionbarPacket.class, true));
    clientbound.register(
        TitleClearPacket.class, TitleClearPacket::new,
        getMappingsForPacket(StateRegistry.PLAY.clientbound, ProtocolVersion.MINECRAFT_1_17, TitleClearPacket.class, true));
    clientbound.register(
        TitleSubtitlePacket.class, TitleSubtitlePacket::new,
        getMappingsForPacket(StateRegistry.PLAY.clientbound, ProtocolVersion.MINECRAFT_1_17, TitleSubtitlePacket.class, true));
    clientbound.register(
        TitleTextPacket.class, TitleTextPacket::new,
        getMappingsForPacket(StateRegistry.PLAY.clientbound, ProtocolVersion.MINECRAFT_1_17, TitleTextPacket.class, true));
    clientbound.register(
        TitleTimesPacket.class, TitleTimesPacket::new,
        getMappingsForPacket(StateRegistry.PLAY.clientbound, ProtocolVersion.MINECRAFT_1_17, TitleTimesPacket.class, true));
    clientbound.register(
        LegacyTitlePacket.class, LegacyTitlePacket::new,
        getMappingsForPacket(StateRegistry.PLAY.clientbound,
            ProtocolVersion.MINECRAFT_1_8, ProtocolVersion.MINECRAFT_1_16_4, LegacyTitlePacket.class, true));
  }

  // Ported from Velocity
  public static class PacketRegistry {

    private final ProtocolUtils.Direction direction;
    private final Map<ProtocolVersion, PacketRegistry.ProtocolRegistry> versions;
    private boolean fallback = true;

    PacketRegistry(ProtocolUtils.Direction direction) {
      this.direction = direction;

      Map<ProtocolVersion, PacketRegistry.ProtocolRegistry> mutableVersions = new EnumMap<>(ProtocolVersion.class);
      for (ProtocolVersion version : ProtocolVersion.values()) {
        if (!version.isLegacy() && !version.isUnknown()) {
          mutableVersions.put(version, new PacketRegistry.ProtocolRegistry(version));
        }
      }

      this.versions = Collections.unmodifiableMap(mutableVersions);
    }

    public PacketRegistry.ProtocolRegistry getProtocolRegistry(final ProtocolVersion version) {
      PacketRegistry.ProtocolRegistry registry = versions.get(version);
      if (registry == null) {
        if (fallback) {
          return getProtocolRegistry(MINIMUM_VERSION);
        }
        throw new IllegalArgumentException("Could not find data for protocol version " + version);
      }
      return registry;
    }

    @SneakyThrows
    public <P extends MinecraftPacket> void register(Class<P> clazz, Supplier<P> packetSupplier,
                                                     StateRegistry.PacketMapping... mappings) {
      if (mappings.length == 0) {
        throw new IllegalArgumentException("At least one mapping must be provided.");
      }

      for (int i = 0; i < mappings.length; i++) {
        StateRegistry.PacketMapping current = mappings[i];
        StateRegistry.PacketMapping next = (i + 1 < mappings.length) ? mappings[i + 1] : current;

        // LimboAPI start
        Class<?> packetMappingClass = StateRegistry.PacketMapping.class;
        Field protocolVersion = packetMappingClass.getDeclaredField("protocolVersion");
        Field lastValidProtocolVersion = packetMappingClass.getDeclaredField("lastValidProtocolVersion");
        Field id = packetMappingClass.getDeclaredField("id");
        Field encodeOnly = packetMappingClass.getDeclaredField("id");

        protocolVersion.setAccessible(true);
        lastValidProtocolVersion.setAccessible(true);
        id.setAccessible(true);
        encodeOnly.setAccessible(true);
        // LimboAPI end

        ProtocolVersion from = (ProtocolVersion) protocolVersion.get(current);
        ProtocolVersion lastValid = (ProtocolVersion) lastValidProtocolVersion.get(current);
        if (lastValid != null) {
          if (next != current) {
            throw new IllegalArgumentException("Cannot add a mapping after last valid mapping");
          }
          if (from.compareTo(lastValid) > 0) {
            throw new IllegalArgumentException(
                "Last mapping version cannot be higher than highest mapping version");
          }
        }
        ProtocolVersion to = current == next ? lastValid != null
            ? lastValid : getLast(SUPPORTED_VERSIONS) : (ProtocolVersion) protocolVersion.get(next);

        if (from.compareTo(to) >= 0 && from != getLast(SUPPORTED_VERSIONS)) {
          throw new IllegalArgumentException(String.format(
              "Next mapping version (%s) should be lower then current (%s)", to, from));
        }

        for (ProtocolVersion protocol : EnumSet.range(from, to)) {
          if (protocol == to && next != current) {
            break;
          }
          PacketRegistry.ProtocolRegistry registry = this.versions.get(protocol);
          if (registry == null) {
            throw new IllegalArgumentException("Unknown protocol version "
                + protocolVersion.get(current));
          }

          if (registry.packetIdToSupplier.containsKey((int) id.get(current))) {
            throw new IllegalArgumentException("Can not register class " + clazz.getSimpleName()
                + " with id " + id.get(current) + " for " + registry.version
                + " because another packet is already registered");
          }

          if (registry.packetClassToId.containsKey(clazz)) {
            throw new IllegalArgumentException(clazz.getSimpleName()
                + " is already registered for version " + registry.version);
          }

          if (!(boolean) encodeOnly.get(current)) {
            registry.packetIdToSupplier.put((Integer) id.get(current), packetSupplier);
          }
          registry.packetClassToId.put(clazz, (Integer) id.get(current));
        }
      }
    }

    public class ProtocolRegistry {

      public final ProtocolVersion version;
      final IntObjectMap<Supplier<? extends MinecraftPacket>> packetIdToSupplier =
          new IntObjectHashMap<>(16, 0.5f);
      final Object2IntMap<Class<? extends MinecraftPacket>> packetClassToId =
          new Object2IntOpenHashMap<>(16, 0.5f);

      ProtocolRegistry(final ProtocolVersion version) {
        this.version = version;
        this.packetClassToId.defaultReturnValue(Integer.MIN_VALUE);
      }

      /**
       * Attempts to create a packet from the specified {@code id}.
       *
       * @param id the packet ID
       * @return the packet instance, or {@code null} if the ID is not registered
       */
      public @Nullable MinecraftPacket createPacket(final int id) {
        final Supplier<? extends MinecraftPacket> supplier = this.packetIdToSupplier.get(id);
        if (supplier == null) {
          return null;
        }
        return supplier.get();
      }

      /**
       * Attempts to look up the packet ID for an {@code packet}.
       *
       * @param packet the packet to look up
       * @return the packet ID
       * @throws IllegalArgumentException if the packet ID is not found
       */
      public int getPacketId(final MinecraftPacket packet) {
        final int id = this.packetClassToId.getInt(packet.getClass());
        if (id == Integer.MIN_VALUE) {
          throw new IllegalArgumentException(String.format(
              "Unable to find id for packet of type %s in %s protocol %s",
              packet.getClass().getName(), PacketRegistry.this.direction, this.version
          ));
        }
        return id;
      }

      /**
       * Attempts to look up the packet ID for a {@code packet} class.
       *
       * @param clazz the packet class to look up
       * @return the packet ID
       * @throws IllegalArgumentException if the packet ID is not found
       */
      public int getPacketId(final Class<? extends MinecraftPacket> clazz) {
        final int id = this.packetClassToId.getInt(clazz);
        if (id == Integer.MIN_VALUE) {
          throw new IllegalArgumentException(String.format(
              "Unable to find id for packet of type %s in %s protocol %s",
              clazz.getName(), PacketRegistry.this.direction, this.version
          ));
        }
        return id;
      }
    }
  }

  public static StateRegistry.PacketMapping map(int id, ProtocolVersion version, boolean encodeOnly) {
    return map(id, version, null, encodeOnly);
  }

  /**
   * Creates a PacketMapping using the provided arguments.
   *
   * @param id         Packet Id
   * @param version    Protocol version
   * @param encodeOnly When true packet decoding will be disabled
   * @param lastValidProtocolVersion Last version this Mapping is valid at
   * @return PacketMapping with the provided arguments
   */
  @SneakyThrows
  public static StateRegistry.PacketMapping map(int id, ProtocolVersion version,
                                                ProtocolVersion lastValidProtocolVersion, boolean encodeOnly) {
    Constructor<StateRegistry.PacketMapping> constructor =
        StateRegistry.PacketMapping.class.getDeclaredConstructor(int.class, ProtocolVersion.class, ProtocolVersion.class, boolean.class);
    constructor.setAccessible(true);
    return constructor.newInstance(id, version, lastValidProtocolVersion, encodeOnly);
  }

  public static StateRegistry.PacketMapping[] getMappingsForPacket(StateRegistry.PacketRegistry registry, Class<? extends MinecraftPacket> packet, boolean encodeOnly) {
    return getMappingsForPacket(registry, ProtocolVersion.MINIMUM_VERSION, ProtocolVersion.MAXIMUM_VERSION, packet, encodeOnly);
  }

  public static StateRegistry.PacketMapping[] getMappingsForPacket(
      StateRegistry.PacketRegistry registry, ProtocolVersion from, Class<? extends MinecraftPacket> packet, boolean encodeOnly) {

    return getMappingsForPacket(registry, from, ProtocolVersion.MAXIMUM_VERSION, packet, encodeOnly);
  }

  @SneakyThrows
  public static StateRegistry.PacketMapping[] getMappingsForPacket(
      StateRegistry.PacketRegistry registry, ProtocolVersion from, ProtocolVersion to, Class<? extends MinecraftPacket> packet, boolean encodeOnly) {

    List<StateRegistry.PacketMapping> mappings = new ArrayList<>();
    for (ProtocolVersion protocol : EnumSet.range(from, to)) {
      Method getProtocolRegistry = StateRegistry.PacketRegistry.class.getDeclaredMethod("getProtocolRegistry", ProtocolVersion.class);
      getProtocolRegistry.setAccessible(true);
      PacketRegistry.ProtocolRegistry protocolRegistry = (PacketRegistry.ProtocolRegistry) getProtocolRegistry.invoke(registry, protocol);
      int id = protocolRegistry.getPacketId(packet);
      Constructor<StateRegistry.PacketMapping> constructor =
          StateRegistry.PacketMapping.class.getDeclaredConstructor(int.class, ProtocolVersion.class, ProtocolVersion.class, boolean.class);
      constructor.setAccessible(true);
      mappings.add(constructor.newInstance(id, protocol, null, encodeOnly));
    }
    return mappings.toArray(new StateRegistry.PacketMapping[0]);
  }
}
