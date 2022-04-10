/*
 * Copyright (C) 2021 - 2022 Elytrium
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

package net.elytrium.limboapi.protocol;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.StateRegistry;
import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;
import net.elytrium.java.commons.reflection.ReflectionException;
import net.elytrium.limboapi.api.protocol.PacketDirection;
import net.elytrium.limboapi.api.protocol.packets.PacketMapping;
import net.elytrium.limboapi.protocol.packet.ChangeGameState;
import net.elytrium.limboapi.protocol.packet.DefaultSpawnPosition;
import net.elytrium.limboapi.protocol.packet.MapDataPacket;
import net.elytrium.limboapi.protocol.packet.Player;
import net.elytrium.limboapi.protocol.packet.PlayerAbilities;
import net.elytrium.limboapi.protocol.packet.PlayerPosition;
import net.elytrium.limboapi.protocol.packet.PlayerPositionAndLook;
import net.elytrium.limboapi.protocol.packet.SetExperience;
import net.elytrium.limboapi.protocol.packet.SetSlot;
import net.elytrium.limboapi.protocol.packet.TeleportConfirm;
import net.elytrium.limboapi.protocol.packet.UpdateViewPosition;
import net.elytrium.limboapi.protocol.packet.world.ChunkData;
import net.elytrium.limboapi.utils.OverlayIntObjectMap;
import net.elytrium.limboapi.utils.OverlayObject2IntMap;
import sun.misc.Unsafe;

@SuppressWarnings("unchecked")
public class LimboProtocol {

  public static final String PREPARED_ENCODER = "prepared-encoder";
  public static final String READ_TIMEOUT = "limboapi-read-timeout";

  private static final Unsafe unsafe;
  private static final StateRegistry limboRegistry;
  private static final Field direction;
  private static final Field versions;
  private static final Field fallback;
  private static final Field version;
  private static final Field packetClassToId;
  private static final Field packetIdToSupplier;
  private static final Method getProtocolRegistry;
  private static final Method register;
  private static final Constructor<StateRegistry.PacketMapping> ctor;

  static {
    try {
      Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
      unsafeField.setAccessible(true);
      unsafe = (Unsafe) unsafeField.get(null);

      limboRegistry = (StateRegistry) unsafe.allocateInstance(StateRegistry.class);

      direction = StateRegistry.PacketRegistry.class.getDeclaredField("direction");
      direction.setAccessible(true);

      versions = StateRegistry.PacketRegistry.class.getDeclaredField("versions");
      versions.setAccessible(true);

      fallback = StateRegistry.PacketRegistry.class.getDeclaredField("fallback");
      fallback.setAccessible(true);

      version = StateRegistry.PacketRegistry.ProtocolRegistry.class.getDeclaredField("version");
      version.setAccessible(true);

      packetClassToId = StateRegistry.PacketRegistry.ProtocolRegistry.class.getDeclaredField("packetClassToId");
      packetClassToId.setAccessible(true);

      packetIdToSupplier = StateRegistry.PacketRegistry.ProtocolRegistry.class.getDeclaredField("packetIdToSupplier");
      packetIdToSupplier.setAccessible(true);

      overlayRegistry(StateRegistry.class.getDeclaredField("clientbound"));
      overlayRegistry(StateRegistry.class.getDeclaredField("serverbound"));

      getProtocolRegistry = StateRegistry.PacketRegistry.class.getDeclaredMethod("getProtocolRegistry", ProtocolVersion.class);
      getProtocolRegistry.setAccessible(true);

      register = StateRegistry.PacketRegistry.class.getDeclaredMethod("register", Class.class, Supplier.class, StateRegistry.PacketMapping[].class);
      register.setAccessible(true);

      ctor = StateRegistry.PacketMapping.class.getDeclaredConstructor(int.class, ProtocolVersion.class, ProtocolVersion.class, boolean.class);
      ctor.setAccessible(true);
    } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | NoSuchFieldException e) {
      throw new ReflectionException(e);
    }
  }

  public static void init() throws InvocationTargetException, InstantiationException, IllegalAccessException {
    register(PacketDirection.SERVERBOUND,
        TeleportConfirm.class, TeleportConfirm::new,
        new StateRegistry.PacketMapping[] {
            map(0x00, ProtocolVersion.MINECRAFT_1_9, false)
        });
    register(PacketDirection.SERVERBOUND,
        PlayerPositionAndLook.class, PlayerPositionAndLook::new,
        new StateRegistry.PacketMapping[] {
            map(0x06, ProtocolVersion.MINECRAFT_1_7_2, false),
            map(0x0D, ProtocolVersion.MINECRAFT_1_9, false),
            map(0x0F, ProtocolVersion.MINECRAFT_1_12, false),
            map(0x0E, ProtocolVersion.MINECRAFT_1_12_1, false),
            map(0x11, ProtocolVersion.MINECRAFT_1_13, false),
            map(0x12, ProtocolVersion.MINECRAFT_1_14, false),
            map(0x13, ProtocolVersion.MINECRAFT_1_16, false),
            map(0x12, ProtocolVersion.MINECRAFT_1_17, false)
        });
    register(PacketDirection.SERVERBOUND,
        PlayerPosition.class, PlayerPosition::new,
        new StateRegistry.PacketMapping[] {
            map(0x04, ProtocolVersion.MINECRAFT_1_7_2, false),
            map(0x0C, ProtocolVersion.MINECRAFT_1_9, false),
            map(0x0E, ProtocolVersion.MINECRAFT_1_12, false),
            map(0x0D, ProtocolVersion.MINECRAFT_1_12_1, false),
            map(0x10, ProtocolVersion.MINECRAFT_1_13, false),
            map(0x11, ProtocolVersion.MINECRAFT_1_14, false),
            map(0x12, ProtocolVersion.MINECRAFT_1_16, false),
            map(0x11, ProtocolVersion.MINECRAFT_1_17, false)
        });
    register(PacketDirection.SERVERBOUND,
        Player.class, Player::new,
        new StateRegistry.PacketMapping[] {
            map(0x03, ProtocolVersion.MINECRAFT_1_7_2, false),
            map(0x0F, ProtocolVersion.MINECRAFT_1_9, false),
            map(0x0D, ProtocolVersion.MINECRAFT_1_12, false),
            map(0x0C, ProtocolVersion.MINECRAFT_1_12_1, false),
            map(0x0F, ProtocolVersion.MINECRAFT_1_13, false),
            map(0x14, ProtocolVersion.MINECRAFT_1_14, false),
            map(0x15, ProtocolVersion.MINECRAFT_1_16, false),
            map(0x14, ProtocolVersion.MINECRAFT_1_17, false)
        });

    register(PacketDirection.CLIENTBOUND,
        PlayerPositionAndLook.class, PlayerPositionAndLook::new,
        new StateRegistry.PacketMapping[] {
            map(0x08, ProtocolVersion.MINECRAFT_1_7_2, true),
            map(0x2E, ProtocolVersion.MINECRAFT_1_9, true),
            map(0x2F, ProtocolVersion.MINECRAFT_1_12_1, true),
            map(0x32, ProtocolVersion.MINECRAFT_1_13, true),
            map(0x35, ProtocolVersion.MINECRAFT_1_14, true),
            map(0x36, ProtocolVersion.MINECRAFT_1_15, true),
            map(0x35, ProtocolVersion.MINECRAFT_1_16, true),
            map(0x34, ProtocolVersion.MINECRAFT_1_16_2, true),
            map(0x38, ProtocolVersion.MINECRAFT_1_17, true)
        });
    register(PacketDirection.CLIENTBOUND,
        ChunkData.class, ChunkData::new,
        new StateRegistry.PacketMapping[] {
            map(0x21, ProtocolVersion.MINECRAFT_1_7_2, true),
            map(0x20, ProtocolVersion.MINECRAFT_1_9, true),
            map(0x22, ProtocolVersion.MINECRAFT_1_13, true),
            map(0x21, ProtocolVersion.MINECRAFT_1_14, true),
            map(0x22, ProtocolVersion.MINECRAFT_1_15, true),
            map(0x21, ProtocolVersion.MINECRAFT_1_16, true),
            map(0x20, ProtocolVersion.MINECRAFT_1_16_2, true),
            map(0x22, ProtocolVersion.MINECRAFT_1_17, true)
        });
    register(PacketDirection.CLIENTBOUND,
        SetSlot.class, SetSlot::new,
        new StateRegistry.PacketMapping[] {
            map(0x2F, ProtocolVersion.MINECRAFT_1_7_2, true),
            map(0x16, ProtocolVersion.MINECRAFT_1_9, true),
            map(0x17, ProtocolVersion.MINECRAFT_1_13, true),
            map(0x16, ProtocolVersion.MINECRAFT_1_14, true),
            map(0x17, ProtocolVersion.MINECRAFT_1_15, true),
            map(0x16, ProtocolVersion.MINECRAFT_1_16, true),
            map(0x15, ProtocolVersion.MINECRAFT_1_16_2, true),
            map(0x16, ProtocolVersion.MINECRAFT_1_17, true)
        });
    register(PacketDirection.CLIENTBOUND,
        MapDataPacket.class, MapDataPacket::new,
        new StateRegistry.PacketMapping[] {
            map(0x34, ProtocolVersion.MINECRAFT_1_7_2, true),
            map(0x24, ProtocolVersion.MINECRAFT_1_9, true),
            map(0x26, ProtocolVersion.MINECRAFT_1_13, true),
            map(0x27, ProtocolVersion.MINECRAFT_1_15, true),
            map(0x26, ProtocolVersion.MINECRAFT_1_16, true),
            map(0x25, ProtocolVersion.MINECRAFT_1_16_2, true),
            map(0x27, ProtocolVersion.MINECRAFT_1_17, true)
        });
    register(PacketDirection.CLIENTBOUND,
        PlayerAbilities.class, PlayerAbilities::new,
        new StateRegistry.PacketMapping[] {
            map(0x39, ProtocolVersion.MINECRAFT_1_7_2, true),
            map(0x2B, ProtocolVersion.MINECRAFT_1_9, true),
            map(0x2C, ProtocolVersion.MINECRAFT_1_12_1, true),
            map(0x2E, ProtocolVersion.MINECRAFT_1_13, true),
            map(0x31, ProtocolVersion.MINECRAFT_1_14, true),
            map(0x32, ProtocolVersion.MINECRAFT_1_15, true),
            map(0x31, ProtocolVersion.MINECRAFT_1_16, true),
            map(0x30, ProtocolVersion.MINECRAFT_1_16_2, true),
            map(0x32, ProtocolVersion.MINECRAFT_1_17, true)
        });
    register(PacketDirection.CLIENTBOUND,
        SetExperience.class, SetExperience::new,
        new StateRegistry.PacketMapping[] {
            map(0x1F, ProtocolVersion.MINECRAFT_1_7_2, true),
            map(0x3D, ProtocolVersion.MINECRAFT_1_9, true),
            map(0x3F, ProtocolVersion.MINECRAFT_1_12, true),
            map(0x40, ProtocolVersion.MINECRAFT_1_12_1, true),
            map(0x43, ProtocolVersion.MINECRAFT_1_13, true),
            map(0x47, ProtocolVersion.MINECRAFT_1_14, true),
            map(0x48, ProtocolVersion.MINECRAFT_1_15, true),
            map(0x51, ProtocolVersion.MINECRAFT_1_17, true)
        });
    register(PacketDirection.CLIENTBOUND,
        UpdateViewPosition.class, UpdateViewPosition::new, // ViewCentre, ChunkRenderDistanceCenter
        new StateRegistry.PacketMapping[] {
            map(0x40, ProtocolVersion.MINECRAFT_1_14, true),
            map(0x41, ProtocolVersion.MINECRAFT_1_15, true),
            map(0x40, ProtocolVersion.MINECRAFT_1_16, true),
            map(0x49, ProtocolVersion.MINECRAFT_1_17, true)
        });
    register(PacketDirection.CLIENTBOUND,
        ChangeGameState.class, ChangeGameState::new,
        new StateRegistry.PacketMapping[] {
            map(0x2B, ProtocolVersion.MINECRAFT_1_7_2, true),
            map(0x1E, ProtocolVersion.MINECRAFT_1_9, true),
            map(0x20, ProtocolVersion.MINECRAFT_1_13, true),
            map(0x1E, ProtocolVersion.MINECRAFT_1_14, true),
            map(0x1F, ProtocolVersion.MINECRAFT_1_15, true),
            map(0x1E, ProtocolVersion.MINECRAFT_1_16, true),
            map(0x1D, ProtocolVersion.MINECRAFT_1_16_2, true),
            map(0x1E, ProtocolVersion.MINECRAFT_1_17, true)
        });
    register(PacketDirection.CLIENTBOUND,
        DefaultSpawnPosition.class, DefaultSpawnPosition::new,
        new StateRegistry.PacketMapping[] {
            map(0x05, ProtocolVersion.MINECRAFT_1_7_2, true),
            map(0x43, ProtocolVersion.MINECRAFT_1_9, true),
            map(0x45, ProtocolVersion.MINECRAFT_1_12, true),
            map(0x46, ProtocolVersion.MINECRAFT_1_12_1, true),
            map(0x49, ProtocolVersion.MINECRAFT_1_13, true),
            map(0x4D, ProtocolVersion.MINECRAFT_1_14, true),
            map(0x4E, ProtocolVersion.MINECRAFT_1_15, true),
            map(0x42, ProtocolVersion.MINECRAFT_1_16, true),
            map(0x4B, ProtocolVersion.MINECRAFT_1_17, true)
        });
  }

  public static void register(PacketDirection direction, Class<?> packetClass, Supplier<?> packetSupplier, StateRegistry.PacketMapping[] packetMappings) {
    StateRegistry.PacketRegistry registry;
    switch (direction) {
      case CLIENTBOUND: {
        registry = limboRegistry.clientbound;
        break;
      }
      case SERVERBOUND: {
        registry = limboRegistry.serverbound;
        break;
      }
      default: {
        throw new IllegalStateException("Unexpected value: " + direction);
      }
    }

    try {
      register.invoke(registry, packetClass, packetSupplier, packetMappings);
    } catch (IllegalAccessException | InvocationTargetException e) {
      e.printStackTrace();
    }
  }

  public static void register(PacketDirection direction, Class<?> packetClass, Supplier<?> packetSupplier, PacketMapping[] packetMappings) {
    register(direction, packetClass, packetSupplier, Arrays.stream(packetMappings).map(packetMapping -> {
      try {
        return map(packetMapping.getId(), packetMapping.getProtocolVersion(), packetMapping.getLastValidProtocolVersion(), packetMapping.isEncodeOnly());
      } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
        throw new ReflectionException(e);
      }
    }).toArray(StateRegistry.PacketMapping[]::new));
  }

  private static StateRegistry.PacketMapping map(int id, ProtocolVersion version, boolean encodeOnly)
      throws InvocationTargetException, InstantiationException, IllegalAccessException {
    return map(id, version, null, encodeOnly);
  }

  private static StateRegistry.PacketMapping map(int id, ProtocolVersion version, ProtocolVersion lastValidProtocolVersion, boolean encodeOnly)
      throws InvocationTargetException, InstantiationException, IllegalAccessException {
    return ctor.newInstance(id, version, lastValidProtocolVersion, encodeOnly);
  }

  private static void overlayRegistry(Field to) throws IllegalAccessException, InstantiationException {
    StateRegistry.PacketRegistry from = (StateRegistry.PacketRegistry) to.get(StateRegistry.PLAY);
    ProtocolUtils.Direction fromDirection = (ProtocolUtils.Direction) direction.get(from);
    Map<ProtocolVersion, StateRegistry.PacketRegistry.ProtocolRegistry> fromVersions =
        (Map<ProtocolVersion, StateRegistry.PacketRegistry.ProtocolRegistry>) versions.get(from);

    StateRegistry.PacketRegistry toSet = (StateRegistry.PacketRegistry) unsafe.allocateInstance(StateRegistry.PacketRegistry.class);
    direction.set(toSet, fromDirection);

    Map<ProtocolVersion, StateRegistry.PacketRegistry.ProtocolRegistry> toVersions = new EnumMap<>(ProtocolVersion.class);
    fromVersions.forEach((fromVersion, fromRegistry) -> {
      try {
        IntObjectMap<Supplier<? extends MinecraftPacket>> fromPacketIdToSupplier =
            (IntObjectMap<Supplier<? extends MinecraftPacket>>) packetIdToSupplier.get(fromRegistry);
        Object2IntMap<Class<? extends MinecraftPacket>> fromPacketClassToId =
            (Object2IntMap<Class<? extends MinecraftPacket>>) packetClassToId.get(fromRegistry);

        StateRegistry.PacketRegistry.ProtocolRegistry toRegistry =
            (StateRegistry.PacketRegistry.ProtocolRegistry) unsafe.allocateInstance(StateRegistry.PacketRegistry.ProtocolRegistry.class);

        packetIdToSupplier.set(toRegistry, new OverlayIntObjectMap<>(fromPacketIdToSupplier, new IntObjectHashMap<>(16, 0.5F)));
        packetClassToId.set(toRegistry, new OverlayObject2IntMap<>(fromPacketClassToId, new Object2IntOpenHashMap<>(16, 0.5F)));

        version.set(toRegistry, fromVersion);
        toVersions.put(fromVersion, toRegistry);
      } catch (IllegalAccessException | InstantiationException e) {
        e.printStackTrace();
      }
    });

    versions.set(toSet, toVersions);
    fallback.set(toSet, true);

    to.setAccessible(true);
    to.set(limboRegistry, toSet);
  }

  public static int getPacketId(StateRegistry.PacketRegistry packetRegistry, Class<? extends MinecraftPacket> packet, ProtocolVersion version) {
    StateRegistry.PacketRegistry.ProtocolRegistry protocolRegistry = null;
    try {
      protocolRegistry = (StateRegistry.PacketRegistry.ProtocolRegistry) getProtocolRegistry.invoke(packetRegistry, version);
    } catch (IllegalAccessException | InvocationTargetException e) {
      e.printStackTrace();
    }

    return getPacketId(protocolRegistry, packet);
  }

  public static int getPacketId(StateRegistry.PacketRegistry.ProtocolRegistry registry, Class<? extends MinecraftPacket> packet) {
    Object2IntMap<Class<? extends MinecraftPacket>> map;
    try {
      map = (Object2IntMap<Class<? extends MinecraftPacket>>) packetClassToId.get(registry);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }

    return map.getInt(packet);
  }

  public static StateRegistry getLimboRegistry() {
    return LimboProtocol.limboRegistry;
  }
}
