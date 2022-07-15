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

package net.elytrium.limboapi.injection.kick;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.proxy.ConnectionRequestBuilder;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.connection.util.ConnectionMessages;
import com.velocitypowered.proxy.connection.util.ConnectionRequestResults;
import com.velocitypowered.proxy.protocol.packet.Disconnect;
import io.netty.channel.EventLoop;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import net.elytrium.limboapi.LimboAPI;
import net.elytrium.limboapi.injection.dummy.ClosedChannel;
import net.elytrium.limboapi.injection.dummy.ClosedMinecraftConnection;
import net.elytrium.limboapi.injection.dummy.DummyEventPool;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;

public class KickListener {

  private static final MinecraftConnection DUMMY_CONNECTION = new ClosedMinecraftConnection(new ClosedChannel(new DummyEventPool()), null);
  private static Field CONNECTION_FIELD;
  private static Field BACKEND_CONNECTION_FIELD;
  private static Method CREATE_CONNECTION_REQUEST;

  private final LimboAPI plugin;

  public KickListener(LimboAPI plugin) {
    this.plugin = plugin;
  }

  @Subscribe(order = PostOrder.LAST)
  public void onPlayerKicked(KickedFromServerEvent event) {
    ConnectedPlayer player = (ConnectedPlayer) event.getPlayer();
    Function<KickedFromServerEvent, Boolean> callback = this.plugin.getKickCallback(player);
    if (callback != null) {
      MinecraftConnection connection = player.getConnection();
      VelocityServerConnection backendConnection = player.getConnectedServer();

      try {
        CONNECTION_FIELD.set(player, DUMMY_CONNECTION);

        EventLoop eventLoop = connection.getChannel().eventLoop();
        eventLoop.schedule(() -> {
          try {
            BACKEND_CONNECTION_FIELD.set(player, null);
            if (connection.isClosed()) {
              return;
            }

            CONNECTION_FIELD.set(player, connection);
            if (!callback.apply(event)) {
              this.handleThen(event, player, backendConnection);
            }
          } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            connection.close();
          }
        }, 250, TimeUnit.MILLISECONDS);
      } catch (IllegalAccessException e) {
        e.printStackTrace();
        connection.close();
      }
    }
  }

  // From Velocity.
  private void handleThen(KickedFromServerEvent event, ConnectedPlayer player, VelocityServerConnection serverConnection)
      throws InvocationTargetException, IllegalAccessException {
    if (event.getResult() instanceof KickedFromServerEvent.DisconnectPlayer) {
      KickedFromServerEvent.DisconnectPlayer res = (KickedFromServerEvent.DisconnectPlayer) event.getResult();
      player.disconnect(res.getReasonComponent());
    } else if (event.getResult() instanceof KickedFromServerEvent.RedirectPlayer) {
      KickedFromServerEvent.RedirectPlayer res = (KickedFromServerEvent.RedirectPlayer) event.getResult();
      ((ConnectionRequestBuilder) CREATE_CONNECTION_REQUEST.invoke(player, res.getServer(), serverConnection))
          .connect()
          .whenCompleteAsync((status, throwable) -> {
            if (throwable != null) {
              player.handleConnectionException(status != null ? status.getAttemptedConnection()
                  : res.getServer(), throwable, true);
              return;
            }

            switch (status.getStatus()) {
              // Impossible/nonsensical cases
              case ALREADY_CONNECTED:
              case CONNECTION_IN_PROGRESS:
                // Fatal case
              case CONNECTION_CANCELLED:
                Component fallbackMsg = res.getMessageComponent();
                if (fallbackMsg == null) {
                  fallbackMsg = Component.empty();
                }
                player.disconnect(status.getReasonComponent().orElse(fallbackMsg));
                break;
              case SERVER_DISCONNECTED:
                Component reason = status.getReasonComponent()
                    .orElse(ConnectionMessages.INTERNAL_SERVER_CONNECTION_ERROR);
                player.handleConnectionException(res.getServer(), Disconnect.create(reason,
                    player.getProtocolVersion()), ((ConnectionRequestResults.Impl) status).isSafe());
                break;
              case SUCCESS:
                Component requestedMessage = res.getMessageComponent();
                if (requestedMessage == null) {
                  requestedMessage = Component.empty();
                }
                if (requestedMessage != Component.empty()) {
                  player.sendMessage(requestedMessage);
                }
                break;
              default:
                // The only remaining value is successful (no need to do anything!)
                break;
            }
          }, player.getConnection().eventLoop());
    } else if (event.getResult() instanceof KickedFromServerEvent.Notify) {
      KickedFromServerEvent.Notify res = (KickedFromServerEvent.Notify) event.getResult();
      if (event.kickedDuringServerConnect() && serverConnection != null) {
        player.sendMessage(Identity.nil(), res.getMessageComponent());
      } else {
        player.disconnect(res.getMessageComponent());
      }
    } else {
      // In case someone gets creative, assume we want to disconnect the player.
      player.disconnect(Component.empty());
    }
  }

  static {
    try {
      CONNECTION_FIELD = ConnectedPlayer.class.getDeclaredField("connection");
      CONNECTION_FIELD.setAccessible(true);

      BACKEND_CONNECTION_FIELD = ConnectedPlayer.class.getDeclaredField("connectedServer");
      BACKEND_CONNECTION_FIELD.setAccessible(true);

      CREATE_CONNECTION_REQUEST =
          ConnectedPlayer.class.getDeclaredMethod("createConnectionRequest", RegisteredServer.class, VelocityServerConnection.class);
      CREATE_CONNECTION_REQUEST.setAccessible(true);
    } catch (NoSuchFieldException | NoSuchMethodException e) {
      e.printStackTrace();
    }
  }
}
