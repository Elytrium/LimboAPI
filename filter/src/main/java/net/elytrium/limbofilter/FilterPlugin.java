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

package net.elytrium.limbofilter;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;
import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.LimboFactory;
import net.elytrium.limboapi.api.chunk.Dimension;
import net.elytrium.limboapi.api.chunk.VirtualWorld;
import net.elytrium.limboapi.api.file.SchematicFile;
import net.elytrium.limboapi.api.file.WorldFile;
import net.elytrium.limbofilter.cache.CachedPackets;
import net.elytrium.limbofilter.commands.FilterCommand;
import net.elytrium.limbofilter.config.Settings;
import net.elytrium.limbofilter.handler.BotFilterSessionHandler;
import net.elytrium.limbofilter.listener.FilterListener;
import net.elytrium.limbofilter.stats.Statistics;
import org.slf4j.Logger;

@Plugin(
    id = "limbofilter",
    name = "LimboFilter",
    version = "1.0.0",
    url = "ely.su",
    authors = {"hevav", "mdxd44"},
    dependencies = {@Dependency(id = "limboapi")}
)

@Getter
@SuppressFBWarnings("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
public class FilterPlugin {
  private static FilterPlugin instance;
  private final Path dataDirectory;
  private final Logger logger;
  private final ProxyServer server;
  private final LimboFactory factory;
  private final CachedPackets packets;
  private final Statistics statistics;
  private Map<String, CachedUser> cachedFilterChecks;
  private ScheduledExecutorService scheduler;
  private Limbo filterServer;

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  @Inject
  public FilterPlugin(ProxyServer server,
                    Logger logger,
                    @Named("limboapi") PluginContainer factory,
                    @DataDirectory Path dataDirectory) {
    this.server = server;
    this.logger = logger;
    this.dataDirectory = dataDirectory;
    this.factory = (LimboFactory) factory.getInstance().get();
    this.packets = new CachedPackets();
    this.statistics = new Statistics();
    statistics.startUpdating();
  }

  @Subscribe
  public void onProxyInitialization(ProxyInitializeEvent event) {
    instance = this;
    reload();
  }

  @SneakyThrows
  public void reload() {
    checkForUpdates();
    Settings.IMP.reload(new File(dataDirectory.toFile().getAbsoluteFile(), "config.yml"));

    CaptchaGeneration.init();
    packets.createPackets();

    cachedFilterChecks = new ConcurrentHashMap<>();

    Settings.MAIN.CAPTCHA_COORDS captchaCoords = Settings.IMP.MAIN.CAPTCHA_COORDS;
    VirtualWorld authWorld = factory.createVirtualWorld(
        Dimension.valueOf(Settings.IMP.MAIN.BOTFILTER_DIMENSION),
        captchaCoords.X, captchaCoords.Y, captchaCoords.Z,
        (float) captchaCoords.YAW, (float) captchaCoords.PITCH);

    if (Settings.IMP.MAIN.LOAD_WORLD) {
      try {
        Path path = dataDirectory.resolve(Settings.IMP.MAIN.WORLD_FILE_PATH);
        WorldFile file;
        switch (Settings.IMP.MAIN.WORLD_FILE_TYPE) {
          case "schematic":
            file = new SchematicFile(path);
            break;
          default:
            logger.error("Incorrect world file type.");
            server.shutdown();
            return;
        }

        Settings.MAIN.WORLD_COORDS coords = Settings.IMP.MAIN.WORLD_COORDS;
        file.toWorld(factory, authWorld, coords.X, coords.Y, coords.Z);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    filterServer = factory.createLimbo(authWorld);

    server.getEventManager().register(this, new FilterListener(this));

    scheduler =
        Executors.newScheduledThreadPool(1, task -> new Thread(task, "purge-cache"));

    scheduler.scheduleAtFixedRate(
        () -> checkCache(cachedFilterChecks, Settings.IMP.MAIN.PURGE_CACHE_MILLIS),
        Settings.IMP.MAIN.PURGE_CACHE_MILLIS,
        Settings.IMP.MAIN.PURGE_CACHE_MILLIS,
        TimeUnit.MILLISECONDS);

    CommandManager manager = server.getCommandManager();
    manager.unregister("limbofilter");
    manager.register("limbofilter",
        new FilterCommand((VelocityServer) server, this),
        "lf", "botfilter", "bf");
  }

  public void cacheFilterUser(Player player) {
    String username = player.getUsername();
    cachedFilterChecks.remove(username);
    InetSocketAddress adr = player.getRemoteAddress();
    cachedFilterChecks.put(username, new CachedUser(adr.getAddress(), System.currentTimeMillis()));
  }

  public boolean shouldCheck(ConnectedPlayer player) {
    InetSocketAddress adr = (InetSocketAddress) player.getConnection().getRemoteAddress();
    return shouldCheck(player.getUsername(), adr.getAddress());
  }

  public boolean shouldCheck(String nickname, InetAddress ip) {
    if (cachedFilterChecks.containsKey(nickname)) {
      return !ip.equals(cachedFilterChecks.get(nickname).getInetAddress());
    } else {
      return true;
    }
  }

  public void filter(Player player) {
    try {
      BotFilterSessionHandler botFilterSessionHandler =
          new BotFilterSessionHandler((ConnectedPlayer) player, this);

      filterServer.spawnPlayer(player, botFilterSessionHandler);
    } catch (Throwable t) {
      logger.error("Error", t);
    }
  }

  @SuppressFBWarnings("NP_IMMEDIATE_DEREFERENCE_OF_READLINE")
  private void checkForUpdates() {
    try {
      URL url = new URL("https://raw.githubusercontent.com/Elytrium/LimboAPI/master/VERSION");
      URLConnection conn = url.openConnection();
      conn.setConnectTimeout(1200);
      conn.setReadTimeout(1200);
      try (BufferedReader in = new BufferedReader(
          new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
        if (!Settings.IMP.VERSION.contains("-DEV")) {
          if (!in.readLine().trim().equalsIgnoreCase(net.elytrium.limboapi.config.Settings.IMP.VERSION)) {
            logger.error("****************************************");
            logger.warn("The new update was found, please update.");
            logger.error("****************************************");
          }
        }
      }
    } catch (IOException ex) {
      logger.warn("Unable to check for updates.", ex);
    }
  }

  private void checkCache(Map<String, CachedUser> userMap, long time) {
    userMap.entrySet().stream()
        .filter(u -> u.getValue().getCheckTime() + time <= System.currentTimeMillis())
        .map(Map.Entry::getKey)
        .forEach(userMap::remove);
  }

  public static FilterPlugin getInstance() {
    return instance;
  }

  @AllArgsConstructor
  @Getter
  private static class CachedUser {
    private InetAddress inetAddress;
    private long checkTime;
  }
}
