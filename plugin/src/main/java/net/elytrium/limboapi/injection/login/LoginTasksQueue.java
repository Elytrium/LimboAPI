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

import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.permission.PermissionsSetupEvent;
import com.velocitypowered.api.permission.PermissionFunction;
import com.velocitypowered.api.permission.PermissionProvider;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.connection.client.InitialConnectSessionHandler;
import com.velocitypowered.proxy.connection.client.LoginSessionHandler;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.netty.MinecraftDecoder;
import com.velocitypowered.proxy.protocol.netty.MinecraftEncoder;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.Queue;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import net.elytrium.limboapi.LimboAPI;
import net.kyori.adventure.text.Component;

@RequiredArgsConstructor
public class LoginTasksQueue {

  private static Constructor<InitialConnectSessionHandler> initialCtor;
  private static Field loginConnectionField;
  private static Field defaultPermissions;
  private static Field association;
  private static Field state;
  private static Method setPermissionFunction;
  private static Method connectToInitialServer;
  private final LimboAPI limboAPI;
  private final LoginSessionHandler handler;
  private final VelocityServer server;
  private final ConnectedPlayer player;
  private final Queue<Runnable> queue;

  static {
    try {
      initialCtor
          = InitialConnectSessionHandler.class.getDeclaredConstructor(ConnectedPlayer.class);
      initialCtor.setAccessible(true);

      loginConnectionField = LoginSessionHandler.class.getDeclaredField("mcConnection");
      loginConnectionField.setAccessible(true);

      defaultPermissions = ConnectedPlayer.class.getDeclaredField("DEFAULT_PERMISSIONS");
      defaultPermissions.setAccessible(true);

      association = MinecraftConnection.class.getDeclaredField("association");
      association.setAccessible(true);

      state = MinecraftConnection.class.getDeclaredField("state");
      state.setAccessible(true);

      setPermissionFunction = ConnectedPlayer.class
          .getDeclaredMethod("setPermissionFunction", PermissionFunction.class);
      setPermissionFunction.setAccessible(true);

      connectToInitialServer = LoginSessionHandler.class
          .getDeclaredMethod("connectToInitialServer", ConnectedPlayer.class);
      connectToInitialServer.setAccessible(true);
    } catch (NoSuchFieldException | NoSuchMethodException e) {
      e.printStackTrace();
    }
  }

  @SuppressWarnings("ConstantConditions")
  public void next() {
    if (this.queue.size() == 0) {
      this.player.getConnection().eventLoop().execute(this::finish);
    } else {
      this.player.getConnection().eventLoop().execute(this.queue.poll());
    }
  }

  private void finish() {
    this.limboAPI.removeLoginQueue(this.player);
    MinecraftConnection connection = this.player.getConnection();
    try {
      // ported from Velocity
      this.server.getEventManager()
          .fire(new PermissionsSetupEvent(player, (PermissionProvider) defaultPermissions.get(null)))
          .thenAcceptAsync(event -> {
            // wait for permissions to load, then set the players permission function
            final PermissionFunction function = event.createFunction(this.player);
            if (function == null) {
              this.limboAPI.getLogger().error(
                  "A plugin permission provider {} provided an invalid permission function"
                      + " for player {}. This is a bug in the plugin, not in Velocity. Falling"
                      + " back to the default permission function.",
                  event.getProvider().getClass().getName(),
                  this.player.getUsername());
            } else {
              try {
                setPermissionFunction.invoke(this.player, function);
              } catch (IllegalAccessException | InvocationTargetException ex) {
                this.limboAPI.getLogger()
                    .error("Exception while completing injection to {}", this.player, ex);
              }
            }
            initialize(connection);
          });
    } catch (IllegalAccessException ex) {
      this.limboAPI.getLogger()
          .error("Exception while completing injection to {}", this.player, ex);
    }
  }

  // Ported from Velocity
  @SneakyThrows
  private void initialize(MinecraftConnection connection) {
    state.set(connection, StateRegistry.PLAY);
    connection.getChannel().pipeline().get(MinecraftEncoder.class).setState(StateRegistry.PLAY);
    connection.getChannel().pipeline().get(MinecraftDecoder.class).setState(StateRegistry.PLAY);
    association.set(connection, this.player);

    this.server.getEventManager().fire(new LoginEvent(this.player))
        .thenAcceptAsync(event -> {
          if (connection.isClosed()) {
            // The player was disconnected
            this.server.getEventManager().fireAndForget(new DisconnectEvent(this.player,
                DisconnectEvent.LoginStatus.CANCELLED_BY_USER_BEFORE_COMPLETE));
            return;
          }

          Optional<Component> reason = event.getResult().getReasonComponent();
          if (reason.isPresent()) {
            this.player.disconnect0(reason.get(), true);
          } else {
            if (!this.server.registerConnection(this.player)) {
              this.player.disconnect0(Component.translatable("velocity.error.already-connected-proxy"),
                  true);
              return;
            }

            try {
              connection.setSessionHandler(initialCtor.newInstance(this.player));
              this.server.getEventManager().fire(new PostLoginEvent(this.player))
                  .thenAccept((ignored) -> {
                    try {
                      // go back i want to be ~~monke~~ original mcConnection
                      loginConnectionField.set(this.handler, connection);
                      connectToInitialServer.invoke(this.handler, this.player);
                    } catch (IllegalAccessException | InvocationTargetException ex) {
                      this.limboAPI.getLogger()
                          .error("Exception while connecting {} to initial server", this.player, ex);
                    }
                  });
            } catch (InstantiationException | InvocationTargetException | IllegalAccessException e) {
              e.printStackTrace();
            }
          }
        }, connection.eventLoop())
        .exceptionally((ex) -> {
          this.limboAPI.getLogger()
              .error("Exception while completing login initialisation phase for {}", this.player, ex);
          return null;
        });
  }
}
