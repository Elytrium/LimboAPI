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

package net.elytrium.limboapi.protocol;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.StateRegistry;
import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.elytrium.commons.utils.reflection.ReflectionException;
import net.elytrium.limboapi.api.protocol.PacketDirection;
import net.elytrium.limboapi.api.protocol.packets.PacketMapping;
import net.elytrium.limboapi.api.utils.OverlayMap;
import net.elytrium.limboapi.protocol.packets.c2s.MoveOnGroundOnlyPacket;
import net.elytrium.limboapi.protocol.packets.c2s.MovePacket;
import net.elytrium.limboapi.protocol.packets.c2s.MovePositionOnlyPacket;
import net.elytrium.limboapi.protocol.packets.c2s.MoveRotationOnlyPacket;
import net.elytrium.limboapi.protocol.packets.c2s.PlayerChatSessionPacket;
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
import net.elytrium.limboapi.protocol.packets.s2c.UpdateTagsPacket;
import net.elytrium.limboapi.protocol.packets.s2c.UpdateViewPositionPacket;
import net.elytrium.limboapi.utils.OverlayIntObjectMap;
import net.elytrium.limboapi.utils.OverlayObject2IntMap;
import sun.misc.Unsafe;

@SuppressWarnings("unchecked")
public class LimboProtocol {

  private static final StateRegistry LIMBO_STATE_REGISTRY;
  private static final MethodHandle REGISTER_METHOD;
  private static final MethodHandle PACKET_MAPPING_CONSTRUCTOR;
  private static final Unsafe UNSAFE;

  public static final String READ_TIMEOUT = "limboapi-read-timeout";

  public static final MethodHandle VERSIONS_GETTER;
  public static final Field VERSIONS_FIELD;
  public static final MethodHandle PACKET_ID_TO_SUPPLIER_GETTER;
  public static final Field PACKET_ID_TO_SUPPLIER_FIELD;
  public static final MethodHandle PACKET_CLASS_TO_ID_GETTER;
  public static final Field PACKET_CLASS_TO_ID_FIELD;
  public static final StateRegistry.PacketRegistry PLAY_CLIENTBOUND_REGISTRY;
  public static final StateRegistry.PacketRegistry PLAY_SERVERBOUND_REGISTRY;
  public static final StateRegistry.PacketRegistry LIMBO_CLIENTBOUND_REGISTRY;
  public static final StateRegistry.PacketRegistry LIMBO_SERVERBOUND_REGISTRY;
  public static final MethodHandle SERVERBOUND_REGISTRY_GETTER;
  public static final MethodHandle CLIENTBOUND_REGISTRY_GETTER;

  static {
    try {
      Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
      unsafeField.setAccessible(true);
      UNSAFE = (Unsafe) unsafeField.get(null);

      LIMBO_STATE_REGISTRY = (StateRegistry) UNSAFE.allocateInstance(StateRegistry.class);

      VERSIONS_GETTER = MethodHandles.privateLookupIn(StateRegistry.PacketRegistry.class, MethodHandles.lookup())
          .findGetter(StateRegistry.PacketRegistry.class, "versions", Map.class);
      VERSIONS_FIELD = StateRegistry.PacketRegistry.class.getDeclaredField("versions");
      VERSIONS_FIELD.setAccessible(true);

      PACKET_ID_TO_SUPPLIER_GETTER = MethodHandles.privateLookupIn(StateRegistry.PacketRegistry.ProtocolRegistry.class, MethodHandles.lookup())
          .findGetter(StateRegistry.PacketRegistry.ProtocolRegistry.class, "packetIdToSupplier", IntObjectMap.class);
      PACKET_ID_TO_SUPPLIER_FIELD = StateRegistry.PacketRegistry.ProtocolRegistry.class.getDeclaredField("packetIdToSupplier");
      PACKET_ID_TO_SUPPLIER_FIELD.setAccessible(true);

      PACKET_CLASS_TO_ID_GETTER = MethodHandles.privateLookupIn(StateRegistry.PacketRegistry.ProtocolRegistry.class, MethodHandles.lookup())
          .findGetter(StateRegistry.PacketRegistry.ProtocolRegistry.class, "packetClassToId", Object2IntMap.class);
      PACKET_CLASS_TO_ID_FIELD = StateRegistry.PacketRegistry.ProtocolRegistry.class.getDeclaredField("packetClassToId");
      PACKET_CLASS_TO_ID_FIELD.setAccessible(true);

      CLIENTBOUND_REGISTRY_GETTER = MethodHandles.privateLookupIn(StateRegistry.class, MethodHandles.lookup())
          .findGetter(StateRegistry.class, "clientbound", StateRegistry.PacketRegistry.class);
      SERVERBOUND_REGISTRY_GETTER = MethodHandles.privateLookupIn(StateRegistry.class, MethodHandles.lookup())
          .findGetter(StateRegistry.class, "serverbound", StateRegistry.PacketRegistry.class);

      PLAY_CLIENTBOUND_REGISTRY = (StateRegistry.PacketRegistry) CLIENTBOUND_REGISTRY_GETTER.invokeExact(StateRegistry.PLAY);
      PLAY_SERVERBOUND_REGISTRY = (StateRegistry.PacketRegistry) SERVERBOUND_REGISTRY_GETTER.invokeExact(StateRegistry.PLAY);

      overlayRegistry(LIMBO_STATE_REGISTRY, "clientbound", PLAY_CLIENTBOUND_REGISTRY);
      overlayRegistry(LIMBO_STATE_REGISTRY, "serverbound", PLAY_SERVERBOUND_REGISTRY);

      LIMBO_CLIENTBOUND_REGISTRY = (StateRegistry.PacketRegistry) CLIENTBOUND_REGISTRY_GETTER.invokeExact(LIMBO_STATE_REGISTRY);
      LIMBO_SERVERBOUND_REGISTRY = (StateRegistry.PacketRegistry) SERVERBOUND_REGISTRY_GETTER.invokeExact(LIMBO_STATE_REGISTRY);

      REGISTER_METHOD = MethodHandles.privateLookupIn(StateRegistry.PacketRegistry.class, MethodHandles.lookup())
          .findVirtual(StateRegistry.PacketRegistry.class, "register",
              MethodType.methodType(void.class, Class.class, Supplier.class, StateRegistry.PacketMapping[].class));

      PACKET_MAPPING_CONSTRUCTOR = MethodHandles.privateLookupIn(StateRegistry.PacketRegistry.class, MethodHandles.lookup())
          .findConstructor(StateRegistry.PacketMapping.class,
              MethodType.methodType(void.class, int.class, ProtocolVersion.class, ProtocolVersion.class, boolean.class));
    } catch (Throwable e) {
      throw new ReflectionException(e);
    }
  }

  private static void overlayRegistry(StateRegistry stateRegistry,
                                      String registryName, StateRegistry.PacketRegistry playRegistry) throws Throwable {
    StateRegistry.PacketRegistry registry = (StateRegistry.PacketRegistry) UNSAFE.allocateInstance(StateRegistry.PacketRegistry.class);

    Field directionField = StateRegistry.PacketRegistry.class.getDeclaredField("direction");
    directionField.setAccessible(true);
    directionField.set(registry, directionField.get(playRegistry));

    Field versionField = StateRegistry.PacketRegistry.ProtocolRegistry.class.getDeclaredField("version");
    versionField.setAccessible(true);

    // Overlay packets from PLAY state registry.
    // P.S. I hate it when someone uses var in code, but there I had no choice.
    var playProtocolRegistryVersions =
        (Map<ProtocolVersion, StateRegistry.PacketRegistry.ProtocolRegistry>) VERSIONS_GETTER.invokeExact(playRegistry);
    Map<ProtocolVersion, StateRegistry.PacketRegistry.ProtocolRegistry> versions = new EnumMap<>(ProtocolVersion.class);
    for (ProtocolVersion version : ProtocolVersion.values()) {
      if (!version.isLegacy() && !version.isUnknown()) {
        StateRegistry.PacketRegistry.ProtocolRegistry playProtoRegistry = playProtocolRegistryVersions.get(version);
        var protoRegistry = (StateRegistry.PacketRegistry.ProtocolRegistry) UNSAFE.allocateInstance(StateRegistry.PacketRegistry.ProtocolRegistry.class);

        versionField.set(protoRegistry, version);

        var playPacketIDToSupplier = (IntObjectMap<Supplier<? extends MinecraftPacket>>) PACKET_ID_TO_SUPPLIER_GETTER.invokeExact(playProtoRegistry);
        PACKET_ID_TO_SUPPLIER_FIELD.set(protoRegistry, new OverlayIntObjectMap<>(playPacketIDToSupplier, new IntObjectHashMap<>(16, 0.5F)));

        var playPacketClassToID = (Object2IntMap<Class<? extends MinecraftPacket>>) PACKET_CLASS_TO_ID_GETTER.invokeExact(playProtoRegistry);
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
    registryField.set(stateRegistry, registry);
  }

  public static void init() throws Throwable {
    register(LIMBO_STATE_REGISTRY, PacketDirection.CLIENTBOUND,
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
        createMapping(0x1D, ProtocolVersion.MINECRAFT_1_19_1, true),
        createMapping(0x1C, ProtocolVersion.MINECRAFT_1_19_3, true),
        createMapping(0x1F, ProtocolVersion.MINECRAFT_1_19_4, true),
        createMapping(0x20, ProtocolVersion.MINECRAFT_1_20_2, true)
    );
    register(LIMBO_STATE_REGISTRY, PacketDirection.CLIENTBOUND,
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
        createMapping(0x21, ProtocolVersion.MINECRAFT_1_19_1, true),
        createMapping(0x20, ProtocolVersion.MINECRAFT_1_19_3, true),
        createMapping(0x24, ProtocolVersion.MINECRAFT_1_19_4, true),
        createMapping(0x25, ProtocolVersion.MINECRAFT_1_20_2, true)
    );
    register(LIMBO_STATE_REGISTRY, PacketDirection.CLIENTBOUND,
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
        createMapping(0x4D, ProtocolVersion.MINECRAFT_1_19_1, true),
        createMapping(0x4C, ProtocolVersion.MINECRAFT_1_19_3, true),
        createMapping(0x50, ProtocolVersion.MINECRAFT_1_19_4, true),
        createMapping(0x52, ProtocolVersion.MINECRAFT_1_20_2, true),
        createMapping(0x54, ProtocolVersion.MINECRAFT_1_20_3, true)
    );
    register(LIMBO_STATE_REGISTRY, PacketDirection.CLIENTBOUND,
        MapDataPacket.class, MapDataPacket::new,
        createMapping(0x34, ProtocolVersion.MINECRAFT_1_7_2, true),
        createMapping(0x24, ProtocolVersion.MINECRAFT_1_9, true),
        createMapping(0x26, ProtocolVersion.MINECRAFT_1_13, true),
        createMapping(0x27, ProtocolVersion.MINECRAFT_1_15, true),
        createMapping(0x26, ProtocolVersion.MINECRAFT_1_16, true),
        createMapping(0x25, ProtocolVersion.MINECRAFT_1_16_2, true),
        createMapping(0x27, ProtocolVersion.MINECRAFT_1_17, true),
        createMapping(0x24, ProtocolVersion.MINECRAFT_1_19, true),
        createMapping(0x26, ProtocolVersion.MINECRAFT_1_19_1, true),
        createMapping(0x25, ProtocolVersion.MINECRAFT_1_19_3, true),
        createMapping(0x29, ProtocolVersion.MINECRAFT_1_19_4, true),
        createMapping(0x2A, ProtocolVersion.MINECRAFT_1_20_2, true)
    );
    register(LIMBO_STATE_REGISTRY, PacketDirection.CLIENTBOUND,
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
        createMapping(0x31, ProtocolVersion.MINECRAFT_1_19_1, true),
        createMapping(0x30, ProtocolVersion.MINECRAFT_1_19_3, true),
        createMapping(0x34, ProtocolVersion.MINECRAFT_1_19_4, true),
        createMapping(0x36, ProtocolVersion.MINECRAFT_1_20_2, true)
    );
    register(LIMBO_STATE_REGISTRY, PacketDirection.CLIENTBOUND,
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
        createMapping(0x39, ProtocolVersion.MINECRAFT_1_19_1, true),
        createMapping(0x38, ProtocolVersion.MINECRAFT_1_19_3, true),
        createMapping(0x3C, ProtocolVersion.MINECRAFT_1_19_4, true),
        createMapping(0x3E, ProtocolVersion.MINECRAFT_1_20_2, true)
    );
    register(LIMBO_STATE_REGISTRY, PacketDirection.CLIENTBOUND,
        SetExperiencePacket.class, SetExperiencePacket::new,
        createMapping(0x1F, ProtocolVersion.MINECRAFT_1_7_2, true),
        createMapping(0x3D, ProtocolVersion.MINECRAFT_1_9, true),
        createMapping(0x3F, ProtocolVersion.MINECRAFT_1_12, true),
        createMapping(0x40, ProtocolVersion.MINECRAFT_1_12_1, true),
        createMapping(0x43, ProtocolVersion.MINECRAFT_1_13, true),
        createMapping(0x47, ProtocolVersion.MINECRAFT_1_14, true),
        createMapping(0x48, ProtocolVersion.MINECRAFT_1_15, true),
        createMapping(0x51, ProtocolVersion.MINECRAFT_1_17, true),
        createMapping(0x54, ProtocolVersion.MINECRAFT_1_19_1, true),
        createMapping(0x52, ProtocolVersion.MINECRAFT_1_19_3, true),
        createMapping(0x56, ProtocolVersion.MINECRAFT_1_19_4, true),
        createMapping(0x58, ProtocolVersion.MINECRAFT_1_20_2, true),
        createMapping(0x5A, ProtocolVersion.MINECRAFT_1_20_3, true)
    );
    register(LIMBO_STATE_REGISTRY, PacketDirection.CLIENTBOUND,
        SetSlotPacket.class, SetSlotPacket::new,
        createMapping(0x2F, ProtocolVersion.MINECRAFT_1_7_2, true),
        createMapping(0x16, ProtocolVersion.MINECRAFT_1_9, true),
        createMapping(0x17, ProtocolVersion.MINECRAFT_1_13, true),
        createMapping(0x16, ProtocolVersion.MINECRAFT_1_14, true),
        createMapping(0x17, ProtocolVersion.MINECRAFT_1_15, true),
        createMapping(0x16, ProtocolVersion.MINECRAFT_1_16, true),
        createMapping(0x15, ProtocolVersion.MINECRAFT_1_16_2, true),
        createMapping(0x16, ProtocolVersion.MINECRAFT_1_17, true),
        createMapping(0x13, ProtocolVersion.MINECRAFT_1_19, true),
        createMapping(0x12, ProtocolVersion.MINECRAFT_1_19_3, true),
        createMapping(0x14, ProtocolVersion.MINECRAFT_1_19_4, true),
        createMapping(0x15, ProtocolVersion.MINECRAFT_1_20_2, true)
    );
    register(LIMBO_STATE_REGISTRY, PacketDirection.CLIENTBOUND,
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
        createMapping(0x5C, ProtocolVersion.MINECRAFT_1_19_1, true),
        createMapping(0x5A, ProtocolVersion.MINECRAFT_1_19_3, true),
        createMapping(0x5E, ProtocolVersion.MINECRAFT_1_19_4, true),
        createMapping(0x60, ProtocolVersion.MINECRAFT_1_20_2, true),
        createMapping(0x62, ProtocolVersion.MINECRAFT_1_20_3, true)
    );
    register(LIMBO_STATE_REGISTRY, PacketDirection.CLIENTBOUND,
        UpdateViewPositionPacket.class, UpdateViewPositionPacket::new, // ViewCentre, ChunkRenderDistanceCenter
        createMapping(0x40, ProtocolVersion.MINECRAFT_1_14, true),
        createMapping(0x41, ProtocolVersion.MINECRAFT_1_15, true),
        createMapping(0x40, ProtocolVersion.MINECRAFT_1_16, true),
        createMapping(0x49, ProtocolVersion.MINECRAFT_1_17, true),
        createMapping(0x48, ProtocolVersion.MINECRAFT_1_19, true),
        createMapping(0x4B, ProtocolVersion.MINECRAFT_1_19_1, true),
        createMapping(0x4A, ProtocolVersion.MINECRAFT_1_19_3, true),
        createMapping(0x4E, ProtocolVersion.MINECRAFT_1_19_4, true),
        createMapping(0x50, ProtocolVersion.MINECRAFT_1_20_2, true),
        createMapping(0x52, ProtocolVersion.MINECRAFT_1_20_3, true)
    );
    register(LIMBO_STATE_REGISTRY, PacketDirection.CLIENTBOUND,
        UpdateTagsPacket.class, UpdateTagsPacket::new,
        createMapping(0x55, ProtocolVersion.MINECRAFT_1_13, true),
        createMapping(0x5B, ProtocolVersion.MINECRAFT_1_14, true),
        createMapping(0x5C, ProtocolVersion.MINECRAFT_1_15, true),
        createMapping(0x5B, ProtocolVersion.MINECRAFT_1_16, true),
        createMapping(0x66, ProtocolVersion.MINECRAFT_1_17, true),
        createMapping(0x67, ProtocolVersion.MINECRAFT_1_18, true),
        createMapping(0x68, ProtocolVersion.MINECRAFT_1_19, true),
        createMapping(0x6B, ProtocolVersion.MINECRAFT_1_19_1, true),
        createMapping(0x6A, ProtocolVersion.MINECRAFT_1_19_3, true),
        createMapping(0x6E, ProtocolVersion.MINECRAFT_1_19_4, true),
        createMapping(0x70, ProtocolVersion.MINECRAFT_1_20_2, true),
        createMapping(0x74, ProtocolVersion.MINECRAFT_1_20_3, true)
    );

    register(LIMBO_STATE_REGISTRY, PacketDirection.SERVERBOUND,
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
        createMapping(0x15, ProtocolVersion.MINECRAFT_1_19_1, false),
        createMapping(0x14, ProtocolVersion.MINECRAFT_1_19_3, false),
        createMapping(0x15, ProtocolVersion.MINECRAFT_1_19_4, false),
        createMapping(0x17, ProtocolVersion.MINECRAFT_1_20_2, false),
        createMapping(0x18, ProtocolVersion.MINECRAFT_1_20_3, false)
    );
    register(LIMBO_STATE_REGISTRY, PacketDirection.SERVERBOUND,
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
        createMapping(0x14, ProtocolVersion.MINECRAFT_1_19_1, false),
        createMapping(0x13, ProtocolVersion.MINECRAFT_1_19_3, false),
        createMapping(0x14, ProtocolVersion.MINECRAFT_1_19_4, false),
        createMapping(0x16, ProtocolVersion.MINECRAFT_1_20_2, false),
        createMapping(0x17, ProtocolVersion.MINECRAFT_1_20_3, false)
    );
    register(LIMBO_STATE_REGISTRY, PacketDirection.SERVERBOUND,
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
        createMapping(0x16, ProtocolVersion.MINECRAFT_1_19_1, false),
        createMapping(0x15, ProtocolVersion.MINECRAFT_1_19_3, false),
        createMapping(0x16, ProtocolVersion.MINECRAFT_1_19_4, false),
        createMapping(0x18, ProtocolVersion.MINECRAFT_1_20_2, false),
        createMapping(0x19, ProtocolVersion.MINECRAFT_1_20_3, false)
    );
    register(LIMBO_STATE_REGISTRY, PacketDirection.SERVERBOUND,
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
        createMapping(0x17, ProtocolVersion.MINECRAFT_1_19_1, false),
        createMapping(0x16, ProtocolVersion.MINECRAFT_1_19_3, false),
        createMapping(0x17, ProtocolVersion.MINECRAFT_1_19_4, false),
        createMapping(0x19, ProtocolVersion.MINECRAFT_1_20_2, false),
        createMapping(0x1A, ProtocolVersion.MINECRAFT_1_20_3, false)
    );
    register(LIMBO_STATE_REGISTRY, PacketDirection.SERVERBOUND,
        TeleportConfirmPacket.class, TeleportConfirmPacket::new,
        createMapping(0x00, ProtocolVersion.MINECRAFT_1_9, false)
    );

    register(PLAY_SERVERBOUND_REGISTRY,
        PlayerChatSessionPacket.class, PlayerChatSessionPacket::new,
        createMapping(0x20, ProtocolVersion.MINECRAFT_1_19_3, false),
        createMapping(0x06, ProtocolVersion.MINECRAFT_1_19_4, false)
    );
  }

  public static StateRegistry createLocalStateRegistry() {
    try {
      StateRegistry stateRegistry = (StateRegistry) UNSAFE.allocateInstance(StateRegistry.class);
      overlayRegistry(stateRegistry, "clientbound", LIMBO_CLIENTBOUND_REGISTRY);
      overlayRegistry(stateRegistry, "serverbound", LIMBO_SERVERBOUND_REGISTRY);
      return stateRegistry;
    } catch (Throwable e) {
      throw new ReflectionException(e);
    }
  }

  public static void register(StateRegistry stateRegistry,
                              PacketDirection direction, Class<?> packetClass, Supplier<?> packetSupplier, PacketMapping[] mappings) {
    register(stateRegistry, direction, packetClass, packetSupplier, Arrays.stream(mappings).map(mapping -> {
      try {
        return createMapping(mapping.getID(), mapping.getProtocolVersion(), mapping.getLastValidProtocolVersion(), mapping.isEncodeOnly());
      } catch (Throwable e) {
        throw new ReflectionException(e);
      }
    }).toArray(StateRegistry.PacketMapping[]::new));
  }

  public static void register(StateRegistry stateRegistry,
                              PacketDirection direction, Class<?> packetClass, Supplier<?> packetSupplier, StateRegistry.PacketMapping... mappings) {
    MethodHandle registryGetter;
    switch (direction) {
      case CLIENTBOUND: {
        registryGetter = CLIENTBOUND_REGISTRY_GETTER;
        break;
      }
      case SERVERBOUND: {
        registryGetter = SERVERBOUND_REGISTRY_GETTER;
        break;
      }
      default: {
        throw new IllegalStateException("Unexpected value: " + direction);
      }
    }

    try {
      register((StateRegistry.PacketRegistry) registryGetter.invokeExact(stateRegistry), packetClass, packetSupplier, mappings);
    } catch (Throwable e) {
      throw new ReflectionException(e);
    }
  }

  public static void register(StateRegistry.PacketRegistry registry,
                              Class<?> packetClass, Supplier<?> packetSupplier, StateRegistry.PacketMapping... mappings) {
    try {
      var versions = (Map<ProtocolVersion, StateRegistry.PacketRegistry.ProtocolRegistry>) VERSIONS_GETTER.invokeExact(registry);
      List<OverlayMap<?, ?>> overlayMaps = versions.values().stream().flatMap(protocolRegistry -> {
        try {
          var idToSupplier = (IntObjectMap<Supplier<? extends MinecraftPacket>>) PACKET_ID_TO_SUPPLIER_GETTER.invokeExact(protocolRegistry);
          var classToId = (Object2IntMap<Class<? extends MinecraftPacket>>) PACKET_CLASS_TO_ID_GETTER.invokeExact(protocolRegistry);
          if (idToSupplier instanceof OverlayMap<?, ?> && classToId instanceof OverlayMap<?, ?>) {
            return Stream.of(
                (OverlayMap<?, ?>) idToSupplier,
                (OverlayMap<?, ?>) classToId
            );
          } else {
            return Stream.empty();
          }
        } catch (Throwable e) {
          throw new ReflectionException(e);
        }
      }).toList();

      overlayMaps.forEach(overlayMap -> overlayMap.setOverride(true));
      REGISTER_METHOD.invokeExact(registry, packetClass, packetSupplier, mappings);
      overlayMaps.forEach(overlayMap -> overlayMap.setOverride(false));
    } catch (Throwable e) {
      throw new ReflectionException(e);
    }
  }

  private static StateRegistry.PacketMapping createMapping(int id, ProtocolVersion version, boolean encodeOnly) throws Throwable {
    return createMapping(id, version, null, encodeOnly);
  }

  private static StateRegistry.PacketMapping createMapping(int id, ProtocolVersion version, ProtocolVersion lastValidProtocolVersion, boolean encodeOnly)
      throws Throwable {
    return (StateRegistry.PacketMapping) PACKET_MAPPING_CONSTRUCTOR.invokeExact(id, version, lastValidProtocolVersion, encodeOnly);
  }

  public static StateRegistry getLimboStateRegistry() {
    return LIMBO_STATE_REGISTRY;
  }
}
