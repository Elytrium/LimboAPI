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
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.AvailableCommands;
import com.velocitypowered.proxy.protocol.packet.BossBar;
import com.velocitypowered.proxy.protocol.packet.Chat;
import com.velocitypowered.proxy.protocol.packet.ClientSettings;
import com.velocitypowered.proxy.protocol.packet.Disconnect;
import com.velocitypowered.proxy.protocol.packet.HeaderAndFooter;
import com.velocitypowered.proxy.protocol.packet.JoinGame;
import com.velocitypowered.proxy.protocol.packet.KeepAlive;
import com.velocitypowered.proxy.protocol.packet.PlayerListItem;
import com.velocitypowered.proxy.protocol.packet.PluginMessage;
import com.velocitypowered.proxy.protocol.packet.ResourcePackRequest;
import com.velocitypowered.proxy.protocol.packet.ResourcePackResponse;
import com.velocitypowered.proxy.protocol.packet.Respawn;
import com.velocitypowered.proxy.protocol.packet.TabCompleteRequest;
import com.velocitypowered.proxy.protocol.packet.TabCompleteResponse;
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
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Supplier;
import net.elytrium.limboapi.api.protocol.PacketDirection;
import net.elytrium.limboapi.api.protocol.packets.PacketMapping;
import net.elytrium.limboapi.protocol.packet.ChangeGameState;
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
import sun.misc.Unsafe;

@SuppressWarnings("SameParameterValue")
public class LimboProtocol {

  private static StateRegistry limboRegistry;
  private static Method getProtocolRegistry;
  private static Method register;
  private static Constructor<StateRegistry.PacketMapping> ctor;
  private static Field packetClassToId;

  static {
    try {
      Constructor<?> unsafeCtor = Unsafe.class.getDeclaredConstructors()[0];
      unsafeCtor.setAccessible(true);
      Unsafe unsafe = (Unsafe) unsafeCtor.newInstance();
      limboRegistry = (StateRegistry) unsafe.allocateInstance(StateRegistry.class);

      Constructor<StateRegistry.PacketRegistry> localCtor = StateRegistry.PacketRegistry.class.getDeclaredConstructor(ProtocolUtils.Direction.class);
      localCtor.setAccessible(true);

      Field clientBound = StateRegistry.class.getDeclaredField("clientbound");
      clientBound.setAccessible(true);
      clientBound.set(limboRegistry, localCtor.newInstance(ProtocolUtils.Direction.CLIENTBOUND));

      Field serverBound = StateRegistry.class.getDeclaredField("serverbound");
      serverBound.setAccessible(true);
      serverBound.set(limboRegistry, localCtor.newInstance(ProtocolUtils.Direction.SERVERBOUND));

      getProtocolRegistry = StateRegistry.PacketRegistry.class.getDeclaredMethod("getProtocolRegistry", ProtocolVersion.class);
      getProtocolRegistry.setAccessible(true);

      register = StateRegistry.PacketRegistry.class.getDeclaredMethod("register", Class.class, Supplier.class, StateRegistry.PacketMapping[].class);
      register.setAccessible(true);

      ctor = StateRegistry.PacketMapping.class.getDeclaredConstructor(int.class, ProtocolVersion.class, ProtocolVersion.class, boolean.class);
      ctor.setAccessible(true);

      packetClassToId = StateRegistry.PacketRegistry.ProtocolRegistry.class.getDeclaredField("packetClassToId");
      packetClassToId.setAccessible(true);
    } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchFieldException e) {
      e.printStackTrace();
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
                    map(0x1E, ProtocolVersion.MINECRAFT_1_9, true)
            });

    register(PacketDirection.SERVERBOUND,
        TabCompleteRequest.class, TabCompleteRequest::new,
        getMappingsForPacket(StateRegistry.PLAY.serverbound, TabCompleteRequest.class, false)
    );
    register(PacketDirection.SERVERBOUND,
        Chat.class, Chat::new,
        getMappingsForPacket(StateRegistry.PLAY.serverbound, Chat.class, false)
    );
    register(PacketDirection.SERVERBOUND,
        ClientSettings.class, ClientSettings::new,
        getMappingsForPacket(StateRegistry.PLAY.serverbound, ClientSettings.class, false)
    );
    register(PacketDirection.SERVERBOUND,
        PluginMessage.class, PluginMessage::new,
        getMappingsForPacket(StateRegistry.PLAY.serverbound, PluginMessage.class, false)
    );
    register(PacketDirection.SERVERBOUND,
        KeepAlive.class, KeepAlive::new,
        getMappingsForPacket(StateRegistry.PLAY.serverbound, KeepAlive.class, false)
    );
    register(PacketDirection.SERVERBOUND,
        ResourcePackResponse.class, ResourcePackResponse::new,
        getMappingsForPacket(StateRegistry.PLAY.serverbound, ProtocolVersion.MINECRAFT_1_8, ResourcePackResponse.class, false)
    );

    register(PacketDirection.CLIENTBOUND,
        BossBar.class, BossBar::new,
        getMappingsForPacket(StateRegistry.PLAY.clientbound, ProtocolVersion.MINECRAFT_1_9, BossBar.class, false)
    );
    register(PacketDirection.CLIENTBOUND,
        Chat.class, Chat::new,
        getMappingsForPacket(StateRegistry.PLAY.clientbound, Chat.class, true)
    );
    register(PacketDirection.CLIENTBOUND,
        TabCompleteResponse.class, TabCompleteResponse::new,
        getMappingsForPacket(StateRegistry.PLAY.clientbound, TabCompleteResponse.class, false)
    );
    register(PacketDirection.CLIENTBOUND,
        AvailableCommands.class, AvailableCommands::new,
        getMappingsForPacket(StateRegistry.PLAY.clientbound, ProtocolVersion.MINECRAFT_1_13, AvailableCommands.class, false)
    );
    register(PacketDirection.CLIENTBOUND,
        PluginMessage.class, PluginMessage::new,
        getMappingsForPacket(StateRegistry.PLAY.clientbound, PluginMessage.class, false)
    );
    register(PacketDirection.CLIENTBOUND,
        Disconnect.class, Disconnect::new,
        getMappingsForPacket(StateRegistry.PLAY.clientbound, Disconnect.class, false)
    );
    register(PacketDirection.CLIENTBOUND,
        KeepAlive.class, KeepAlive::new,
        getMappingsForPacket(StateRegistry.PLAY.clientbound, KeepAlive.class, false)
    );
    register(PacketDirection.CLIENTBOUND,
        JoinGame.class, JoinGame::new,
        getMappingsForPacket(StateRegistry.PLAY.clientbound, JoinGame.class, false)
    );
    register(PacketDirection.CLIENTBOUND,
        Respawn.class, Respawn::new,
        getMappingsForPacket(StateRegistry.PLAY.clientbound, Respawn.class, true)
    );
    register(PacketDirection.CLIENTBOUND,
        ResourcePackRequest.class, ResourcePackRequest::new,
        getMappingsForPacket(StateRegistry.PLAY.clientbound, ProtocolVersion.MINECRAFT_1_8, ResourcePackRequest.class, false)
    );
    register(PacketDirection.CLIENTBOUND,
        HeaderAndFooter.class, HeaderAndFooter::new,
        getMappingsForPacket(StateRegistry.PLAY.clientbound, ProtocolVersion.MINECRAFT_1_8, HeaderAndFooter.class, true)
    );
    register(PacketDirection.CLIENTBOUND,
        LegacyTitlePacket.class, LegacyTitlePacket::new,
        getMappingsForPacket(
            StateRegistry.PLAY.clientbound,
            ProtocolVersion.MINECRAFT_1_8,
            ProtocolVersion.MINECRAFT_1_16_4,
            LegacyTitlePacket.class,
            true
        )
    );
    register(PacketDirection.CLIENTBOUND,
        TitleSubtitlePacket.class, TitleSubtitlePacket::new,
        getMappingsForPacket(StateRegistry.PLAY.clientbound, ProtocolVersion.MINECRAFT_1_17, TitleSubtitlePacket.class, true)
    );
    register(PacketDirection.CLIENTBOUND,
        TitleTextPacket.class, TitleTextPacket::new,
        getMappingsForPacket(StateRegistry.PLAY.clientbound, ProtocolVersion.MINECRAFT_1_17, TitleTextPacket.class, true)
    );
    register(PacketDirection.CLIENTBOUND,
        TitleActionbarPacket.class, TitleActionbarPacket::new,
        getMappingsForPacket(StateRegistry.PLAY.clientbound, ProtocolVersion.MINECRAFT_1_17, TitleActionbarPacket.class, true)
    );
    register(PacketDirection.CLIENTBOUND,
        TitleTimesPacket.class, TitleTimesPacket::new,
        getMappingsForPacket(StateRegistry.PLAY.clientbound, ProtocolVersion.MINECRAFT_1_17, TitleTimesPacket.class, true)
    );
    register(PacketDirection.CLIENTBOUND,
        TitleClearPacket.class, TitleClearPacket::new,
        getMappingsForPacket(StateRegistry.PLAY.clientbound, ProtocolVersion.MINECRAFT_1_17, TitleClearPacket.class, true)
    );
    register(PacketDirection.CLIENTBOUND,
        PlayerListItem.class, PlayerListItem::new,
        getMappingsForPacket(StateRegistry.PLAY.clientbound, PlayerListItem.class, false)
    );
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
    register(direction, packetClass, packetSupplier, (StateRegistry.PacketMapping[]) Arrays.stream(packetMappings).map(packetMapping -> {
      try {
        return map(packetMapping.getId(), packetMapping.getProtocolVersion(), packetMapping.getLastValidProtocolVersion(), packetMapping.isEncodeOnly());
      } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }).toArray());
  }

  private static StateRegistry.PacketMapping map(int id, ProtocolVersion version, boolean encodeOnly)
      throws InvocationTargetException, InstantiationException, IllegalAccessException {
    return map(id, version, null, encodeOnly);
  }

  private static StateRegistry.PacketMapping map(int id, ProtocolVersion version, ProtocolVersion lastValidProtocolVersion, boolean encodeOnly)
      throws InvocationTargetException, InstantiationException, IllegalAccessException {
    return ctor.newInstance(id, version, lastValidProtocolVersion, encodeOnly);
  }

  private static StateRegistry.PacketMapping[] getMappingsForPacket(StateRegistry.PacketRegistry registry,
      Class<? extends MinecraftPacket> packet, boolean encodeOnly) {
    return getMappingsForPacket(registry, ProtocolVersion.MINIMUM_VERSION, ProtocolVersion.MAXIMUM_VERSION, packet, encodeOnly);
  }

  private static StateRegistry.PacketMapping[] getMappingsForPacket(StateRegistry.PacketRegistry registry,
      ProtocolVersion from, Class<? extends MinecraftPacket> packet, boolean encodeOnly) {
    return getMappingsForPacket(registry, from, ProtocolVersion.MAXIMUM_VERSION, packet, encodeOnly);
  }

  private static StateRegistry.PacketMapping[] getMappingsForPacket(StateRegistry.PacketRegistry registry,
      ProtocolVersion from, ProtocolVersion to, Class<? extends MinecraftPacket> packet, boolean encodeOnly) {
    List<StateRegistry.PacketMapping> mappings = new ArrayList<>();
    for (ProtocolVersion protocol : EnumSet.range(from, to)) {
      int id = getPacketId(registry, packet, protocol);
      try {
        mappings.add(map(id, protocol, null, encodeOnly));
      } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
        e.printStackTrace();
      }
    }

    return mappings.toArray(new StateRegistry.PacketMapping[0]);
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

  @SuppressWarnings("unchecked")
  public static int getPacketId(StateRegistry.PacketRegistry.ProtocolRegistry registry, Class<? extends MinecraftPacket> packet) {
    Object2IntMap<Class<? extends MinecraftPacket>> map = null;
    try {
      map = (Object2IntMap<Class<? extends MinecraftPacket>>) packetClassToId.get(registry);
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    }

    return map == null ? Integer.MIN_VALUE : map.getInt(packet);
  }

  public static StateRegistry getLimboRegistry() {
    return LimboProtocol.limboRegistry;
  }
}
