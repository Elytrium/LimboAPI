/*
 * Copyright (C) 2021 - 2023 Elytrium
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

import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.permission.PermissionsSetupEvent;
import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.permission.PermissionFunction;
import com.velocitypowered.api.permission.PermissionProvider;
import com.velocitypowered.api.proxy.InboundConnection;
import com.velocitypowered.api.proxy.crypto.IdentifiedKey;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.AuthSessionHandler;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.connection.client.InitialConnectSessionHandler;
import com.velocitypowered.proxy.crypto.IdentifiedKeyImpl;
import com.velocitypowered.proxy.network.Connections;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.LegacyPlayerListItem;
import com.velocitypowered.proxy.protocol.packet.UpsertPlayerInfo;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoop;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import net.elytrium.commons.utils.reflection.ReflectionException;
import net.elytrium.limboapi.LimboAPI;
import net.elytrium.limboapi.api.event.SafeGameProfileRequestEvent;
import net.elytrium.limboapi.injection.event.EventManagerHook;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

public class LoginTasksQueue {

  private static final MethodHandle PROFILE_FIELD;
  private static final Field DEFAULT_PERMISSIONS_FIELD;
  private static final MethodHandle SET_PERMISSION_FUNCTION_METHOD;
  private static final MethodHandle INITIAL_CONNECT_SESSION_HANDLER_CONSTRUCTOR;
  private static final Field MC_CONNECTION_FIELD;
  private static final MethodHandle CONNECT_TO_INITIAL_SERVER_METHOD;
  private static final MethodHandle PLAYER_KEY_FIELD;

  private final LimboAPI plugin;
  private final Object handler;
  private final VelocityServer server;
  private final ConnectedPlayer player;
  private final InboundConnection inbound;
  private final Queue<Runnable> queue;

  public LoginTasksQueue(LimboAPI plugin, Object handler, VelocityServer server, ConnectedPlayer player,
      InboundConnection inbound, Queue<Runnable> queue) {
    this.plugin = plugin;
    this.handler = handler;
    this.server = server;
    this.player = player;
    this.inbound = inbound;
    this.queue = queue;
  }

  public void next() {
    MinecraftConnection connection = this.player.getConnection();
    if (connection.getChannel().isActive()) {
      EventLoop eventLoop = connection.eventLoop();
      if (this.queue.size() == 0) {
        eventLoop.execute(this::finish);
      } else {
        eventLoop.execute(Objects.requireNonNull(this.queue.poll()));
      }
    }
  }

  @SuppressWarnings("deprecation")
  private void finish() {
    this.plugin.removeLoginQueue(this.player);

    EventManagerHook eventManager = (EventManagerHook) this.server.getEventManager();
    MinecraftConnection connection = this.player.getConnection();
    Logger logger = LimboAPI.getLogger();

    eventManager.proceedProfile(this.player.getGameProfile());
    eventManager.fire(new GameProfileRequestEvent(this.inbound, this.player.getGameProfile(), this.player.isOnlineMode())).thenAcceptAsync(
        gameProfile -> eventManager.fire(new SafeGameProfileRequestEvent(gameProfile.getGameProfile(), gameProfile.isOnlineMode()))
            .thenAcceptAsync(safeGameProfile -> {
              try {
                if (connection.getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_19_1) <= 0) {
                  connection.delayedWrite(new LegacyPlayerListItem(
                      LegacyPlayerListItem.REMOVE_PLAYER,
                      List.of(new LegacyPlayerListItem.Item(this.player.getUniqueId()))
                  ));

                  connection.delayedWrite(new LegacyPlayerListItem(
                      LegacyPlayerListItem.ADD_PLAYER,
                      List.of(
                          new LegacyPlayerListItem.Item(this.player.getUniqueId())
                              .setName(safeGameProfile.getUsername())
                              .setProperties(safeGameProfile.getGameProfile().getProperties())
                      )
                  ));
                } else {
                  UpsertPlayerInfo.Entry playerInfoEntry = new UpsertPlayerInfo.Entry(this.player.getUniqueId());
                  playerInfoEntry.setDisplayName(Component.text(safeGameProfile.getUsername()));
                  playerInfoEntry.setProfile(safeGameProfile.getGameProfile());

                  connection.delayedWrite(new UpsertPlayerInfo(
                      EnumSet.of(
                          UpsertPlayerInfo.Action.UPDATE_DISPLAY_NAME,
                          UpsertPlayerInfo.Action.ADD_PLAYER),
                      List.of(playerInfoEntry)));
                }

                PROFILE_FIELD.invokeExact(this.player, safeGameProfile.getGameProfile());

                // From Velocity.
                eventManager
                    .fire(new PermissionsSetupEvent(this.player, (PermissionProvider) DEFAULT_PERMISSIONS_FIELD.get(null)))
                    .thenAcceptAsync(event -> {
                      if (!connection.isClosed()) {
                        // Wait for permissions to load, then set the players' permission function.
                        PermissionFunction function = event.createFunction(this.player);
                        if (function == null) {
                          logger.error(
                              "A plugin permission provider {} provided an invalid permission function"
                                  + " for player {}. This is a bug in the plugin, not in Velocity. Falling"
                                  + " back to the default permission function.",
                              event.getProvider().getClass().getName(),
                              this.player.getUsername()
                          );
                        } else {
                          try {
                            SET_PERMISSION_FUNCTION_METHOD.invokeExact(this.player, function);
                          } catch (Throwable ex) {
                            logger.error("Exception while completing injection to {}", this.player, ex);
                          }
                        }
                        try {
                          this.initialize(connection);
                        } catch (Throwable e) {
                          throw new ReflectionException(e);
                        }
                      }
                    }, connection.eventLoop());
              } catch (Throwable e) {
                logger.error("Exception while completing injection to {}", this.player, e);
              }
            }, connection.eventLoop()),
        connection.eventLoop()
    );
  }

  // From Velocity.
  @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
  private void initialize(MinecraftConnection connection) throws Throwable {
    connection.setAssociation(this.player);
    connection.setState(StateRegistry.PLAY);

    ChannelPipeline pipeline = connection.getChannel().pipeline();
    this.plugin.deject3rdParty(pipeline);

    if (pipeline.get(Connections.FRAME_ENCODER) == null) {
      this.plugin.fixCompressor(pipeline, connection.getProtocolVersion());
    }

    if (this.player.getIdentifiedKey() != null) {
      IdentifiedKey playerKey = this.player.getIdentifiedKey();
      if (playerKey.getSignatureHolder() == null) {
        if (playerKey instanceof IdentifiedKeyImpl) {
          IdentifiedKeyImpl unlinkedKey = (IdentifiedKeyImpl) playerKey;
          // Failsafe
          if (!unlinkedKey.internalAddHolder(this.player.getUniqueId())) {
            PLAYER_KEY_FIELD.invokeExact(this.player, (IdentifiedKey) null);
          }
        }
      } else {
        if (!Objects.equals(playerKey.getSignatureHolder(), this.player.getUniqueId())) {
          PLAYER_KEY_FIELD.invokeExact(this.player, (IdentifiedKey) null);
        }
      }
    }

    Logger logger = LimboAPI.getLogger();
    this.server.getEventManager().fire(new LoginEvent(this.player)).thenAcceptAsync(event -> {
      if (connection.isClosed()) {
        // The player was disconnected.
        this.server.getEventManager().fireAndForget(new DisconnectEvent(this.player, DisconnectEvent.LoginStatus.CANCELLED_BY_USER_BEFORE_COMPLETE));
      } else {
        Optional<Component> reason = event.getResult().getReasonComponent();
        if (reason.isPresent()) {
          this.player.disconnect0(reason.get(), true);
        } else {
          if (this.server.registerConnection(this.player)) {
            try {
              connection.setActiveSessionHandler(connection.getState(),
                  (InitialConnectSessionHandler) INITIAL_CONNECT_SESSION_HANDLER_CONSTRUCTOR.invokeExact(this.player, this.server));
              this.server.getEventManager().fire(new PostLoginEvent(this.player)).thenAccept(postLoginEvent -> {
                try {
                  MC_CONNECTION_FIELD.set(this.handler, connection);
                  CONNECT_TO_INITIAL_SERVER_METHOD.invoke((AuthSessionHandler) this.handler, this.player);
                } catch (Throwable e) {
                  throw new ReflectionException(e);
                }
              });
            } catch (Throwable e) {
              throw new ReflectionException(e);
            }
          } else {
            this.player.disconnect0(Component.translatable("velocity.error.already-connected-proxy"), true);
          }
        }
      }
    }, connection.eventLoop()).exceptionally(t -> {
      logger.error("Exception while completing login initialisation phase for {}", this.player, t);
      return null;
    });
  }

  static {
    try {
      PROFILE_FIELD = MethodHandles.privateLookupIn(ConnectedPlayer.class, MethodHandles.lookup())
          .findSetter(ConnectedPlayer.class, "profile", GameProfile.class);

      DEFAULT_PERMISSIONS_FIELD = ConnectedPlayer.class.getDeclaredField("DEFAULT_PERMISSIONS");
      DEFAULT_PERMISSIONS_FIELD.setAccessible(true);

      SET_PERMISSION_FUNCTION_METHOD = MethodHandles.privateLookupIn(ConnectedPlayer.class, MethodHandles.lookup())
          .findVirtual(ConnectedPlayer.class, "setPermissionFunction", MethodType.methodType(void.class, PermissionFunction.class));

      INITIAL_CONNECT_SESSION_HANDLER_CONSTRUCTOR = MethodHandles
          .privateLookupIn(InitialConnectSessionHandler.class, MethodHandles.lookup())
          .findConstructor(InitialConnectSessionHandler.class, MethodType.methodType(void.class, ConnectedPlayer.class, VelocityServer.class));

      CONNECT_TO_INITIAL_SERVER_METHOD = MethodHandles.privateLookupIn(AuthSessionHandler.class, MethodHandles.lookup())
          .findVirtual(AuthSessionHandler.class, "connectToInitialServer", MethodType.methodType(CompletableFuture.class, ConnectedPlayer.class));

      MC_CONNECTION_FIELD = AuthSessionHandler.class.getDeclaredField("mcConnection");
      MC_CONNECTION_FIELD.setAccessible(true);

      PLAYER_KEY_FIELD = MethodHandles.privateLookupIn(ConnectedPlayer.class, MethodHandles.lookup())
          .findSetter(ConnectedPlayer.class, "playerKey", IdentifiedKey.class);
    } catch (NoSuchFieldException | NoSuchMethodException | IllegalAccessException e) {
      throw new ReflectionException(e);
    }
  }
}
