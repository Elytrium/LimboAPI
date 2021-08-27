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
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import net.elytrium.limboapi.LimboAPI;
import net.elytrium.limboapi.api.event.LoginLimboRegisterEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

@RequiredArgsConstructor
public class LoginListener {

  private static final ClosedMinecraftConnection closed;
  private static Constructor<ConnectedPlayer> ctor;
  private static Field craftConnectionField;
  private static Field loginConnectionField;
  private static Field spawned;

  static {
    closed = new ClosedMinecraftConnection(new ClosedChannel(), LimboAPI.getInstance().getServer());

    try {
      ctor = ConnectedPlayer.class.getDeclaredConstructor(
          VelocityServer.class, GameProfile.class, MinecraftConnection.class, InetSocketAddress.class, boolean.class);
      ctor.setAccessible(true);

      craftConnectionField = InitialInboundConnection.class.getDeclaredField("connection");
      craftConnectionField.setAccessible(true);

      loginConnectionField = LoginSessionHandler.class.getDeclaredField("mcConnection");
      loginConnectionField.setAccessible(true);

      spawned = ClientPlaySessionHandler.class.getDeclaredField("spawned");
      spawned.setAccessible(true);
    } catch (NoSuchFieldException | NoSuchMethodException e) {
      e.printStackTrace();
    }
  }

  private final LimboAPI limboAPI;
  private final VelocityServer server;
  private final List<String> onlineMode = new ArrayList<>();

  @Subscribe(order = PostOrder.LAST)
  public void hookPreLogin(PreLoginEvent e) {
    PreLoginEvent.PreLoginComponentResult result = e.getResult();
    if (!result.isForceOfflineMode() && (this.server.getConfiguration().isOnlineMode() || result
        .isOnlineModeAllowed())) {
      this.onlineMode.add(e.getUsername());
    }
  }

  @Subscribe
  public void onDisconnect(DisconnectEvent e) {
    this.limboAPI.removeQueue(e.getPlayer());
  }

  @SneakyThrows
  @Subscribe(order = PostOrder.LAST)
  public void hookLoginSession(GameProfileRequestEvent e) {
    // Changing mcConnection to the closed one. For what? To break the "initializePlayer"
    // method (which checks mcConnection.isActive()) and to override it. :)
    MinecraftConnection connection = (MinecraftConnection) craftConnectionField.get(e.getConnection());
    LoginSessionHandler handler = (LoginSessionHandler) connection.getSessionHandler();
    loginConnectionField.set(handler, closed);

    connection.eventLoop().execute(() -> {
      try {
        // Initiate a regular connection and move over to it.
        ConnectedPlayer player = ctor.newInstance(
            this.server, e.getGameProfile(), connection,
            e.getConnection().getVirtualHost().orElse(null),
            this.onlineMode.contains(e.getUsername()));

        if (!this.server.canRegisterConnection(player)) {
          // TODO: Prepare this packet. Хм. Или не нужно?
          player.disconnect0(Component.translatable("velocity.error.already-connected-proxy",
              NamedTextColor.RED), true);
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
              LoginTasksQueue queue =
                  new LoginTasksQueue(this.limboAPI, this.server, player, limboEvent.getCallbacks());

              this.limboAPI.addQueue(player, queue);
              queue.next();
            }, connection.eventLoop());
      } catch (InstantiationException | IllegalAccessException | InvocationTargetException ex) {
        this.limboAPI.getLogger()
            .error("Exception while completing injection to {}", e.getUsername(), ex);
      }
    });
  }

  @Subscribe
  public void hookPlaySession(ServerConnectedEvent e) {
    ConnectedPlayer player = (ConnectedPlayer) e.getPlayer();
    MinecraftConnection connection = player.getConnection();

    connection.eventLoop().execute(() -> {
      if (!(connection.getSessionHandler() instanceof ClientPlaySessionHandler)) {
        ClientPlaySessionHandler playHandler = new ClientPlaySessionHandler(this.server, player);
        try {
          spawned.set(playHandler, this.limboAPI.isLimboJoined(player));
        } catch (IllegalAccessException ex) {
          this.limboAPI.getLogger()
              .error("Exception while hooking into ClientPlaySessionHandler of {}", player, ex);
        }
        connection.setSessionHandler(playHandler);
      }
    });
  }
}
