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

package net.elytrium.limboauth.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import com.velocitypowered.api.util.UuidUtils;
import net.elytrium.limboapi.api.event.LoginLimboRegisterEvent;
import net.elytrium.limboauth.AuthPlugin;
import net.elytrium.limboauth.config.Settings;

public class AuthListener {

  @Subscribe
  public void onProxyConnect(PreLoginEvent e) {
    if (Settings.IMP.MAIN.ONLINE_MODE_NEED_AUTH || !AuthPlugin.getInstance().isPremium(e.getUsername())) {
      e.setResult(PreLoginEvent.PreLoginComponentResult.forceOfflineMode());
    } else {
      e.setResult(PreLoginEvent.PreLoginComponentResult.forceOnlineMode());
    }
  }

  @Subscribe
  public void onLogin(LoginLimboRegisterEvent e) {
    if (AuthPlugin.getInstance().needAuth(e.getPlayer())) {
      e.addCallback(() -> AuthPlugin.getInstance().auth(e.getPlayer()));
    }
  }

  @Subscribe
  public void onProfile(GameProfileRequestEvent e) {
    if (!Settings.IMP.MAIN.ONLINE_UUID_IF_POSSIBLE) {
      e.setGameProfile(
          e.getOriginalProfile().withId(
              UuidUtils.generateOfflinePlayerUuid(e.getUsername())));
    }
  }
}
