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

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.crypto.IdentifiedKey;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.api.util.UuidUtils;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.config.PlayerInfoForwarding;
import com.velocitypowered.proxy.config.VelocityConfiguration;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.AuthSessionHandler;
import com.velocitypowered.proxy.connection.client.ClientPlaySessionHandler;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.connection.client.InitialInboundConnection;
import com.velocitypowered.proxy.connection.client.LoginInboundConnection;
import com.velocitypowered.proxy.network.Connections;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.ServerLoginSuccess;
import com.velocitypowered.proxy.protocol.packet.SetCompression;
import io.netty.channel.ChannelPipeline;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.elytrium.java.commons.reflection.ReflectionException;
import net.elytrium.limboapi.LimboAPI;
import net.elytrium.limboapi.api.event.LoginLimboRegisterEvent;
import net.elytrium.limboapi.injection.dummy.ClosedChannel;
import net.elytrium.limboapi.injection.dummy.ClosedMinecraftConnection;
import net.elytrium.limboapi.injection.dummy.DummyEventPool;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class LoginListener {

  private static final ClosedMinecraftConnection CLOSED_MINECRAFT_CONNECTION;

  private static final Field DELEGATE_FIELD;
  private static final Field MC_CONNECTION_FIELD;
  private static final Constructor<ConnectedPlayer> CONNECTED_PLAYER_CONSTRUCTOR;
  private static final Field SPAWNED_FIELD;

  private final List<String> onlineMode = new ArrayList<>();
  private final LimboAPI plugin;
  private final VelocityServer server;

  public LoginListener(LimboAPI plugin, VelocityServer server) {
    this.plugin = plugin;
    this.server = server;
  }

  @Subscribe(order = PostOrder.LAST)
  public void hookPreLogin(PreLoginEvent event) {
    PreLoginEvent.PreLoginComponentResult result = event.getResult();
    if (!result.isForceOfflineMode() && (this.server.getConfiguration().isOnlineMode() || result.isOnlineModeAllowed())) {
      this.onlineMode.add(event.getUsername());
    }
  }

  @Subscribe
  public void hookInitialServer(PlayerChooseInitialServerEvent event) {
    if (this.plugin.hasNextServer(event.getPlayer())) {
      event.setInitialServer(this.plugin.getNextServer(event.getPlayer()));
    }
  }

  @Subscribe
  public void onDisconnect(DisconnectEvent event) {
    this.onlineMode.remove(event.getPlayer().getUsername());
  }

  @SuppressWarnings("ConstantConditions")
  public void hookLoginSession(GameProfileRequestEvent event) throws IllegalAccessException {
    LoginInboundConnection inboundConnection = (LoginInboundConnection) event.getConnection();
    // In some cases, e.g. if the player logged out or was kicked right before the GameProfileRequestEvent hook,
    // the connection will be broken (possibly by GC) and we can't get it from the delegate field.
    if (LoginInboundConnection.class.isAssignableFrom(inboundConnection.getClass())) {
      // Changing mcConnection to the closed one. For what? To break the "initializePlayer"
      // method (which checks mcConnection.isActive()) and to override it. :)
      InitialInboundConnection inbound = (InitialInboundConnection) DELEGATE_FIELD.get(inboundConnection);
      MinecraftConnection connection = inbound.getConnection();
      Object handler = connection.getSessionHandler();
      MC_CONNECTION_FIELD.set(handler, CLOSED_MINECRAFT_CONNECTION);

      // From Velocity.
      if (!connection.isClosed()) {
        connection.eventLoop().execute(() -> {
          try {
            // Initiate a regular connection and move over to it.
            ConnectedPlayer player = CONNECTED_PLAYER_CONSTRUCTOR.newInstance(
                this.server,
                event.getGameProfile(),
                connection,
                inboundConnection.getVirtualHost().orElse(null),
                this.onlineMode.contains(event.getUsername()),
                inboundConnection.getIdentifiedKey()
            );
            if (this.server.canRegisterConnection(player)) {
              if (!connection.isClosed()) {
                // Complete the Login process.
                int threshold = this.server.getConfiguration().getCompressionThreshold();
                ChannelPipeline pipeline = connection.getChannel().pipeline();
                if (threshold >= 0 && connection.getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_8) >= 0) {
                  connection.write(new SetCompression(threshold));
                  this.plugin.fixDecompressor(pipeline, threshold, true);
                }
                pipeline.remove(Connections.FRAME_ENCODER);

                this.plugin.inject3rdParty(player, connection, pipeline);

                VelocityConfiguration configuration = this.server.getConfiguration();
                UUID playerUniqueID = player.getUniqueId();
                if (configuration.getPlayerInfoForwardingMode() == PlayerInfoForwarding.NONE) {
                  playerUniqueID = UuidUtils.generateOfflinePlayerUuid(player.getUsername());
                }

                ServerLoginSuccess success = new ServerLoginSuccess();
                success.setUsername(player.getUsername());
                success.setProperties(player.getGameProfileProperties());
                success.setUuid(playerUniqueID);
                connection.write(this.plugin.encodeSingleLogin(success, connection.getProtocolVersion()));

                this.plugin.setInitialID(player, playerUniqueID);

                connection.setState(StateRegistry.PLAY);

                this.server.getEventManager().fire(new LoginLimboRegisterEvent(player)).thenAcceptAsync(limboRegisterEvent -> {
                  LoginTasksQueue queue = new LoginTasksQueue(this.plugin, handler, this.server, player, inbound, limboRegisterEvent.getOnJoinCallbacks());
                  this.plugin.addLoginQueue(player, queue);
                  this.plugin.setKickCallback(player, limboRegisterEvent.getOnKickCallback());
                  queue.next();
                }, connection.eventLoop()).exceptionally(t -> {
                  LimboAPI.getLogger().error("Exception while registering LimboAPI login handlers for {}.", player, t);
                  return null;
                });
              }
            } else {
              player.disconnect0(Component.translatable("velocity.error.already-connected-proxy", NamedTextColor.RED), true);
            }
          } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
          }
        });
      }
    }
  }

  @Subscribe
  public void hookPlaySession(ServerConnectedEvent event) {
    ConnectedPlayer player = (ConnectedPlayer) event.getPlayer();
    MinecraftConnection connection = player.getConnection();

    connection.eventLoop().execute(() -> {
      if (!(connection.getSessionHandler() instanceof ClientPlaySessionHandler)) {
        ClientPlaySessionHandler playHandler = new ClientPlaySessionHandler(this.server, player);
        try {
          SPAWNED_FIELD.set(playHandler, this.plugin.isLimboJoined(player));
        } catch (IllegalAccessException e) {
          LimboAPI.getLogger().error("Exception while hooking into ClientPlaySessionHandler of {}.", player, e);
        }

        connection.setSessionHandler(playHandler);
      }
    });
  }

  static {
    CLOSED_MINECRAFT_CONNECTION = new ClosedMinecraftConnection(new ClosedChannel(new DummyEventPool()), null);

    try {
      CONNECTED_PLAYER_CONSTRUCTOR = ConnectedPlayer.class.getDeclaredConstructor(
          VelocityServer.class,
          GameProfile.class,
          MinecraftConnection.class,
          InetSocketAddress.class,
          boolean.class,
          IdentifiedKey.class
      );
      CONNECTED_PLAYER_CONSTRUCTOR.setAccessible(true);

      DELEGATE_FIELD = LoginInboundConnection.class.getDeclaredField("delegate");
      DELEGATE_FIELD.setAccessible(true);

      MC_CONNECTION_FIELD = AuthSessionHandler.class.getDeclaredField("mcConnection");
      MC_CONNECTION_FIELD.setAccessible(true);

      SPAWNED_FIELD = ClientPlaySessionHandler.class.getDeclaredField("spawned");
      SPAWNED_FIELD.setAccessible(true);
    } catch (NoSuchFieldException | NoSuchMethodException e) {
      throw new ReflectionException(e);
    }
  }
}
