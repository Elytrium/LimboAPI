/*
 * Copyright (C) 2021 - 2022 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api;

import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.proxy.Player;
import net.elytrium.limboapi.api.command.LimboCommandMeta;

public interface Limbo {

  void spawnPlayer(Player player, LimboSessionHandler handler);

  void respawnPlayer(Player player);

  Limbo setName(String name);

  Limbo registerCommand(LimboCommandMeta commandMeta);

  Limbo registerCommand(CommandMeta commandMeta, Command command);
}
