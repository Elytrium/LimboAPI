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

import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.permission.PermissionsSetupEvent;
import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import com.velocitypowered.api.permission.PermissionFunction;
import com.velocitypowered.api.permission.PermissionProvider;
import com.velocitypowered.api.proxy.InboundConnection;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.AuthSessionHandler;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.connection.client.InitialConnectSessionHandler;
import com.velocitypowered.proxy.event.VelocityEventManager;
import com.velocitypowered.proxy.network.Connections;
import com.velocitypowered.proxy.protocol.StateRegistry;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoop;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import net.elytrium.java.commons.reflection.ReflectionException;
import net.elytrium.limboapi.LimboAPI;
import net.elytrium.limboapi.api.event.SafeGameProfileRequestEvent;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

public class LoginTasksQueue {

  private static final Field PROFILE_FIELD;
  private static final Field DEFAULT_PERMISSIONS_FIELD;
  private static final Method SET_PERMISSION_FUNCTION_METHOD;
  private static final Constructor<InitialConnectSessionHandler> INITIAL_CONNECT_SESSION_HANDLER_CONSTRUCTOR;
  private static final Field MC_CONNECTION_FIELD;
  private static final Method CONNECT_TO_INITIAL_SERVER_METHOD;

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

    VelocityEventManager eventManager = this.server.getEventManager();
    MinecraftConnection connection = this.player.getConnection();
    Logger logger = LimboAPI.getLogger();
    eventManager.fire(new GameProfileRequestEvent(this.inbound, this.player.getGameProfile(), this.player.isOnlineMode())).thenAcceptAsync(
        gameProfile -> eventManager.fire(new SafeGameProfileRequestEvent(gameProfile.getGameProfile(), gameProfile.isOnlineMode()))
            .thenAcceptAsync(safeGameProfile -> {
              try {
                PROFILE_FIELD.set(this.player, safeGameProfile.getGameProfile());

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
                            SET_PERMISSION_FUNCTION_METHOD.invoke(this.player, function);
                          } catch (IllegalAccessException | InvocationTargetException ex) {
                            logger.error("Exception while completing injection to {}", this.player, ex);
                          }
                        }
                        try {
                          this.initialize(connection);
                        } catch (IllegalAccessException e) {
                          e.printStackTrace();
                        }
                      }
                    }, connection.eventLoop());
              } catch (IllegalAccessException e) {
                logger.error("Exception while completing injection to {}", this.player, e);
              }
            }, connection.eventLoop()),
        connection.eventLoop()
    );
  }

  // From Velocity.
  private void initialize(MinecraftConnection connection) throws IllegalAccessException {
    connection.setAssociation(this.player);
    connection.setState(StateRegistry.PLAY);

    ChannelPipeline pipeline = connection.getChannel().pipeline();
    this.plugin.deject3rdParty(pipeline);

    if (!pipeline.names().contains(Connections.FRAME_ENCODER) && !pipeline.names().contains(Connections.COMPRESSION_ENCODER)) {
      this.plugin.fixCompressor(pipeline, connection.getProtocolVersion());
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
              connection.setSessionHandler(INITIAL_CONNECT_SESSION_HANDLER_CONSTRUCTOR.newInstance(this.player));
              this.server.getEventManager().fire(new PostLoginEvent(this.player)).thenAccept(postLoginEvent -> {
                try {
                  MC_CONNECTION_FIELD.set(this.handler, connection);
                  CONNECT_TO_INITIAL_SERVER_METHOD.invoke(this.handler, this.player);
                } catch (IllegalAccessException | InvocationTargetException e) {
                  logger.error("Exception while connecting {} to initial server", this.player, e);
                }
              });
            } catch (InstantiationException | InvocationTargetException | IllegalAccessException e) {
              e.printStackTrace();
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
      PROFILE_FIELD = ConnectedPlayer.class.getDeclaredField("profile");
      PROFILE_FIELD.setAccessible(true);

      DEFAULT_PERMISSIONS_FIELD = ConnectedPlayer.class.getDeclaredField("DEFAULT_PERMISSIONS");
      DEFAULT_PERMISSIONS_FIELD.setAccessible(true);

      SET_PERMISSION_FUNCTION_METHOD = ConnectedPlayer.class.getDeclaredMethod("setPermissionFunction", PermissionFunction.class);
      SET_PERMISSION_FUNCTION_METHOD.setAccessible(true);

      INITIAL_CONNECT_SESSION_HANDLER_CONSTRUCTOR = InitialConnectSessionHandler.class.getDeclaredConstructor(ConnectedPlayer.class);
      INITIAL_CONNECT_SESSION_HANDLER_CONSTRUCTOR.setAccessible(true);

      MC_CONNECTION_FIELD = AuthSessionHandler.class.getDeclaredField("mcConnection");
      MC_CONNECTION_FIELD.setAccessible(true);

      CONNECT_TO_INITIAL_SERVER_METHOD = AuthSessionHandler.class.getDeclaredMethod("connectToInitialServer", ConnectedPlayer.class);
      CONNECT_TO_INITIAL_SERVER_METHOD.setAccessible(true);
    } catch (NoSuchFieldException | NoSuchMethodException e) {
      throw new ReflectionException(e);
    }
  }
}
