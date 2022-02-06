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

package net.elytrium.limboapi.injection.disconnect;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import net.elytrium.limboapi.LimboAPI;

public class DisconnectListener {

  private final LimboAPI plugin;

  public DisconnectListener(LimboAPI plugin) {
    this.plugin = plugin;
  }

  @Subscribe
  public void onDisconnect(DisconnectEvent event) {
    this.plugin.unsetLimboJoined(event.getPlayer());
    this.plugin.removeLoginQueue(event.getPlayer());
    this.plugin.removeNextServer(event.getPlayer());
    this.plugin.removeInitialUUID(event.getPlayer());
  }
}
