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
import com.google.common.collect.ImmutableMap;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.command.builtin.CommandMessages;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import net.elytrium.limbofilter.FilterPlugin;
import net.elytrium.limbofilter.config.Settings;
import net.elytrium.limbofilter.stats.Statistics;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.checkerframework.checker.nullness.qual.NonNull;

public class FilterCommand implements SimpleCommand {

  private interface SubCommand {

    void execute(final CommandSource source, final String @NonNull [] args);

    default List<String> suggest(final CommandSource source, final String @NonNull [] currentArgs) {
      return ImmutableList.of();
    }

    boolean hasPermission(final CommandSource source, final String @NonNull [] args);
  }

  private final Map<String, SubCommand> commands;

  public FilterCommand(VelocityServer server, FilterPlugin plugin) {
    this.commands = ImmutableMap.<String, SubCommand>builder()
        .put("reload", new Reload(plugin))
        .put("stats", new Stats(server, plugin))
        .build();
  }

  private void usage(CommandSource source) {
    source.sendMessage(Identity.nil(), Component.text("§eThis server is using LimboFilter"));
    source.sendMessage(Identity.nil(), Component.text("§e(c) 2021 Elytrium"));
    source.sendMessage(Identity.nil(), Component.text("§ahttps://ely.su/github/"));
  }

  @Override
  public void execute(final SimpleCommand.Invocation invocation) {
    final CommandSource source = invocation.source();
    final String[] args = invocation.arguments();

    if (args.length == 0) {
      usage(source);
      return;
    }

    SubCommand command = commands.get(args[0].toLowerCase(Locale.US));
    if (command == null) {
      usage(source);
      return;
    }
    @SuppressWarnings("nullness")
    String[] actualArgs = Arrays.copyOfRange(args, 1, args.length);
    command.execute(source, actualArgs);
  }

  @Override
  public List<String> suggest(final SimpleCommand.Invocation invocation) {
    final CommandSource source = invocation.source();
    final String[] currentArgs = invocation.arguments();

    if (currentArgs.length == 0) {
      return commands.entrySet().stream()
          .filter(e -> e.getValue().hasPermission(source, new String[0]))
          .map(Map.Entry::getKey)
          .collect(ImmutableList.toImmutableList());
    }

    if (currentArgs.length == 1) {
      return commands.entrySet().stream()
          .filter(e -> e.getKey().regionMatches(true, 0, currentArgs[0], 0, currentArgs[0].length()))
          .filter(e -> e.getValue().hasPermission(source, new String[0]))
          .map(Map.Entry::getKey)
          .collect(ImmutableList.toImmutableList());
    }

    SubCommand command = commands.get(currentArgs[0].toLowerCase(Locale.US));
    if (command == null) {
      return ImmutableList.of();
    }
    @SuppressWarnings("nullness")
    String[] actualArgs = Arrays.copyOfRange(currentArgs, 1, currentArgs.length);
    return command.suggest(source, actualArgs);
  }

  @Override
  public boolean hasPermission(final SimpleCommand.Invocation invocation) {
    final CommandSource source = invocation.source();
    final String[] args = invocation.arguments();

    if (args.length == 0) {
      return commands.values().stream().anyMatch(e -> e.hasPermission(source, args));
    }
    SubCommand command = commands.get(args[0].toLowerCase(Locale.US));
    if (command == null) {
      return true;
    }
    @SuppressWarnings("nullness")
    String[] actualArgs = Arrays.copyOfRange(args, 1, args.length);
    return command.hasPermission(source, actualArgs);
  }

  private static class Reload implements SubCommand {

    private final FilterPlugin plugin;

    private Reload(FilterPlugin plugin) {
      this.plugin = plugin;
    }

    @Override
    public void execute(CommandSource source, String @NonNull [] args) {
      plugin.reload();
    }

    @Override
    public boolean hasPermission(final CommandSource source, final String @NonNull [] args) {
      return source.hasPermission("limbofilter.reload");
    }
  }

  private static class Stats implements SubCommand {

    private final List<UUID> playersWithStats = Collections.synchronizedList(new ArrayList<>());

    private Stats(VelocityServer server, FilterPlugin plugin) {
      new Timer().scheduleAtFixedRate(new TimerTask() {
        @Override
        public void run() {
          try {
            Statistics statistics = plugin.getStatistics();
            playersWithStats.stream().map(server::getPlayer).forEach(player -> player.ifPresent(p -> p.sendActionBar(
                LegacyComponentSerializer
                    .legacyAmpersand()
                    .deserialize(Settings.IMP.MAIN.STRINGS.STATS_FORMAT
                        .replace("{0}", String.valueOf(statistics.getBlockedConnections()))
                        .replace("{1}", String.valueOf(statistics.getConnectionsPerSecond()))
                        .replace("{2}", String.valueOf(statistics.getPingsPerSecond()))
                        .replace("{3}", String.valueOf(statistics.getTotalConnectionsPerSecond()))
                        .replace("{4}", String.valueOf(p.getPing()))
                    ))));
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      }, 1000, 1000);
    }

    @Override
    public void execute(CommandSource source, String @NonNull [] args) {
      if (!(source instanceof Player)) {
        source.sendMessage(CommandMessages.PLAYERS_ONLY);
        return;
      }
      ConnectedPlayer player = (ConnectedPlayer) source;
      if (playersWithStats.contains(player.getUniqueId())) {
        source.sendMessage(
            LegacyComponentSerializer
                .legacyAmpersand()
                .deserialize(Settings.IMP.MAIN.STRINGS.STATS_DISABLED));
        playersWithStats.remove(player.getUniqueId());
      } else {
        source.sendMessage(
            LegacyComponentSerializer
                .legacyAmpersand()
                .deserialize(Settings.IMP.MAIN.STRINGS.STATS_ENABLED));
        playersWithStats.add(player.getUniqueId());
      }
    }

    @Override
    public boolean hasPermission(final CommandSource source, final String @NonNull [] args) {
      return source.hasPermission("limbofilter.stats");
    }
  }
}
