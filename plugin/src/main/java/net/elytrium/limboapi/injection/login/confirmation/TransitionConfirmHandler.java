/*
 * Copyright (C) 2021 - 2023 Elytrium
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

package net.elytrium.limboapi.injection.login.confirmation;

import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.config.FinishedUpdate;
import net.elytrium.limboapi.LimboAPI;

public class TransitionConfirmHandler extends ConfirmHandler {

  public TransitionConfirmHandler(LimboAPI plugin, MinecraftConnection connection) {
    super(plugin, connection);
  }

  @Override
  public boolean handle(FinishedUpdate packet) {
    if (this.connection.getState() == StateRegistry.PLAY) {
      this.plugin.setState(this.connection, StateRegistry.CONFIG);
      this.confirmation.complete(this);
      return true;
    }

    return false;
  }

  public void trackTransition(ConnectedPlayer player, Runnable runnable) {
    this.setPlayer(player);
    this.plugin.setActiveSessionHandler(this.connection, StateRegistry.PLAY, this);
    this.waitForConfirmation(runnable);
  }
}
