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
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.LoginAcknowledged;
import io.netty.buffer.ByteBuf;

public class LoginConfirmHandler extends ConfirmHandler {

  public LoginConfirmHandler(MinecraftConnection connection) {
    super(connection);
  }

  @Override
  public boolean handle(LoginAcknowledged packet) {
    this.connection.setState(StateRegistry.CONFIG);
    this.confirmation.complete(this);
    return true;
  }

  @Override
  public void handleUnknown(ByteBuf buf) {
    this.connection.close(true);
  }
}
