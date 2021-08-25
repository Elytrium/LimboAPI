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
 */

package net.elytrium.limboapi.injection;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.ConnectionHandshakeEvent;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.InitialInboundConnection;
import com.velocitypowered.proxy.connection.client.LoginSessionHandler;
import java.lang.reflect.Field;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import net.elytrium.limboapi.LimboAPI;

@RequiredArgsConstructor
public class HandshakeListener {
  private static Field connectionField;

  static {
    try {
      connectionField = InitialInboundConnection.class.getDeclaredField("connection");
      connectionField.setAccessible(true);
    } catch (NoSuchFieldException e) {
      e.printStackTrace();
    }
  }

  private final LimboAPI limboAPI;

  @SneakyThrows
  @Subscribe
  public void onHandshakeFinished(ConnectionHandshakeEvent e) {
    // TODO: Better injection
    new Thread(() -> {
      try {
        MinecraftConnection connection = (MinecraftConnection) connectionField.get(e.getConnection());
        //noinspection StatementWithEmptyBody
        while (!(connection.getSessionHandler() instanceof LoginSessionHandler)) {
          // busy wait.
        }

        connection.setSessionHandler(new FakeLoginSessionHandler(
            limboAPI.getServer(), connection, (InitialInboundConnection) e.getConnection()));
      } catch (IllegalAccessException ex) {
        ex.printStackTrace();
      }
    }).start();
  }
}
