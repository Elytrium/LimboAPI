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
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import net.elytrium.limboapi.api.event.LoginLimboRegisterEvent;
import net.elytrium.limbofilter.FilterPlugin;
import net.elytrium.limbofilter.config.Settings;

@RequiredArgsConstructor
public class FilterListener {

  private final FilterPlugin plugin;

  @SneakyThrows
  @Subscribe(order = PostOrder.FIRST)
  public void onProxyConnect(PreLoginEvent e) {
    if (!Settings.IMP.MAIN.ONLINE_MODE_VERIFY
        && !plugin.shouldCheck(
            e.getUsername(), e.getConnection().getRemoteAddress().getAddress())) {
      e.setResult(PreLoginEvent.PreLoginComponentResult.forceOfflineMode());
    }
  }

  @Subscribe(order = PostOrder.FIRST)
  public void onLogin(LoginLimboRegisterEvent e) {
    plugin.getStatistics().addConnectionPerSecond();
    if (plugin.shouldCheck((ConnectedPlayer) e.getPlayer())) {
      e.addCallback(() -> plugin.filter(e.getPlayer()));
    }
  }

  @Subscribe
  public void onPing(ProxyPingEvent e) {
    plugin.getStatistics().addPingPerSecond();
  }

  @Subscribe
  public void onQuery(ProxyQueryEvent e) {
    plugin.getStatistics().addPingPerSecond();
  }
}
