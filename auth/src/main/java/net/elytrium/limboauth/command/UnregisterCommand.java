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

package net.elytrium.limboauth.command;

import com.j256.ormlite.dao.Dao;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import java.util.Locale;
import lombok.SneakyThrows;
import net.elytrium.limboauth.config.Settings;
import net.elytrium.limboauth.model.RegisteredPlayer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class UnregisterCommand implements SimpleCommand {

  private final Dao<RegisteredPlayer, String> playerDao;

  public UnregisterCommand(Dao<RegisteredPlayer, String> playerDao) {
    this.playerDao = playerDao;
  }

  @SneakyThrows
  @Override
  public void execute(final Invocation invocation) {
    final CommandSource source = invocation.source();
    final String[] args = invocation.arguments();

    if (args.length != 1) {
      source.sendMessage(
          LegacyComponentSerializer
              .legacyAmpersand()
              .deserialize(Settings.IMP.MAIN.STRINGS.UNREGISTER_USAGE));
    } else {
      playerDao.deleteById(args[0].toLowerCase(Locale.ROOT));
      source.sendMessage(
          LegacyComponentSerializer
              .legacyAmpersand()
              .deserialize(Settings.IMP.MAIN.STRINGS.UNREGISTER_SUCCESSFUL));
    }
  }

  @Override
  public boolean hasPermission(final Invocation invocation) {
    return invocation.source().hasPermission("limboauth.unregister");
  }
}
