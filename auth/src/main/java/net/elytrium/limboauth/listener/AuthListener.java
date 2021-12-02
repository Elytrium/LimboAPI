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

import com.j256.ormlite.dao.Dao;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import com.velocitypowered.api.util.UuidUtils;
import java.sql.SQLException;
import java.util.UUID;
import net.elytrium.limboapi.api.event.LoginLimboRegisterEvent;
import net.elytrium.limboauth.AuthPlugin;
import net.elytrium.limboauth.Settings;
import net.elytrium.limboauth.handler.AuthSessionHandler;
import net.elytrium.limboauth.model.RegisteredPlayer;

public class AuthListener {

  private final Dao<RegisteredPlayer, String> playerDao;

  public AuthListener(Dao<RegisteredPlayer, String> playerDao) {
    this.playerDao = playerDao;
  }

  @Subscribe
  public void onProxyConnect(PreLoginEvent e) {
    if (!e.getResult().isForceOfflineMode()) {
      if (Settings.IMP.MAIN.ONLINE_MODE_NEED_AUTH || !AuthPlugin.getInstance().isPremium(e.getUsername())) {
        e.setResult(PreLoginEvent.PreLoginComponentResult.forceOfflineMode());
      } else {
        e.setResult(PreLoginEvent.PreLoginComponentResult.forceOnlineMode());
      }
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
    if (Settings.IMP.MAIN.SAVE_UUID) {
      RegisteredPlayer registeredPlayer = AuthSessionHandler.fetchInfo(this.playerDao, e.getOriginalProfile().getId());

      if (registeredPlayer != null) {
        e.setGameProfile(e.getOriginalProfile().withId(UUID.fromString(registeredPlayer.uuid)));
        return;
      }

      registeredPlayer = AuthSessionHandler.fetchInfo(this.playerDao, e.getUsername());

      if (registeredPlayer != null) {
        if (e.isOnlineMode()) {
          try {
            registeredPlayer.premiumUuid = e.getOriginalProfile().getId().toString();
            this.playerDao.update(registeredPlayer);
          } catch (SQLException ex) {
            ex.printStackTrace();
          }
        }
        e.setGameProfile(e.getOriginalProfile().withId(UUID.fromString(registeredPlayer.uuid)));
      }
    }

    if (!Settings.IMP.MAIN.ONLINE_UUID_IF_POSSIBLE) {
      e.setGameProfile(e.getOriginalProfile().withId(UuidUtils.generateOfflinePlayerUuid(e.getUsername())));
    }
  }
}
