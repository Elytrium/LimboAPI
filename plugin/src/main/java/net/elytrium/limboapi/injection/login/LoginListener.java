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
import com.velocitypowered.proxy.crypto.IdentifiedKeyImpl;
import com.velocitypowered.proxy.network.Connections;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.VelocityConnectionEvent;
import com.velocitypowered.proxy.protocol.packet.ServerLoginSuccessPacket;
import com.velocitypowered.proxy.protocol.packet.SetCompressionPacket;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.elytrium.commons.utils.reflection.ReflectionException;
import net.elytrium.limboapi.LimboAPI;
import net.elytrium.limboapi.api.event.LoginLimboRegisterEvent;
import net.elytrium.limboapi.injection.dummy.ClosedChannel;
import net.elytrium.limboapi.injection.dummy.ClosedMinecraftConnection;
import net.elytrium.limboapi.injection.dummy.DummyEventPool;
import net.elytrium.limboapi.injection.login.confirmation.LoginConfirmHandler;
import net.elytrium.limboapi.injection.packet.ServerLoginSuccessHook;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class LoginListener {

  private static final ClosedMinecraftConnection CLOSED_MINECRAFT_CONNECTION;

  private static final MethodHandle DELEGATE_FIELD;
  private static final Field MC_CONNECTION_FIELD;
  private static final MethodHandle CONNECTED_PLAYER_CONSTRUCTOR;
  private static final MethodHandle SPAWNED_FIELD;

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

    this.plugin.setLimboJoined(event.getPlayer());
  }

  @Subscribe
  public void onDisconnect(DisconnectEvent event) {
    this.onlineMode.remove(event.getPlayer().getUsername());
  }

  @SuppressWarnings("ConstantConditions")
  public void hookLoginSession(GameProfileRequestEvent event) throws Throwable {
    LoginInboundConnection inboundConnection = (LoginInboundConnection) event.getConnection();
    // In some cases, e.g. if the player logged out or was kicked right before the GameProfileRequestEvent hook,
    // the connection will be broken (possibly by GC) and we can't get it from the delegate field.
    if (LoginInboundConnection.class.isAssignableFrom(inboundConnection.getClass())) {
      // Changing mcConnection to the closed one. For what? To break the "initializePlayer"
      // method (which checks mcConnection.isActive()) and to override it. :)
      InitialInboundConnection inbound = (InitialInboundConnection) DELEGATE_FIELD.invokeExact(inboundConnection);
      MinecraftConnection connection = inbound.getConnection();
      Object handler = connection.getActiveSessionHandler();
      MC_CONNECTION_FIELD.set(handler, CLOSED_MINECRAFT_CONNECTION);

      if (connection.getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_20_2) >= 0) {
        connection.setActiveSessionHandler(StateRegistry.LOGIN, new LoginConfirmHandler(this.plugin, connection));
      }

      // From Velocity.
      if (!connection.isClosed()) {
        connection.eventLoop().execute(() -> {
          try {
            IdentifiedKey playerKey = inboundConnection.getIdentifiedKey();
            if (playerKey != null) {
              if (playerKey.getSignatureHolder() == null) {
                if (playerKey instanceof IdentifiedKeyImpl unlinkedKey) {
                  // Failsafe
                  if (!unlinkedKey.internalAddHolder(event.getGameProfile().getId())) {
                    playerKey = null;
                  }
                }
              } else if (!Objects.equals(playerKey.getSignatureHolder(), event.getGameProfile().getId())) {
                playerKey = null;
              }
            }

            // Initiate a regular connection and move over to it.
            ConnectedPlayer player = (ConnectedPlayer) CONNECTED_PLAYER_CONSTRUCTOR.invokeExact(
                this.server,
                event.getGameProfile(),
                connection,
                inboundConnection.getVirtualHost().orElse(null),
                this.onlineMode.contains(event.getUsername()),
                playerKey
            );
            if (connection.getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_20_2) >= 0) {
              ((LoginConfirmHandler) connection.getActiveSessionHandler()).setPlayer(player);
            }
            if (this.server.canRegisterConnection(player)) {
              if (!connection.isClosed()) {
                // Complete the Login process.
                int threshold = this.server.getConfiguration().getCompressionThreshold();
                ChannelPipeline pipeline = connection.getChannel().pipeline();
                boolean compressionEnabled = threshold >= 0 && connection.getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_8) >= 0;
                if (compressionEnabled) {
                  connection.write(new SetCompressionPacket(threshold));
                  this.plugin.fixDecompressor(pipeline, threshold, true);
                  pipeline.addFirst(Connections.COMPRESSION_ENCODER, new ChannelOutboundHandlerAdapter());
                }
                pipeline.remove(Connections.FRAME_ENCODER);

                this.plugin.inject3rdParty(player, connection, pipeline);
                if (compressionEnabled) {
                  pipeline.fireUserEventTriggered(VelocityConnectionEvent.COMPRESSION_ENABLED);
                }

                VelocityConfiguration configuration = this.server.getConfiguration();
                UUID playerUniqueID = player.getUniqueId();
                if (configuration.getPlayerInfoForwardingMode() == PlayerInfoForwarding.NONE) {
                  playerUniqueID = UuidUtils.generateOfflinePlayerUuid(player.getUsername());
                }

                ServerLoginSuccessHook successHook = new ServerLoginSuccessHook();
                successHook.setUsername(player.getUsername());
                successHook.setProperties(player.getGameProfileProperties());
                successHook.setUuid(playerUniqueID);
                connection.write(successHook);

                ServerLoginSuccessPacket success = new ServerLoginSuccessPacket();
                success.setUsername(player.getUsername());
                success.setProperties(player.getGameProfileProperties());
                success.setUuid(playerUniqueID);

                ChannelHandler compressionHandler = pipeline.get(Connections.COMPRESSION_ENCODER);
                if (compressionHandler != null) {
                  connection.write(this.plugin.encodeSingleLogin(success, connection.getProtocolVersion()));
                } else {
                  ChannelHandler frameHandler = pipeline.get(Connections.FRAME_ENCODER);
                  if (frameHandler != null) {
                    pipeline.remove(frameHandler);
                  }

                  connection.write(this.plugin.encodeSingleLoginUncompressed(success, connection.getProtocolVersion()));
                }

                this.plugin.setInitialID(player, playerUniqueID);

                if (connection.getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_20_2) >= 0) {
                  ((LoginConfirmHandler) connection.getActiveSessionHandler())
                      .thenRun(() -> this.fireRegisterEvent(player, connection, inbound, handler));
                } else {
                  connection.setState(StateRegistry.PLAY);
                  this.fireRegisterEvent(player, connection, inbound, handler);
                }
              }
            } else {
              player.disconnect0(Component.translatable("velocity.error.already-connected-proxy", NamedTextColor.RED), true);
            }
          } catch (Throwable e) {
            throw new ReflectionException(e);
          }
        });
      }
    }
  }

  private void fireRegisterEvent(ConnectedPlayer player, MinecraftConnection connection,
      InitialInboundConnection inbound, Object handler) {
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

  @Subscribe
  public void hookPlaySession(ServerConnectedEvent event) {
    ConnectedPlayer player = (ConnectedPlayer) event.getPlayer();
    MinecraftConnection connection = player.getConnection();

    // 1.20.2+ can ignore this, as it should be despawned by default
    if (connection.getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_20_2) >= 0) {
      return;
    }

    connection.eventLoop().execute(() -> {
      if (!(connection.getActiveSessionHandler() instanceof ClientPlaySessionHandler)) {
        try {
          ClientPlaySessionHandler playHandler = new ClientPlaySessionHandler(this.server, player);
          SPAWNED_FIELD.invokeExact(playHandler, this.plugin.isLimboJoined(player));
          connection.setActiveSessionHandler(connection.getState(), playHandler);
        } catch (Throwable e) {
          throw new ReflectionException(e);
        }
      }
    });
  }

  static {
    CLOSED_MINECRAFT_CONNECTION = new ClosedMinecraftConnection(new ClosedChannel(new DummyEventPool()), null);

    try {
      CONNECTED_PLAYER_CONSTRUCTOR = MethodHandles.privateLookupIn(ConnectedPlayer.class, MethodHandles.lookup())
          .findConstructor(ConnectedPlayer.class,
              MethodType.methodType(
                  void.class,
                  VelocityServer.class,
                  GameProfile.class,
                  MinecraftConnection.class,
                  InetSocketAddress.class,
                  boolean.class,
                  IdentifiedKey.class
              )
          );

      DELEGATE_FIELD = MethodHandles.privateLookupIn(LoginInboundConnection.class, MethodHandles.lookup())
          .findGetter(LoginInboundConnection.class, "delegate", InitialInboundConnection.class);

      MC_CONNECTION_FIELD = AuthSessionHandler.class.getDeclaredField("mcConnection");
      MC_CONNECTION_FIELD.setAccessible(true);

      SPAWNED_FIELD = MethodHandles.privateLookupIn(ClientPlaySessionHandler.class, MethodHandles.lookup())
          .findSetter(ClientPlaySessionHandler.class, "spawned", boolean.class);
    } catch (NoSuchFieldException | NoSuchMethodException | IllegalAccessException e) {
      throw new ReflectionException(e);
    }
  }
}
