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
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;
import net.elytrium.limboapi.api.LimboFactory;
import net.elytrium.limboapi.api.chunk.Dimension;
import net.elytrium.limboapi.api.chunk.VirtualWorld;
import net.elytrium.limbofilter.config.Settings;
import net.elytrium.limbofilter.handler.BotFilterSessionHandler;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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
public class FilterPlugin {
  private static FilterPlugin instance;
  private final Path dataDirectory;
  private final Logger logger;
  private final ProxyServer server;
  private final LimboFactory factory;
  private Map<String, CachedUser> cachedFilterChecks;
  private ScheduledExecutorService scheduler;

  @Inject
  public FilterPlugin(ProxyServer server,
                    Logger logger,
                    @Named("limboapi") LimboFactory factory,
                    @DataDirectory Path dataDirectory) {
    this.server = server;
    this.logger = logger;
    this.dataDirectory = dataDirectory;
    this.factory = factory;
  }

  @Subscribe
  public void onProxyInitialization(ProxyInitializeEvent event) {
    instance = this;
    reload();
  }

  @SneakyThrows
  public void reload() {
    Settings.IMP.reload(new File(dataDirectory.toFile().getAbsoluteFile(), "config.yml"));

    cachedFilterChecks = new ConcurrentHashMap<>();

    Settings.MAIN.CAPTCHA_COORDS captchaCoords = Settings.IMP.MAIN.CAPTCHA_COORDS;
    VirtualWorld authWorld = factory.createVirtualWorld(
        Dimension.valueOf(Settings.IMP.MAIN.BOTFILTER_DIMENSION),
        captchaCoords.X, captchaCoords.Y, captchaCoords.Z,
        (float) captchaCoords.YAW, (float) captchaCoords.PITCH);

    if (Settings.IMP.MAIN.) {
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

        Settings.MAIN.VIRTUAL_COORDS coords = Settings.IMP.MAIN.VIRTUAL_COORDS;
        file.toWorld(factory, authWorld, coords.X, coords.Y, coords.Z);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    authServer = factory.createVirtualServer(authWorld);

    nicknameInvalid = LegacyComponentSerializer.legacyAmpersand()
        .deserialize(Settings.IMP.MAIN.STRINGS.NICKNAME_INVALID);
    nicknamePremium = LegacyComponentSerializer.legacyAmpersand()
        .deserialize(Settings.IMP.MAIN.STRINGS.NICKNAME_PREMIUM);

    server.getEventManager().register(this, new AuthListener());

    scheduler =
        Executors.newScheduledThreadPool(1, task -> new Thread(task, "purge-cache"));

    scheduler.scheduleAtFixedRate(
        () -> checkCache(cachedFilterChecks, Settings.IMP.MAIN.PURGE_CACHE_MILLIS),
        Settings.IMP.MAIN.PURGE_CACHE_MILLIS,
        Settings.IMP.MAIN.PURGE_CACHE_MILLIS,
        TimeUnit.MILLISECONDS);
  }

  public void cacheAuthUser(Player player) {
    String username = player.getUsername();
    cachedFilterChecks.remove(username);
    InetSocketAddress adr = player.getRemoteAddress();
    cachedFilterChecks.put(username, new CachedUser(adr.getAddress(), System.currentTimeMillis()));
  }

  public void filter(Player player) {
    try {
      BotFilterSessionHandler botFilterSessionHandler =
          new BotFilterSessionHandler(player.getUsername());

      filterServer.spawnPlayer(player, botFilterSessionHandler);
    } catch (Throwable t) {
      logger.error("Error", t);
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
