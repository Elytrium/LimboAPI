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

public class FallbackListener {

  @Subscribe
  public void onProxyConnect(PlayerChooseInitialServerEvent e) {
    if (e.getInitialServer().isEmpty()) {
      // TODO: send to limbo
    }
  }

  @Subscribe
  @SuppressWarnings("ConstantConditions")
  public void onKick(KickedFromServerEvent e) {
    // Forcing NullPointerException to prevent player kicking
    // May not work :(
    e.setResult(KickedFromServerEvent.DisconnectPlayer.create(null));
    // TODO: send to limbo
  }
}
