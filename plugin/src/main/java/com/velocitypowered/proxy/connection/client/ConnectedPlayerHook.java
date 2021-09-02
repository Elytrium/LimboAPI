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

package com.velocitypowered.proxy.connection.client;

import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.net.InetSocketAddress;
import net.elytrium.limboapi.LimboAPI;

public class ConnectedPlayerHook extends ConnectedPlayer {

  private final LimboAPI limbo;

  public ConnectedPlayerHook(LimboAPI limbo,
      VelocityServer server, GameProfile profile,MinecraftConnection connection,
      @Nullable InetSocketAddress virtualHost, boolean onlineMode) {
    super(server, profile, connection, virtualHost, onlineMode);
    this.limbo = limbo;
  }

  @Override
  public boolean isActive() {
    return super.isActive() && !this.limbo.isCurrentlyLimboJoined(this);
  }
}
