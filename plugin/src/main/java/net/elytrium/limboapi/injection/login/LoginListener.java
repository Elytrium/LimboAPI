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

package net.elytrium.limboapi.injection.login;

import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_8;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.api.util.UuidUtils;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.config.PlayerInfoForwarding;
import com.velocitypowered.proxy.config.VelocityConfiguration;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.ClientPlaySessionHandler;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.connection.client.InitialInboundConnection;
import com.velocitypowered.proxy.connection.client.LoginInboundConnection;
import com.velocitypowered.proxy.connection.client.LoginSessionHandler;
import com.velocitypowered.proxy.protocol.packet.ServerLoginSuccess;
import com.velocitypowered.proxy.protocol.packet.SetCompression;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.elytrium.limboapi.LimboAPI;
import net.elytrium.limboapi.api.event.LoginLimboRegisterEvent;
import net.elytrium.limboapi.injection.dummy.ClosedChannel;
import net.elytrium.limboapi.injection.dummy.ClosedMinecraftConnection;
import net.elytrium.limboapi.injection.dummy.DummyEventPool;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class LoginListener {

  private static final ClosedMinecraftConnection closed;

  private static final Constructor<ConnectedPlayer> ctor;
  private static final Field loginConnectionField;
  private static final Field delegate;
  private static final Field spawned;

  private final LimboAPI plugin;
  private final VelocityServer server;
  private final List<String> onlineMode = new ArrayList<>();

  public LoginListener(LimboAPI plugin, VelocityServer server) {
    this.plugin = plugin;
    this.server = server;
  }

  static {
    closed = new ClosedMinecraftConnection(new ClosedChannel(new DummyEventPool()), null);

    try {
      ctor = ConnectedPlayer.class.getDeclaredConstructor(
          VelocityServer.class,
          GameProfile.class,
          MinecraftConnection.class,
          InetSocketAddress.class,
          boolean.class
      );
      ctor.setAccessible(true);

      delegate = LoginInboundConnection.class.getDeclaredField("delegate");
      delegate.setAccessible(true);

      loginConnectionField = LoginSessionHandler.class.getDeclaredField("mcConnection");
      loginConnectionField.setAccessible(true);

      spawned = ClientPlaySessionHandler.class.getDeclaredField("spawned");
      spawned.setAccessible(true);
    } catch (NoSuchFieldException | NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
  }

  @Subscribe(order = PostOrder.LAST)
  public void hookPreLogin(PreLoginEvent event) {
    PreLoginEvent.PreLoginComponentResult result = event.getResult();
    if (!result.isForceOfflineMode() && (this.server.getConfiguration().isOnlineMode() || result.isOnlineModeAllowed())) {
      this.onlineMode.add(event.getUsername());
    }
  }

  @Subscribe
  public void onDisconnect(DisconnectEvent event) {
    this.plugin.removeLoginQueue(event.getPlayer());
    this.onlineMode.remove(event.getPlayer().getUsername());
  }

  public void hookLoginSession(GameProfileRequestEvent event) throws IllegalAccessException {
    // Changing mcConnection to the closed one. For what? To break the "initializePlayer"
    // method (which checks mcConnection.isActive()) and to override it. :)
    InitialInboundConnection inbound = (InitialInboundConnection) delegate.get(event.getConnection());
    MinecraftConnection connection = inbound.getConnection();
    LoginSessionHandler handler = (LoginSessionHandler) connection.getSessionHandler();
    loginConnectionField.set(handler, closed);
    if (connection.isClosed()) {
      return;
    }

    connection.eventLoop().execute(() -> {
      try {
        // Initiate a regular connection and move over to it.
        ConnectedPlayer player = ctor.newInstance(
            this.server, event.getGameProfile(), connection,
            event.getConnection().getVirtualHost().orElse(null),
            this.onlineMode.contains(event.getUsername())
        );

        if (!this.server.canRegisterConnection(player)) {
          player.disconnect0(Component.translatable("velocity.error.already-connected-proxy", NamedTextColor.RED), true);
          return;
        }

        if (connection.isClosed()) {
          return;
        }

        // Completing the Login process
        int threshold = this.server.getConfiguration().getCompressionThreshold();
        if (threshold >= 0 && connection.getProtocolVersion().compareTo(MINECRAFT_1_8) >= 0) {
          connection.write(new SetCompression(threshold));
          connection.setCompressionThreshold(threshold);
        }

        VelocityConfiguration configuration = this.server.getConfiguration();
        UUID playerUniqueId = player.getUniqueId();
        if (configuration.getPlayerInfoForwardingMode() == PlayerInfoForwarding.NONE) {
          playerUniqueId = UuidUtils.generateOfflinePlayerUuid(player.getUsername());
        }

        ServerLoginSuccess success = new ServerLoginSuccess();
        success.setUsername(player.getUsername());
        success.setUuid(playerUniqueId);
        connection.write(success);

        this.server.getEventManager()
            .fire(new LoginLimboRegisterEvent(player))
            .thenAcceptAsync(limboEvent -> {
              LoginTasksQueue queue = new LoginTasksQueue(this.plugin, handler, this.server, player, inbound, limboEvent.getCallbacks());

              this.plugin.addLoginQueue(player, queue);
              queue.next();
            }, connection.eventLoop());
      } catch (InstantiationException | IllegalAccessException | InvocationTargetException ex) {
        ex.printStackTrace();
      }
    });
  }

  @Subscribe
  public void hookPlaySession(ServerConnectedEvent event) {
    ConnectedPlayer player = (ConnectedPlayer) event.getPlayer();
    MinecraftConnection connection = player.getConnection();

    connection.eventLoop().execute(() -> {
      if (!(connection.getSessionHandler() instanceof ClientPlaySessionHandler)) {
        ClientPlaySessionHandler playHandler = new ClientPlaySessionHandler(this.server, player);
        try {
          spawned.set(playHandler, this.plugin.isLimboJoined(player));
        } catch (IllegalAccessException ex) {
          this.plugin.getLogger().error("Exception while hooking into ClientPlaySessionHandler of {}", player, ex);
        }

        connection.setSessionHandler(playHandler);
      }
    });
  }
}
