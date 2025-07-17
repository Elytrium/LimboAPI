/*
 * Copyright (C) 2021 - 2025 Elytrium
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
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.backend.BackendPlaySessionHandler;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.UpsertPlayerInfoPacket;
import io.netty.buffer.ByteBuf;
import io.netty.util.collection.IntObjectMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import net.elytrium.commons.utils.reflection.ReflectionException;
import net.elytrium.limboapi.LimboAPI;
import net.elytrium.limboapi.protocol.LimboProtocol;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unchecked")
public class UpsertPlayerInfoHook implements MinecraftPacket {

  private static final MethodHandle SERVER_CONN_FIELD;

  static {
    try {
      SERVER_CONN_FIELD = MethodHandles.privateLookupIn(BackendPlaySessionHandler.class, MethodHandles.lookup())
          .findGetter(BackendPlaySessionHandler.class, "serverConn", VelocityServerConnection.class);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new ReflectionException(e);
    }
  }

  private final UpsertPlayerInfoPacket delegate = new UpsertPlayerInfoPacket();
  private final LimboAPI plugin;

  private UpsertPlayerInfoHook(LimboAPI plugin) {
    this.plugin = plugin;
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    if (handler instanceof BackendPlaySessionHandler backend) {
      try {
        ConnectedPlayer player = ((VelocityServerConnection) SERVER_CONN_FIELD.invokeExact(backend)).getPlayer();
        UUID initialID = this.plugin.getInitialID(player);
        List<UpsertPlayerInfoPacket.Entry> items = this.delegate.getEntries();

        for (int i = 0; i < items.size(); ++i) {
          UpsertPlayerInfoPacket.Entry item = items.get(i);

          if (player.getUniqueId().equals(item.getProfileId())) {
            UpsertPlayerInfoPacket.Entry fixedEntry = getFixedEntry(initialID, item, player);

            items.set(i, fixedEntry);
          }
        }
      } catch (Throwable e) {
        throw new ReflectionException(e);
      }
    }

    return this.delegate.handle(handler);
  }

  private static UpsertPlayerInfoPacket.@NotNull Entry getFixedEntry(UUID initialID, UpsertPlayerInfoPacket.Entry item, ConnectedPlayer player) {
    UpsertPlayerInfoPacket.Entry fixedEntry = new UpsertPlayerInfoPacket.Entry(initialID);
    fixedEntry.setDisplayName(item.getDisplayName());
    fixedEntry.setGameMode(item.getGameMode());
    fixedEntry.setLatency(item.getLatency());
    fixedEntry.setListed(item.isListed());
    fixedEntry.setListOrder(item.getListOrder());
    fixedEntry.setChatSession(item.getChatSession());

    if (item.getProfile() != null && item.getProfile().getId().equals(player.getUniqueId())) {
      fixedEntry.setProfile(new GameProfile(initialID, item.getProfile().getName(), item.getProfile().getProperties()));
    } else {
      fixedEntry.setProfile(item.getProfile());
    }

    return fixedEntry;
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    this.delegate.encode(buf, direction, version);
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    this.delegate.decode(buf, direction, version);
  }

  public static void init(LimboAPI plugin, StateRegistry.PacketRegistry registry) throws ReflectiveOperationException {
    // See LimboProtocol#overlayRegistry about var.
    var playProtocolRegistryVersions = (Map<ProtocolVersion, StateRegistry.PacketRegistry.ProtocolRegistry>) LimboProtocol.VERSIONS_FIELD.get(registry);
    playProtocolRegistryVersions.forEach((protocolVersion, protocolRegistry) -> {
      try {
        var packetIDToSupplier = (IntObjectMap<Supplier<? extends MinecraftPacket>>) LimboProtocol.PACKET_ID_TO_SUPPLIER_FIELD.get(protocolRegistry);
        var packetClassToID = (Object2IntMap<Class<? extends MinecraftPacket>>) LimboProtocol.PACKET_CLASS_TO_ID_FIELD.get(protocolRegistry);

        int id = packetClassToID.getInt(UpsertPlayerInfoPacket.class);
        packetClassToID.put(UpsertPlayerInfoHook.class, id);
        packetIDToSupplier.put(id, () -> new UpsertPlayerInfoHook(plugin));
      } catch (ReflectiveOperationException e) {
        throw new ReflectionException(e);
      }
    });
  }
}
