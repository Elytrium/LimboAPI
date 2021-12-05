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

package net.elytrium.limbofilter.commands;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.elytrium.limbofilter.FilterPlugin;
import net.elytrium.limbofilter.Settings;
import net.elytrium.limbofilter.stats.Statistics;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class LimboFilterCommand implements SimpleCommand {

  private final FilterPlugin plugin;

  private final List<UUID> playersWithStats = Collections.synchronizedList(new ArrayList<>());

  public LimboFilterCommand(VelocityServer server, FilterPlugin plugin) {
    this.plugin = plugin;

    new Timer().scheduleAtFixedRate(new TimerTask() {
      @Override
      public void run() {
        try {
          LimboFilterCommand.this.playersWithStats
              .stream()
              .map(server::getPlayer)
              .forEach(player -> player.ifPresent(p -> p.sendActionBar(LimboFilterCommand.this.createStatsComponent(p.getPing()))));
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }, 1000, 1000);
  }

  @Override
  public List<String> suggest(SimpleCommand.Invocation invocation) {
    CommandSource source = invocation.source();
    String[] args = invocation.arguments();

    if (args.length == 0) {
      return this.getSubCommands()
          .filter(cmd -> source.hasPermission("limbofilter." + cmd))
          .collect(Collectors.toList());
    } else if (args.length == 1) {
      return this.getSubCommands()
          .filter(cmd -> source.hasPermission("limbofilter." + cmd))
          .filter(str -> str.regionMatches(true, 0, args[0], 0, args[0].length()))
          .collect(Collectors.toList());
    }

    return ImmutableList.of();
  }

  @Override
  public void execute(SimpleCommand.Invocation invocation) {
    CommandSource source = invocation.source();
    String[] args = invocation.arguments();

    if (args.length == 1) {
      switch (args[0].toLowerCase(Locale.ROOT)) {
        case "reload": {
          if (source.hasPermission("limbofilter.reload")) {
            try {
              this.plugin.reload();
              source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(Settings.IMP.MAIN.STRINGS.RELOAD));
            } catch (Exception e) {
              source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(Settings.IMP.MAIN.STRINGS.RELOAD_FAILED));
              e.printStackTrace();
            }
          } else {
            this.showHelp(source);
          }
          return;
        }
        case "stats": {
          if (source instanceof Player) {
            if (source.hasPermission("limbofilter.stats")) {
              ConnectedPlayer player = (ConnectedPlayer) source;
              if (!this.playersWithStats.contains(player.getUniqueId())) {
                source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(Settings.IMP.MAIN.STRINGS.STATS_ENABLED));
                this.playersWithStats.add(player.getUniqueId());
              } else {
                source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(Settings.IMP.MAIN.STRINGS.STATS_DISABLED));
                this.playersWithStats.remove(player.getUniqueId());
              }
            } else {
              this.showHelp(source);
            }
          } else {
            source.sendMessage(this.createStatsComponent(0));
          }
          return;
        }
        default: {
          this.showHelp(source);
        }
      }
    }

    this.showHelp(source);
  }

  private void showHelp(CommandSource source) {
    source.sendMessage(Component.text("§eThis server is using LimboFilter and LimboAPI"));
    source.sendMessage(Component.text("§e(c) 2021 Elytrium"));
    source.sendMessage(Component.text("§ahttps://ely.su/github/"));
    source.sendMessage(Component.text("§r"));
    source.sendMessage(Component.text("§fAvailable subcommands:"));
    // Java moment
    this.getSubCommands()
        .filter(cmd -> source.hasPermission("limbofilter." + cmd))
        .forEach(cmd -> {
          if (cmd.equals("reload")) {
            source.sendMessage(Component.text("    §a/limbofilter reload §8- §eReload config"));
          } else if (cmd.equals("stats")) {
            source.sendMessage(Component.text("    §a/limbofilter stats §8- §eEnable/Disable statistics of connections and blocked bots"));
          }
        });
  }

  private Stream<String> getSubCommands() {
    return Stream.of("reload", "stats");
  }

  private Component createStatsComponent(long ping) {
    Statistics statistics = this.plugin.getStatistics();

    return LegacyComponentSerializer.legacyAmpersand().deserialize(
        MessageFormat.format(
            Settings.IMP.MAIN.STRINGS.STATS_FORMAT,
            statistics.getBlockedConnections(),
            statistics.getConnections() + "/" + Settings.IMP.MAIN.UNIT_OF_TIME_CPS + "s",
            statistics.getPings() + "/" + Settings.IMP.MAIN.UNIT_OF_TIME_PPS + "s",
            statistics.getTotalConnection(),
            ping
        )
    );
  }
}
