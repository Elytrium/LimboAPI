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
 */

package net.elytrium.limboapi.protocol;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
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
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Supplier;
import lombok.Getter;
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

@SuppressWarnings("SameParameterValue")
public class LimboProtocol {

  @Getter
  private static StateRegistry limboRegistry;

  private static Method getProtocolRegistry;

  private static Method register;

  private static Constructor<StateRegistry.PacketMapping> ctor;

  private static Field packetClassToId;

  static {
    try {
      Constructor<StateRegistry> localCtor = StateRegistry.class.getDeclaredConstructor();
      localCtor.setAccessible(true);
      limboRegistry = localCtor.newInstance();

      getProtocolRegistry = StateRegistry.PacketRegistry.class
          .getDeclaredMethod("getProtocolRegistry", ProtocolVersion.class);
      getProtocolRegistry.setAccessible(true);

      register = StateRegistry.PacketRegistry.class
          .getDeclaredMethod("register", Class.class, Supplier.class, StateRegistry.PacketMapping[].class);
      register.setAccessible(true);

      ctor = StateRegistry.PacketMapping.class
          .getDeclaredConstructor(int.class, ProtocolVersion.class, ProtocolVersion.class, boolean.class);
      ctor.setAccessible(true);

      packetClassToId = StateRegistry.PacketRegistry.ProtocolRegistry.class
          .getDeclaredField("packetClassToId");
      packetClassToId.setAccessible(true);
    } catch (NoSuchMethodException | InstantiationException
        | IllegalAccessException | InvocationTargetException | NoSuchFieldException e) {
      e.printStackTrace();
    }
  }

  @SneakyThrows
  public static void init() {
    register.invoke(limboRegistry.serverbound,
        TeleportConfirm.class, (Supplier<Object>) TeleportConfirm::new,
        new StateRegistry.PacketMapping[]{
            map(0x00, ProtocolVersion.MINECRAFT_1_9, true)
        });
    register.invoke(limboRegistry.serverbound,
        PlayerPositionAndLook.class, (Supplier<Object>) PlayerPositionAndLook::new,
        new StateRegistry.PacketMapping[]{
            map(0x06, ProtocolVersion.MINECRAFT_1_7_2, false),
            map(0x06, ProtocolVersion.MINECRAFT_1_8, false),
            map(0x0D, ProtocolVersion.MINECRAFT_1_9, false),
            map(0x0F, ProtocolVersion.MINECRAFT_1_12, false),
            map(0x0E, ProtocolVersion.MINECRAFT_1_12_1, false),
            map(0x11, ProtocolVersion.MINECRAFT_1_13, false),
            map(0x12, ProtocolVersion.MINECRAFT_1_14, false),
            map(0x13, ProtocolVersion.MINECRAFT_1_16, false),
            map(0x12, ProtocolVersion.MINECRAFT_1_17, false)
        });
    register.invoke(limboRegistry.serverbound,
        PlayerPosition.class, (Supplier<Object>) PlayerPosition::new,
        new StateRegistry.PacketMapping[]{
          map(0x0B, ProtocolVersion.MINECRAFT_1_7_2, false),
          map(0x04, ProtocolVersion.MINECRAFT_1_7_6, false),
          map(0x04, ProtocolVersion.MINECRAFT_1_8, false),
          map(0x0C, ProtocolVersion.MINECRAFT_1_9, false),
          map(0x0E, ProtocolVersion.MINECRAFT_1_12, false),
          map(0x0D, ProtocolVersion.MINECRAFT_1_12_1, false),
          map(0x10, ProtocolVersion.MINECRAFT_1_13, false),
          map(0x11, ProtocolVersion.MINECRAFT_1_14, false),
          map(0x12, ProtocolVersion.MINECRAFT_1_16, false),
          map(0x11, ProtocolVersion.MINECRAFT_1_17, false)
        });
    register.invoke(limboRegistry.serverbound,
        Player.class, (Supplier<Object>) Player::new,
        new StateRegistry.PacketMapping[]{
          map(0x03, ProtocolVersion.MINECRAFT_1_7_2, false),
          map(0x03, ProtocolVersion.MINECRAFT_1_8, false),
          map(0x0F, ProtocolVersion.MINECRAFT_1_9, false),
          map(0x0D, ProtocolVersion.MINECRAFT_1_12, false),
          map(0x0C, ProtocolVersion.MINECRAFT_1_12_1, false),
          map(0x0F, ProtocolVersion.MINECRAFT_1_13, false),
          map(0x14, ProtocolVersion.MINECRAFT_1_14, false),
          map(0x15, ProtocolVersion.MINECRAFT_1_16, false),
          map(0x14, ProtocolVersion.MINECRAFT_1_17, false)
        });
    register.invoke(limboRegistry.clientbound,
        PlayerPositionAndLook.class, (Supplier<Object>) PlayerPositionAndLook::new,
        new StateRegistry.PacketMapping[]{
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
    register.invoke(limboRegistry.clientbound,
        ChunkData.class, (Supplier<Object>) ChunkData::new,
        new StateRegistry.PacketMapping[]{
          map(0x21, ProtocolVersion.MINECRAFT_1_7_2, true),
          map(0x21, ProtocolVersion.MINECRAFT_1_8, true),
          map(0x20, ProtocolVersion.MINECRAFT_1_9, true),
          map(0x22, ProtocolVersion.MINECRAFT_1_13, true),
          map(0x21, ProtocolVersion.MINECRAFT_1_14, true),
          map(0x22, ProtocolVersion.MINECRAFT_1_15, true),
          map(0x21, ProtocolVersion.MINECRAFT_1_16, true),
          map(0x20, ProtocolVersion.MINECRAFT_1_16_2, true),
          map(0x22, ProtocolVersion.MINECRAFT_1_17, true)
        });
    register.invoke(limboRegistry.clientbound,
        SetSlot.class, (Supplier<Object>) SetSlot::new,
        new StateRegistry.PacketMapping[]{
          map(0x2F, ProtocolVersion.MINECRAFT_1_7_2, true),
          map(0x2F, ProtocolVersion.MINECRAFT_1_8, true),
          map(0x16, ProtocolVersion.MINECRAFT_1_9, true),
          map(0x17, ProtocolVersion.MINECRAFT_1_13, true),
          map(0x16, ProtocolVersion.MINECRAFT_1_14, true),
          map(0x17, ProtocolVersion.MINECRAFT_1_15, true),
          map(0x16, ProtocolVersion.MINECRAFT_1_16, true),
          map(0x15, ProtocolVersion.MINECRAFT_1_16_2, true),
          map(0x16, ProtocolVersion.MINECRAFT_1_17, true)
        });
    register.invoke(limboRegistry.clientbound,
        MapDataPacket.class, (Supplier<Object>) MapDataPacket::new,
        new StateRegistry.PacketMapping[]{
          map(0x34, ProtocolVersion.MINECRAFT_1_7_2, true),
          map(0x34, ProtocolVersion.MINECRAFT_1_8, true),
          map(0x24, ProtocolVersion.MINECRAFT_1_9, true),
          map(0x26, ProtocolVersion.MINECRAFT_1_13, true),
          map(0x26, ProtocolVersion.MINECRAFT_1_14, true),
          map(0x27, ProtocolVersion.MINECRAFT_1_15, true),
          map(0x26, ProtocolVersion.MINECRAFT_1_16, true),
          map(0x25, ProtocolVersion.MINECRAFT_1_16_2, true),
          map(0x27, ProtocolVersion.MINECRAFT_1_17, true)
        });
    register.invoke(limboRegistry.clientbound,
        PlayerAbilities.class, (Supplier<Object>) PlayerAbilities::new,
        new StateRegistry.PacketMapping[]{
          map(0x39, ProtocolVersion.MINECRAFT_1_7_2, true),
          map(0x39, ProtocolVersion.MINECRAFT_1_8, true),
          map(0x2B, ProtocolVersion.MINECRAFT_1_9, true),
          map(0x2C, ProtocolVersion.MINECRAFT_1_12_1, true),
          map(0x2E, ProtocolVersion.MINECRAFT_1_13, true),
          map(0x31, ProtocolVersion.MINECRAFT_1_14, true),
          map(0x32, ProtocolVersion.MINECRAFT_1_15, true),
          map(0x31, ProtocolVersion.MINECRAFT_1_16, true),
          map(0x30, ProtocolVersion.MINECRAFT_1_16_2, true),
          map(0x32, ProtocolVersion.MINECRAFT_1_17, true)
        });
    register.invoke(limboRegistry.clientbound,
        SetExp.class, (Supplier<Object>) SetExp::new,
        new StateRegistry.PacketMapping[]{
          map(0x2B, ProtocolVersion.MINECRAFT_1_7_2, true),
          map(0x1F, ProtocolVersion.MINECRAFT_1_8, true),
          map(0x3D, ProtocolVersion.MINECRAFT_1_9, true),
          map(0x3F, ProtocolVersion.MINECRAFT_1_12, true),
          map(0x40, ProtocolVersion.MINECRAFT_1_12_1, true),
          map(0x43, ProtocolVersion.MINECRAFT_1_13, true),
          map(0x47, ProtocolVersion.MINECRAFT_1_14, true),
          map(0x48, ProtocolVersion.MINECRAFT_1_15, true),
          map(0x51, ProtocolVersion.MINECRAFT_1_17, true)
        });
    register.invoke(limboRegistry.clientbound,
        UpdateViewPosition.class, (Supplier<Object>) UpdateViewPosition::new,
        new StateRegistry.PacketMapping[]{
          map(0x40, ProtocolVersion.MINECRAFT_1_7_2, true),
          map(0x40, ProtocolVersion.MINECRAFT_1_14, true),
          map(0x41, ProtocolVersion.MINECRAFT_1_15, true),
          map(0x40, ProtocolVersion.MINECRAFT_1_16_1, true),
          map(0x49, ProtocolVersion.MINECRAFT_1_17, true)
        });

    register.invoke(limboRegistry.serverbound,
        Chat.class, (Supplier<Object>) Chat::new,
        getMappingsForPacket(StateRegistry.PLAY.serverbound, Chat.class, false));
    register.invoke(limboRegistry.serverbound,
        ClientSettings.class, (Supplier<Object>) ClientSettings::new,
        getMappingsForPacket(StateRegistry.PLAY.serverbound, ClientSettings.class, false));
    register.invoke(limboRegistry.serverbound,
        PluginMessage.class, (Supplier<Object>) PluginMessage::new,
        getMappingsForPacket(StateRegistry.PLAY.serverbound, PluginMessage.class, false));
    register.invoke(limboRegistry.clientbound,
        Chat.class, (Supplier<Object>) Chat::new,
        getMappingsForPacket(StateRegistry.PLAY.clientbound, Chat.class, true));
    register.invoke(limboRegistry.clientbound,
        JoinGame.class, (Supplier<Object>) JoinGame::new,
        getMappingsForPacket(StateRegistry.PLAY.clientbound, JoinGame.class, true));
    register.invoke(limboRegistry.clientbound,
        Disconnect.class, (Supplier<Object>) Disconnect::new,
        getMappingsForPacket(StateRegistry.PLAY.clientbound, Disconnect.class, true));
    register.invoke(limboRegistry.clientbound,
        PluginMessage.class, (Supplier<Object>) PluginMessage::new,
        getMappingsForPacket(StateRegistry.PLAY.clientbound, PluginMessage.class, true));
    register.invoke(limboRegistry.clientbound,
        Respawn.class, (Supplier<Object>) Respawn::new,
        getMappingsForPacket(StateRegistry.PLAY.clientbound, Respawn.class, true));
    register.invoke(limboRegistry.clientbound,
        TitleActionbarPacket.class, (Supplier<Object>) TitleActionbarPacket::new,
        getMappingsForPacket(StateRegistry.PLAY.clientbound,
            ProtocolVersion.MINECRAFT_1_17, TitleActionbarPacket.class, true));
    register.invoke(limboRegistry.clientbound,
        TitleClearPacket.class, (Supplier<Object>) TitleClearPacket::new,
        getMappingsForPacket(StateRegistry.PLAY.clientbound,
            ProtocolVersion.MINECRAFT_1_17, TitleClearPacket.class, true));
    register.invoke(limboRegistry.clientbound,
        TitleSubtitlePacket.class, (Supplier<Object>) TitleSubtitlePacket::new,
        getMappingsForPacket(StateRegistry.PLAY.clientbound,
            ProtocolVersion.MINECRAFT_1_17, TitleSubtitlePacket.class, true));
    register.invoke(limboRegistry.clientbound,
        TitleTextPacket.class, (Supplier<Object>) TitleTextPacket::new,
        getMappingsForPacket(StateRegistry.PLAY.clientbound,
            ProtocolVersion.MINECRAFT_1_17, TitleTextPacket.class, true));
    register.invoke(limboRegistry.clientbound,
        TitleTimesPacket.class, (Supplier<Object>) TitleTimesPacket::new,
        getMappingsForPacket(StateRegistry.PLAY.clientbound,
            ProtocolVersion.MINECRAFT_1_17, TitleTimesPacket.class, true));
    register.invoke(limboRegistry.clientbound,
        LegacyTitlePacket.class, (Supplier<Object>) LegacyTitlePacket::new,
        getMappingsForPacket(StateRegistry.PLAY.clientbound,
            ProtocolVersion.MINECRAFT_1_8, ProtocolVersion.MINECRAFT_1_16_4, LegacyTitlePacket.class, true));
  }

  private static StateRegistry.PacketMapping map(int id, ProtocolVersion version, boolean encodeOnly) {
    return map(id, version, null, encodeOnly);
  }

  @SneakyThrows
  private static StateRegistry.PacketMapping map(
      int id, ProtocolVersion version, ProtocolVersion lastValidProtocolVersion, boolean encodeOnly) {
    return ctor.newInstance(id, version, lastValidProtocolVersion, encodeOnly);
  }

  private static StateRegistry.PacketMapping[] getMappingsForPacket(
      StateRegistry.PacketRegistry registry, Class<? extends MinecraftPacket> packet, boolean encodeOnly) {
    return getMappingsForPacket(
        registry, ProtocolVersion.MINIMUM_VERSION, ProtocolVersion.MAXIMUM_VERSION, packet, encodeOnly);
  }

  private static StateRegistry.PacketMapping[] getMappingsForPacket(
      StateRegistry.PacketRegistry registry, ProtocolVersion from,
      Class<? extends MinecraftPacket> packet, boolean encodeOnly) {

    return getMappingsForPacket(registry, from, ProtocolVersion.MAXIMUM_VERSION, packet, encodeOnly);
  }

  @SneakyThrows
  private static StateRegistry.PacketMapping[] getMappingsForPacket(
      StateRegistry.PacketRegistry registry, ProtocolVersion from, ProtocolVersion to,
      Class<? extends MinecraftPacket> packet, boolean encodeOnly) {

    List<StateRegistry.PacketMapping> mappings = new ArrayList<>();
    for (ProtocolVersion protocol : EnumSet.range(from, to)) {
      int id = getPacketId(registry, packet, protocol);
      mappings.add(map(id, protocol, null, encodeOnly));
    }
    return mappings.toArray(new StateRegistry.PacketMapping[0]);
  }

  @SneakyThrows
  public static int getPacketId(
      StateRegistry.PacketRegistry packetRegistry,
      Class<? extends MinecraftPacket> packet, ProtocolVersion version) {
    StateRegistry.PacketRegistry.ProtocolRegistry protocolRegistry
        = (StateRegistry.PacketRegistry.ProtocolRegistry) getProtocolRegistry.invoke(packetRegistry, version);
    return getPacketId(protocolRegistry, packet);
  }

  @SuppressWarnings("unchecked")
  @SneakyThrows
  public static int getPacketId(
      StateRegistry.PacketRegistry.ProtocolRegistry registry,
      Class<? extends MinecraftPacket> packet) {
    Object2IntMap<Class<? extends MinecraftPacket>> map =
        (Object2IntMap<Class<? extends MinecraftPacket>>) packetClassToId.get(registry);
    return map.getInt(packet);
  }
}
