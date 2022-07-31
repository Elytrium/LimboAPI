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
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;
import net.elytrium.java.commons.reflection.ReflectionException;
import net.elytrium.limboapi.api.protocol.PacketDirection;
import net.elytrium.limboapi.api.protocol.packets.PacketMapping;
import net.elytrium.limboapi.protocol.packets.c2s.MoveOnGroundOnlyPacket;
import net.elytrium.limboapi.protocol.packets.c2s.MovePacket;
import net.elytrium.limboapi.protocol.packets.c2s.MovePositionOnlyPacket;
import net.elytrium.limboapi.protocol.packets.c2s.MoveRotationOnlyPacket;
import net.elytrium.limboapi.protocol.packets.c2s.TeleportConfirmPacket;
import net.elytrium.limboapi.protocol.packets.s2c.ChangeGameStatePacket;
import net.elytrium.limboapi.protocol.packets.s2c.ChunkDataPacket;
import net.elytrium.limboapi.protocol.packets.s2c.DefaultSpawnPositionPacket;
import net.elytrium.limboapi.protocol.packets.s2c.MapDataPacket;
import net.elytrium.limboapi.protocol.packets.s2c.PlayerAbilitiesPacket;
import net.elytrium.limboapi.protocol.packets.s2c.PositionRotationPacket;
import net.elytrium.limboapi.protocol.packets.s2c.SetExperiencePacket;
import net.elytrium.limboapi.protocol.packets.s2c.SetSlotPacket;
import net.elytrium.limboapi.protocol.packets.s2c.TimeUpdatePacket;
import net.elytrium.limboapi.protocol.packets.s2c.UpdateViewPositionPacket;
import net.elytrium.limboapi.utils.OverlayIntObjectMap;
import net.elytrium.limboapi.utils.OverlayObject2IntMap;
import sun.misc.Unsafe;

@SuppressWarnings("unchecked")
public class LimboProtocol {

  private static final StateRegistry LIMBO_STATE_REGISTRY;
  private static final Method GET_PROTOCOL_REGISTRY_METHOD;
  private static final Method REGISTER_METHOD;
  private static final Constructor<StateRegistry.PacketMapping> PACKET_MAPPING_CONSTRUCTOR;

  public static final String PREPARED_ENCODER = "prepared-encoder";
  public static final String READ_TIMEOUT = "limboapi-read-timeout";
  public static final String DECOMPRESSOR = "limboapi-decompressor";

  public static final Field VERSIONS_FIELD;
  public static final Field PACKET_ID_TO_SUPPLIER_FIELD;
  public static final Field PACKET_CLASS_TO_ID_FIELD;

  static {
    try {
      Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
      unsafeField.setAccessible(true);
      Unsafe unsafe = (Unsafe) unsafeField.get(null);

      LIMBO_STATE_REGISTRY = (StateRegistry) unsafe.allocateInstance(StateRegistry.class);

      VERSIONS_FIELD = StateRegistry.PacketRegistry.class.getDeclaredField("versions");
      VERSIONS_FIELD.setAccessible(true);

      PACKET_ID_TO_SUPPLIER_FIELD = StateRegistry.PacketRegistry.ProtocolRegistry.class.getDeclaredField("packetIdToSupplier");
      PACKET_ID_TO_SUPPLIER_FIELD.setAccessible(true);

      PACKET_CLASS_TO_ID_FIELD = StateRegistry.PacketRegistry.ProtocolRegistry.class.getDeclaredField("packetClassToId");
      PACKET_CLASS_TO_ID_FIELD.setAccessible(true);

      overlayRegistry(unsafe, "clientbound", StateRegistry.PLAY.clientbound);
      overlayRegistry(unsafe, "serverbound", StateRegistry.PLAY.serverbound);

      GET_PROTOCOL_REGISTRY_METHOD = StateRegistry.PacketRegistry.class.getDeclaredMethod("getProtocolRegistry", ProtocolVersion.class);
      GET_PROTOCOL_REGISTRY_METHOD.setAccessible(true);

      REGISTER_METHOD = StateRegistry.PacketRegistry.class.getDeclaredMethod(
          "register",
          Class.class,
          Supplier.class,
          StateRegistry.PacketMapping[].class
      );
      REGISTER_METHOD.setAccessible(true);

      PACKET_MAPPING_CONSTRUCTOR = StateRegistry.PacketMapping.class.getDeclaredConstructor(
          int.class,
          ProtocolVersion.class,
          ProtocolVersion.class,
          boolean.class
      );
      PACKET_MAPPING_CONSTRUCTOR.setAccessible(true);
    } catch (ReflectiveOperationException e) {
      throw new ReflectionException(e);
    }
  }

  private static void overlayRegistry(Unsafe unsafe, String registryName, StateRegistry.PacketRegistry playRegistry) throws ReflectiveOperationException {
    StateRegistry.PacketRegistry registry = (StateRegistry.PacketRegistry) unsafe.allocateInstance(StateRegistry.PacketRegistry.class);

    Field directionField = StateRegistry.PacketRegistry.class.getDeclaredField("direction");
    directionField.setAccessible(true);
    directionField.set(registry, ProtocolUtils.Direction.valueOf(registryName.toUpperCase()));

    Field versionField = StateRegistry.PacketRegistry.ProtocolRegistry.class.getDeclaredField("version");
    versionField.setAccessible(true);

    // Overlay packets from PLAY state registry.
    // P.S. I hate it when someone uses var in code, but there I had no choice.
    var playProtocolRegistryVersions = (Map<ProtocolVersion, StateRegistry.PacketRegistry.ProtocolRegistry>) VERSIONS_FIELD.get(playRegistry);
    Map<ProtocolVersion, StateRegistry.PacketRegistry.ProtocolRegistry> versions = new EnumMap<>(ProtocolVersion.class);
    for (ProtocolVersion version : ProtocolVersion.values()) {
      if (!version.isLegacy() && !version.isUnknown()) {
        StateRegistry.PacketRegistry.ProtocolRegistry playProtoRegistry = playProtocolRegistryVersions.get(version);
        var protoRegistry = (StateRegistry.PacketRegistry.ProtocolRegistry) unsafe.allocateInstance(StateRegistry.PacketRegistry.ProtocolRegistry.class);

        versionField.set(protoRegistry, version);

        var playPacketIDToSupplier = (IntObjectMap<Supplier<? extends MinecraftPacket>>) PACKET_ID_TO_SUPPLIER_FIELD.get(playProtoRegistry);
        PACKET_ID_TO_SUPPLIER_FIELD.set(protoRegistry, new OverlayIntObjectMap<>(playPacketIDToSupplier, new IntObjectHashMap<>(16, 0.5F)));

        var playPacketClassToID = (Object2IntMap<Class<? extends MinecraftPacket>>) PACKET_CLASS_TO_ID_FIELD.get(playProtoRegistry);
        Object2IntMap<Class<? extends MinecraftPacket>> packetClassToID = new Object2IntOpenHashMap<>(16, 0.5F);
        packetClassToID.defaultReturnValue(playPacketClassToID.defaultReturnValue());
        PACKET_CLASS_TO_ID_FIELD.set(protoRegistry, new OverlayObject2IntMap<>(playPacketClassToID, packetClassToID));

        versions.put(version, protoRegistry);
      }
    }

    VERSIONS_FIELD.set(registry, Collections.unmodifiableMap(versions));

    Field fallbackField = StateRegistry.PacketRegistry.class.getDeclaredField("fallback");
    fallbackField.setAccessible(true);
    fallbackField.set(registry, false);

    Field registryField = StateRegistry.class.getDeclaredField(registryName);
    registryField.setAccessible(true);
    registryField.set(LIMBO_STATE_REGISTRY, registry);
  }

  public static void init() throws ReflectiveOperationException {
    register(PacketDirection.CLIENTBOUND,
        ChangeGameStatePacket.class, ChangeGameStatePacket::new,
        createMapping(0x2B, ProtocolVersion.MINECRAFT_1_7_2, true),
        createMapping(0x1E, ProtocolVersion.MINECRAFT_1_9, true),
        createMapping(0x20, ProtocolVersion.MINECRAFT_1_13, true),
        createMapping(0x1E, ProtocolVersion.MINECRAFT_1_14, true),
        createMapping(0x1F, ProtocolVersion.MINECRAFT_1_15, true),
        createMapping(0x1E, ProtocolVersion.MINECRAFT_1_16, true),
        createMapping(0x1D, ProtocolVersion.MINECRAFT_1_16_2, true),
        createMapping(0x1E, ProtocolVersion.MINECRAFT_1_17, true),
        createMapping(0x1B, ProtocolVersion.MINECRAFT_1_19, true),
        createMapping(0x1D, ProtocolVersion.MINECRAFT_1_19_1, true)
    );
    register(PacketDirection.CLIENTBOUND,
        ChunkDataPacket.class, ChunkDataPacket::new,
        createMapping(0x21, ProtocolVersion.MINECRAFT_1_7_2, true),
        createMapping(0x20, ProtocolVersion.MINECRAFT_1_9, true),
        createMapping(0x22, ProtocolVersion.MINECRAFT_1_13, true),
        createMapping(0x21, ProtocolVersion.MINECRAFT_1_14, true),
        createMapping(0x22, ProtocolVersion.MINECRAFT_1_15, true),
        createMapping(0x21, ProtocolVersion.MINECRAFT_1_16, true),
        createMapping(0x20, ProtocolVersion.MINECRAFT_1_16_2, true),
        createMapping(0x22, ProtocolVersion.MINECRAFT_1_17, true),
        createMapping(0x1F, ProtocolVersion.MINECRAFT_1_19, true),
        createMapping(0x21, ProtocolVersion.MINECRAFT_1_19_1, true)
    );
    register(PacketDirection.CLIENTBOUND,
        DefaultSpawnPositionPacket.class, DefaultSpawnPositionPacket::new,
        createMapping(0x05, ProtocolVersion.MINECRAFT_1_7_2, true),
        createMapping(0x43, ProtocolVersion.MINECRAFT_1_9, true),
        createMapping(0x45, ProtocolVersion.MINECRAFT_1_12, true),
        createMapping(0x46, ProtocolVersion.MINECRAFT_1_12_1, true),
        createMapping(0x49, ProtocolVersion.MINECRAFT_1_13, true),
        createMapping(0x4D, ProtocolVersion.MINECRAFT_1_14, true),
        createMapping(0x4E, ProtocolVersion.MINECRAFT_1_15, true),
        createMapping(0x42, ProtocolVersion.MINECRAFT_1_16, true),
        createMapping(0x4B, ProtocolVersion.MINECRAFT_1_17, true),
        createMapping(0x4A, ProtocolVersion.MINECRAFT_1_19, true),
        createMapping(0x4D, ProtocolVersion.MINECRAFT_1_19_1, true)
    );
    register(PacketDirection.CLIENTBOUND,
        MapDataPacket.class, MapDataPacket::new,
        createMapping(0x34, ProtocolVersion.MINECRAFT_1_7_2, true),
        createMapping(0x24, ProtocolVersion.MINECRAFT_1_9, true),
        createMapping(0x26, ProtocolVersion.MINECRAFT_1_13, true),
        createMapping(0x27, ProtocolVersion.MINECRAFT_1_15, true),
        createMapping(0x26, ProtocolVersion.MINECRAFT_1_16, true),
        createMapping(0x25, ProtocolVersion.MINECRAFT_1_16_2, true),
        createMapping(0x27, ProtocolVersion.MINECRAFT_1_17, true),
        createMapping(0x24, ProtocolVersion.MINECRAFT_1_19, true),
        createMapping(0x26, ProtocolVersion.MINECRAFT_1_19_1, true)
    );
    register(PacketDirection.CLIENTBOUND,
        PlayerAbilitiesPacket.class, PlayerAbilitiesPacket::new,
        createMapping(0x39, ProtocolVersion.MINECRAFT_1_7_2, true),
        createMapping(0x2B, ProtocolVersion.MINECRAFT_1_9, true),
        createMapping(0x2C, ProtocolVersion.MINECRAFT_1_12_1, true),
        createMapping(0x2E, ProtocolVersion.MINECRAFT_1_13, true),
        createMapping(0x31, ProtocolVersion.MINECRAFT_1_14, true),
        createMapping(0x32, ProtocolVersion.MINECRAFT_1_15, true),
        createMapping(0x31, ProtocolVersion.MINECRAFT_1_16, true),
        createMapping(0x30, ProtocolVersion.MINECRAFT_1_16_2, true),
        createMapping(0x32, ProtocolVersion.MINECRAFT_1_17, true),
        createMapping(0x2F, ProtocolVersion.MINECRAFT_1_19, true),
        createMapping(0x31, ProtocolVersion.MINECRAFT_1_19_1, true)
    );
    register(PacketDirection.CLIENTBOUND,
        PositionRotationPacket.class, PositionRotationPacket::new,
        createMapping(0x08, ProtocolVersion.MINECRAFT_1_7_2, true),
        createMapping(0x2E, ProtocolVersion.MINECRAFT_1_9, true),
        createMapping(0x2F, ProtocolVersion.MINECRAFT_1_12_1, true),
        createMapping(0x32, ProtocolVersion.MINECRAFT_1_13, true),
        createMapping(0x35, ProtocolVersion.MINECRAFT_1_14, true),
        createMapping(0x36, ProtocolVersion.MINECRAFT_1_15, true),
        createMapping(0x35, ProtocolVersion.MINECRAFT_1_16, true),
        createMapping(0x34, ProtocolVersion.MINECRAFT_1_16_2, true),
        createMapping(0x38, ProtocolVersion.MINECRAFT_1_17, true),
        createMapping(0x36, ProtocolVersion.MINECRAFT_1_19, true),
        createMapping(0x39, ProtocolVersion.MINECRAFT_1_19_1, true)
    );
    register(PacketDirection.CLIENTBOUND,
        SetExperiencePacket.class, SetExperiencePacket::new,
        createMapping(0x1F, ProtocolVersion.MINECRAFT_1_7_2, true),
        createMapping(0x3D, ProtocolVersion.MINECRAFT_1_9, true),
        createMapping(0x3F, ProtocolVersion.MINECRAFT_1_12, true),
        createMapping(0x40, ProtocolVersion.MINECRAFT_1_12_1, true),
        createMapping(0x43, ProtocolVersion.MINECRAFT_1_13, true),
        createMapping(0x47, ProtocolVersion.MINECRAFT_1_14, true),
        createMapping(0x48, ProtocolVersion.MINECRAFT_1_15, true),
        createMapping(0x51, ProtocolVersion.MINECRAFT_1_17, true),
        createMapping(0x54, ProtocolVersion.MINECRAFT_1_19_1, true)
    );
    register(PacketDirection.CLIENTBOUND,
        SetSlotPacket.class, SetSlotPacket::new,
        createMapping(0x2F, ProtocolVersion.MINECRAFT_1_7_2, true),
        createMapping(0x16, ProtocolVersion.MINECRAFT_1_9, true),
        createMapping(0x17, ProtocolVersion.MINECRAFT_1_13, true),
        createMapping(0x16, ProtocolVersion.MINECRAFT_1_14, true),
        createMapping(0x17, ProtocolVersion.MINECRAFT_1_15, true),
        createMapping(0x16, ProtocolVersion.MINECRAFT_1_16, true),
        createMapping(0x15, ProtocolVersion.MINECRAFT_1_16_2, true),
        createMapping(0x16, ProtocolVersion.MINECRAFT_1_17, true),
        createMapping(0x13, ProtocolVersion.MINECRAFT_1_19, true)
    );
    register(PacketDirection.CLIENTBOUND,
        TimeUpdatePacket.class, TimeUpdatePacket::new,
        createMapping(0x03, ProtocolVersion.MINECRAFT_1_7_2, true),
        createMapping(0x44, ProtocolVersion.MINECRAFT_1_9, true),
        createMapping(0x46, ProtocolVersion.MINECRAFT_1_12, true),
        createMapping(0x47, ProtocolVersion.MINECRAFT_1_12_1, true),
        createMapping(0x4A, ProtocolVersion.MINECRAFT_1_13, true),
        createMapping(0x4E, ProtocolVersion.MINECRAFT_1_14, true),
        createMapping(0x4F, ProtocolVersion.MINECRAFT_1_15, true),
        createMapping(0x4E, ProtocolVersion.MINECRAFT_1_16, true),
        createMapping(0x58, ProtocolVersion.MINECRAFT_1_17, true),
        createMapping(0x59, ProtocolVersion.MINECRAFT_1_18, true),
        createMapping(0x5C, ProtocolVersion.MINECRAFT_1_19_1, true)
    );
    register(PacketDirection.CLIENTBOUND,
        UpdateViewPositionPacket.class, UpdateViewPositionPacket::new, // ViewCentre, ChunkRenderDistanceCenter
        createMapping(0x40, ProtocolVersion.MINECRAFT_1_14, true),
        createMapping(0x41, ProtocolVersion.MINECRAFT_1_15, true),
        createMapping(0x40, ProtocolVersion.MINECRAFT_1_16, true),
        createMapping(0x49, ProtocolVersion.MINECRAFT_1_17, true),
        createMapping(0x48, ProtocolVersion.MINECRAFT_1_19, true),
        createMapping(0x4B, ProtocolVersion.MINECRAFT_1_19_1, true)
    );

    register(PacketDirection.SERVERBOUND,
        MovePacket.class, MovePacket::new,
        createMapping(0x06, ProtocolVersion.MINECRAFT_1_7_2, false),
        createMapping(0x0D, ProtocolVersion.MINECRAFT_1_9, false),
        createMapping(0x0F, ProtocolVersion.MINECRAFT_1_12, false),
        createMapping(0x0E, ProtocolVersion.MINECRAFT_1_12_1, false),
        createMapping(0x11, ProtocolVersion.MINECRAFT_1_13, false),
        createMapping(0x12, ProtocolVersion.MINECRAFT_1_14, false),
        createMapping(0x13, ProtocolVersion.MINECRAFT_1_16, false),
        createMapping(0x12, ProtocolVersion.MINECRAFT_1_17, false),
        createMapping(0x14, ProtocolVersion.MINECRAFT_1_19, false),
        createMapping(0x15, ProtocolVersion.MINECRAFT_1_19_1, false)
    );
    register(PacketDirection.SERVERBOUND,
        MovePositionOnlyPacket.class, MovePositionOnlyPacket::new,
        createMapping(0x04, ProtocolVersion.MINECRAFT_1_7_2, false),
        createMapping(0x0C, ProtocolVersion.MINECRAFT_1_9, false),
        createMapping(0x0E, ProtocolVersion.MINECRAFT_1_12, false),
        createMapping(0x0D, ProtocolVersion.MINECRAFT_1_12_1, false),
        createMapping(0x10, ProtocolVersion.MINECRAFT_1_13, false),
        createMapping(0x11, ProtocolVersion.MINECRAFT_1_14, false),
        createMapping(0x12, ProtocolVersion.MINECRAFT_1_16, false),
        createMapping(0x11, ProtocolVersion.MINECRAFT_1_17, false),
        createMapping(0x13, ProtocolVersion.MINECRAFT_1_19, false),
        createMapping(0x14, ProtocolVersion.MINECRAFT_1_19_1, false)
    );
    register(PacketDirection.SERVERBOUND,
        MoveRotationOnlyPacket.class, MoveRotationOnlyPacket::new,
        createMapping(0x05, ProtocolVersion.MINECRAFT_1_7_2, false),
        createMapping(0x0E, ProtocolVersion.MINECRAFT_1_9, false),
        createMapping(0x10, ProtocolVersion.MINECRAFT_1_12, false),
        createMapping(0x0F, ProtocolVersion.MINECRAFT_1_12_1, false),
        createMapping(0x12, ProtocolVersion.MINECRAFT_1_13, false),
        createMapping(0x13, ProtocolVersion.MINECRAFT_1_14, false),
        createMapping(0x14, ProtocolVersion.MINECRAFT_1_16, false),
        createMapping(0x13, ProtocolVersion.MINECRAFT_1_17, false),
        createMapping(0x15, ProtocolVersion.MINECRAFT_1_19, false),
        createMapping(0x16, ProtocolVersion.MINECRAFT_1_19_1, false)
    );
    register(PacketDirection.SERVERBOUND,
        MoveOnGroundOnlyPacket.class, MoveOnGroundOnlyPacket::new,
        createMapping(0x03, ProtocolVersion.MINECRAFT_1_7_2, false),
        createMapping(0x0F, ProtocolVersion.MINECRAFT_1_9, false),
        createMapping(0x0D, ProtocolVersion.MINECRAFT_1_12, false),
        createMapping(0x0C, ProtocolVersion.MINECRAFT_1_12_1, false),
        createMapping(0x0F, ProtocolVersion.MINECRAFT_1_13, false),
        createMapping(0x14, ProtocolVersion.MINECRAFT_1_14, false),
        createMapping(0x15, ProtocolVersion.MINECRAFT_1_16, false),
        createMapping(0x14, ProtocolVersion.MINECRAFT_1_17, false),
        createMapping(0x16, ProtocolVersion.MINECRAFT_1_19, false),
        createMapping(0x17, ProtocolVersion.MINECRAFT_1_19_1, false)
    );
    register(PacketDirection.SERVERBOUND,
        TeleportConfirmPacket.class, TeleportConfirmPacket::new,
        createMapping(0x00, ProtocolVersion.MINECRAFT_1_9, false)
    );
  }

  public static void register(PacketDirection direction, Class<?> packetClass, Supplier<?> packetSupplier, PacketMapping[] mappings) {
    register(direction, packetClass, packetSupplier, Arrays.stream(mappings).map(mapping -> {
      try {
        return createMapping(mapping.getID(), mapping.getProtocolVersion(), mapping.getLastValidProtocolVersion(), mapping.isEncodeOnly());
      } catch (ReflectiveOperationException e) {
        throw new ReflectionException(e);
      }
    }).toArray(StateRegistry.PacketMapping[]::new));
  }

  private static void register(PacketDirection direction, Class<?> packetClass, Supplier<?> packetSupplier, StateRegistry.PacketMapping... mappings) {
    StateRegistry.PacketRegistry registry;
    switch (direction) {
      case CLIENTBOUND: {
        registry = LIMBO_STATE_REGISTRY.clientbound;
        break;
      }
      case SERVERBOUND: {
        registry = LIMBO_STATE_REGISTRY.serverbound;
        break;
      }
      default: {
        throw new IllegalStateException("Unexpected value: " + direction);
      }
    }

    try {
      REGISTER_METHOD.invoke(registry, packetClass, packetSupplier, mappings);
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new ReflectionException(e);
    }
  }

  private static StateRegistry.PacketMapping createMapping(int id, ProtocolVersion version, boolean encodeOnly) throws ReflectiveOperationException {
    return createMapping(id, version, null, encodeOnly);
  }

  private static StateRegistry.PacketMapping createMapping(int id, ProtocolVersion version, ProtocolVersion lastValidProtocolVersion, boolean encodeOnly)
      throws ReflectiveOperationException {
    return PACKET_MAPPING_CONSTRUCTOR.newInstance(id, version, lastValidProtocolVersion, encodeOnly);
  }

  public static StateRegistry getLimboStateRegistry() {
    return LIMBO_STATE_REGISTRY;
  }
}
