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
 */

package net.elytrium.limboapi.utils;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyReloadEvent;
import net.elytrium.limboapi.LimboAPI;

public class ReloadListener {

  private final LimboAPI plugin;

  public ReloadListener(LimboAPI plugin) {
    this.plugin = plugin;
  }

  @Subscribe
  public void onReload(ProxyReloadEvent event) {
    this.plugin.reloadPreparedPacketFactory();
    this.plugin.reload();
  }
}
