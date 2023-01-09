/*
 * Copyright (C) 2021 - 2023 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api;

import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.proxy.Player;
import net.elytrium.limboapi.api.command.LimboCommandMeta;
import net.elytrium.limboapi.api.player.GameMode;

public interface Limbo {

  void spawnPlayer(Player player, LimboSessionHandler handler);

  void respawnPlayer(Player player);

  long getCurrentOnline();

  Limbo setName(String name);

  Limbo setReadTimeout(int millis);

  Limbo setWorldTime(long ticks);

  Limbo setGameMode(GameMode gameMode);

  Limbo setShouldRespawn(boolean shouldRespawn);

  Limbo setReducedDebugInfo(boolean reducedDebugInfo);

  Limbo setViewDistance(int viewDistance);

  Limbo setSimulationDistance(int simulationDistance);

  Limbo setMaxSuppressPacketLength(int maxSuppressPacketLength);

  Limbo registerCommand(LimboCommandMeta commandMeta);

  Limbo registerCommand(CommandMeta commandMeta, Command command);

  void dispose();
}
