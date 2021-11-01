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

package net.elytrium.limbofilter.listener;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.event.query.ProxyQueryEvent;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import net.elytrium.limboapi.api.event.LoginLimboRegisterEvent;
import net.elytrium.limbofilter.FilterPlugin;
import net.elytrium.limbofilter.Settings;

public class FilterListener {

  private final FilterPlugin plugin;

  public FilterListener(FilterPlugin plugin) {
    this.plugin = plugin;
  }

  @Subscribe(order = PostOrder.FIRST)
  public void onProxyConnect(PreLoginEvent e) {
    this.plugin.getStatistics().addConnection();
    if (this.plugin.checkCpsLimit(Settings.IMP.MAIN.FILTER_AUTO_TOGGLE.ONLINE_MODE_VERIFY)
        && this.plugin.shouldCheck(e.getUsername(), e.getConnection().getRemoteAddress().getAddress())) {
      e.setResult(PreLoginEvent.PreLoginComponentResult.forceOfflineMode());
    }
  }

  @Subscribe(order = PostOrder.FIRST)
  public void onLogin(LoginLimboRegisterEvent e) {
    if (this.plugin.shouldCheck((ConnectedPlayer) e.getPlayer())) {
      e.addCallback(() -> this.plugin.filter(e.getPlayer()));
    }
  }

  @Subscribe(order = PostOrder.LAST)
  public void onPing(ProxyPingEvent e) {
    if (this.plugin.checkPpsLimit(Settings.IMP.MAIN.FILTER_AUTO_TOGGLE.DISABLE_MOTD_PICTURE)) {
      e.setPing(e.getPing().asBuilder().clearFavicon().build());
    }

    this.plugin.getStatistics().addPing();
  }

  @Subscribe
  public void onQuery(ProxyQueryEvent e) {
    this.plugin.getStatistics().addPing();
  }
}
