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

package net.elytrium.limboapi.injection.packet;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.backend.BackendPlaySessionHandler;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.PlayerListItem;
import io.netty.util.collection.IntObjectMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import net.elytrium.limboapi.LimboAPI;

@SuppressWarnings("unchecked")
public class PlayerListItemHook extends PlayerListItem {

  private static Field serverConnField;

  private final LimboAPI plugin;

  private PlayerListItemHook(LimboAPI plugin) {
    this.plugin = plugin;
  }

  public static void init(LimboAPI plugin) {
    try {
      Field versionsField = StateRegistry.PacketRegistry.class.getDeclaredField("versions");
      versionsField.setAccessible(true);

      Field packetIdToSupplierField = StateRegistry.PacketRegistry.ProtocolRegistry.class.getDeclaredField("packetIdToSupplier");
      packetIdToSupplierField.setAccessible(true);

      Field packetClassToIdField = StateRegistry.PacketRegistry.ProtocolRegistry.class.getDeclaredField("packetClassToId");
      packetClassToIdField.setAccessible(true);

      serverConnField = BackendPlaySessionHandler.class.getDeclaredField("serverConn");
      serverConnField.setAccessible(true);

      ((Map<ProtocolVersion, StateRegistry.PacketRegistry.ProtocolRegistry>)
          versionsField.get(StateRegistry.PLAY.clientbound)).forEach((version, registry) -> {
            try {
              IntObjectMap<Supplier<? extends MinecraftPacket>> packetIdToSupplier
                  = (IntObjectMap<Supplier<? extends MinecraftPacket>>) packetIdToSupplierField.get(registry);

              Object2IntMap<Class<? extends MinecraftPacket>> packetClassToId
                  = (Object2IntMap<Class<? extends MinecraftPacket>>) packetClassToIdField.get(registry);

              int packetId = packetClassToId.getInt(PlayerListItem.class);
              packetClassToId.put(PlayerListItemHook.class, packetId);
              packetIdToSupplier.remove(packetId);
              packetIdToSupplier.put(packetId, () -> new PlayerListItemHook(plugin));
            } catch (IllegalAccessException e) {
              e.printStackTrace();
            }
          });
    } catch (NoSuchFieldException | IllegalAccessException e) {
      e.printStackTrace();
    }
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    if (handler instanceof BackendPlaySessionHandler) {
      try {
        List<Item> items = this.getItems();
        for (int i = 0; i < items.size(); ++i) {
          Item item = items.get(i);
          ConnectedPlayer player = ((VelocityServerConnection) serverConnField.get(handler)).getPlayer();
          UUID initialID = this.plugin.getInitialID(player);

          if (player.getUniqueId().equals(item.getUuid())) {
            items.set(i, new Item(initialID)
                .setDisplayName(item.getDisplayName())
                .setGameMode(item.getGameMode())
                .setLatency(item.getLatency())
                .setName(item.getName())
                .setProperties(item.getProperties()));
          }
        }
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      }
    }

    return super.handle(handler);
  }
}
