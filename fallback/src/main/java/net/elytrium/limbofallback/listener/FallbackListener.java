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

package net.elytrium.limbofallback.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;
import net.elytrium.limbofallback.FallbackPlugin;

public class FallbackListener {

  private final AtomicInteger counter = new AtomicInteger();

  @Subscribe
  public void onProxyConnect(PlayerChooseInitialServerEvent e) {
    RegisteredServer srv = e.getInitialServer().get();
    if (srv.getServerInfo().getName().startsWith("Limbo_")) {
      FallbackPlugin.getInstance().sendToFallBackServer(e.getPlayer(), srv);
    } else if (e.getInitialServer().isEmpty() || srv.getServerInfo() == null) {
      FallbackPlugin.getInstance().sendToFallBackServer(e.getPlayer(), srv);
    }
  }

  @Subscribe
  public void onKick(KickedFromServerEvent e) {
    FallbackPlugin.getInstance().getLogger().info("Sending player to limbo.");
    int i = this.counter.incrementAndGet();
    if (i > 255) {
      this.counter.set(0);
      i = 0;
    }
    e.setResult(KickedFromServerEvent.RedirectPlayer.create(
        FallbackPlugin.getInstance().getServer().registerServer(
            new ServerInfo("Limbo_" + i + "_" + e.getPlayer().getUsername(), InetSocketAddress.createUnresolved("192.168.44." + i, i))
        )
    ));
  }
}
