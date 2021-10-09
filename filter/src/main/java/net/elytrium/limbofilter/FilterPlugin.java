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
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.elytrium.limboapi.BuildConstants;
import net.elytrium.limboapi.LimboAPI;
import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.LimboFactory;
import net.elytrium.limboapi.api.chunk.Dimension;
import net.elytrium.limboapi.api.chunk.VirtualWorld;
import net.elytrium.limboapi.api.file.SchematicFile;
import net.elytrium.limboapi.api.file.WorldFile;
import net.elytrium.limbofilter.cache.CachedCaptcha;
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
    version = BuildConstants.LIMBO_VERSION,
    url = "ely.su",
    authors = {"hevav", "mdxd44"},
    dependencies = {@Dependency(id = "limboapi")}
)

public class FilterPlugin {

  private static FilterPlugin instance;

  private final Path dataDirectory;
  private final Logger logger;
  private final ProxyServer server;
  private final LimboFactory factory;
  private final CachedPackets packets;
  private final Statistics statistics;

  private Map<String, CachedUser> cachedFilterChecks;
  private CachedCaptcha cachedCaptcha;
  private Limbo filterServer;

  @Inject
  @SuppressWarnings("OptionalGetWithoutIsPresent")
  public FilterPlugin(ProxyServer server, Logger logger, @Named("limboapi") PluginContainer factory, @DataDirectory Path dataDirectory) {
    setInstance(this);

    this.server = server;
    this.logger = logger;
    this.dataDirectory = dataDirectory;
    this.factory = (LimboFactory) factory.getInstance().get();
    this.packets = new CachedPackets();
    this.statistics = new Statistics();
    this.statistics.startUpdating();
  }

  private static void setInstance(FilterPlugin thisInst) {
    instance = thisInst;
  }

  @Subscribe
  public void onProxyInitialization(ProxyInitializeEvent event) {
    this.server.getEventManager().register(this, new FilterListener(this));
    this.reload();
  }

  @SuppressWarnings("SwitchStatementWithTooFewBranches")
  public void reload() {
    Settings.IMP.reload(new File(this.dataDirectory.toFile().getAbsoluteFile(), "config.yml"));

    BotFilterSessionHandler.reload();

    this.cachedCaptcha = new CachedCaptcha();
    CaptchaGeneration.init();
    this.packets.createPackets();

    this.cachedFilterChecks = new ConcurrentHashMap<>();

    Settings.MAIN.COORDS captchaCoords = Settings.IMP.MAIN.COORDS;
    VirtualWorld authWorld = this.factory.createVirtualWorld(
        Dimension.valueOf(Settings.IMP.MAIN.BOTFILTER_DIMENSION),
        captchaCoords.CAPTCHA_X, captchaCoords.CAPTCHA_Y, captchaCoords.CAPTCHA_Z,
        (float) captchaCoords.CAPTCHA_YAW, (float) captchaCoords.CAPTCHA_PITCH);

    if (Settings.IMP.MAIN.LOAD_WORLD) {
      try {
        Path path = this.dataDirectory.resolve(Settings.IMP.MAIN.WORLD_FILE_PATH);
        WorldFile file;
        switch (Settings.IMP.MAIN.WORLD_FILE_TYPE) {
          case "schematic": {
            file = new SchematicFile(path);
            break;
          }
          default: {
            this.logger.error("Incorrect world file type.");
            this.server.shutdown();
            return;
          }
        }

        Settings.MAIN.WORLD_COORDS coords = Settings.IMP.MAIN.WORLD_COORDS;
        file.toWorld(this.factory, authWorld, coords.X, coords.Y, coords.Z);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    this.filterServer = this.factory.createLimbo(authWorld);

    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, task -> new Thread(task, "purge-cache"));

    scheduler.scheduleAtFixedRate(
        () -> this.checkCache(this.cachedFilterChecks, Settings.IMP.MAIN.PURGE_CACHE_MILLIS),
        Settings.IMP.MAIN.PURGE_CACHE_MILLIS,
        Settings.IMP.MAIN.PURGE_CACHE_MILLIS,
        TimeUnit.MILLISECONDS);

    CommandManager manager = this.server.getCommandManager();
    manager.unregister("limbofilter");
    manager.register("limbofilter", new FilterCommand((VelocityServer) this.server, this), "lf", "botfilter", "bf");
  }

  public void cacheFilterUser(Player player) {
    String username = player.getUsername();
    this.cachedFilterChecks.remove(username);
    this.cachedFilterChecks.put(username, new CachedUser(player.getRemoteAddress().getAddress(), System.currentTimeMillis()));
  }

  public boolean shouldCheck(ConnectedPlayer player) {
    if (!this.checkLimit(Settings.IMP.MAIN.CONNECTION_LIMIT.ALL_BYPASS)) {
      return false;
    }

    if (player.isOnlineMode() && !this.checkLimit(Settings.IMP.MAIN.CONNECTION_LIMIT.ONLINE_MODE_BYPASS)) {
      return false;
    }

    return this.shouldCheck(player.getUsername(), ((InetSocketAddress) player.getConnection().getRemoteAddress()).getAddress());
  }

  public boolean shouldCheck(String nickname, InetAddress ip) {
    if (this.cachedFilterChecks.containsKey(nickname)) {
      return !ip.equals(this.cachedFilterChecks.get(nickname).getInetAddress());
    } else {
      return true;
    }
  }

  public void filter(Player player) {
    try {
      this.filterServer.spawnPlayer(player, new BotFilterSessionHandler((ConnectedPlayer) player, this));
    } catch (Throwable t) {
      this.logger.error("Error", t);
    }
  }

  private void checkCache(Map<String, CachedUser> userMap, long time) {
    userMap.entrySet().stream()
        .filter(u -> u.getValue().getCheckTime() + time <= System.currentTimeMillis())
        .map(Map.Entry::getKey)
        .forEach(userMap::remove);
  }

  public boolean checkLimit(int limit) {
    if (limit == -1) {
      return false;
    }

    return limit <= this.statistics.getConnectionsPerSecond();
  }

  public static FilterPlugin getInstance() {
    return instance;
  }

  public Logger getLogger() {
    return this.logger;
  }

  public LimboFactory getFactory() {
    return this.factory;
  }

  public CachedPackets getPackets() {
    return this.packets;
  }

  public Statistics getStatistics() {
    return this.statistics;
  }

  public CachedCaptcha getCachedCaptcha() {
    return this.cachedCaptcha;
  }

  private static class CachedUser {

    private final InetAddress inetAddress;
    private final long checkTime;

    public CachedUser(InetAddress inetAddress, long checkTime) {
      this.inetAddress = inetAddress;
      this.checkTime = checkTime;
    }

    public InetAddress getInetAddress() {
      return this.inetAddress;
    }

    public long getCheckTime() {
      return this.checkTime;
    }
  }
}
