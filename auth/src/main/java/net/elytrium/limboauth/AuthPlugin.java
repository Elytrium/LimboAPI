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

package net.elytrium.limboauth;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcPooledConnectionSource;
import com.j256.ormlite.table.TableUtils;
import com.velocitypowered.api.command.CommandManager;
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
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.Locale;
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
import net.elytrium.limboauth.command.AuthReloadCommand;
import net.elytrium.limboauth.command.ChangePasswordCommand;
import net.elytrium.limboauth.command.TotpCommand;
import net.elytrium.limboauth.command.UnregisterCommand;
import net.elytrium.limboauth.config.Settings;
import net.elytrium.limboauth.handler.AuthSessionHandler;
import net.elytrium.limboauth.listener.AuthListener;
import net.elytrium.limboauth.model.RegisteredPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.slf4j.Logger;

@Plugin(
    id = "limboauth",
    name = "LimboAuth",
    version = "1.0.0",
    url = "ely.su",
    authors = {"hevav", "mdxd44"},
    dependencies = {@Dependency(id = "limboapi")}
)

@Getter
public class AuthPlugin {
  private final HttpClient client = HttpClient.newHttpClient();
  private static AuthPlugin instance;
  private final Path dataDirectory;
  private final Logger logger;
  private final ProxyServer server;
  private final LimboFactory factory;
  private Dao<RegisteredPlayer, String> playerDao;
  private Limbo authServer;
  private Map<String, CachedUser> cachedAuthChecks;
  private ScheduledExecutorService scheduler;

  private Component nicknameInvalid;
  private Component nicknamePremium;

  @Inject
  public AuthPlugin(ProxyServer server,
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

    cachedAuthChecks = new ConcurrentHashMap<>();

    CommandManager manager = server.getCommandManager();
    JdbcPooledConnectionSource connectionSource
        = new JdbcPooledConnectionSource("jdbc:h2:mem:myDb"); // TODO for @mdxd44: Switch db

    TableUtils.createTableIfNotExists(connectionSource, RegisteredPlayer.class);

    playerDao
        = DaoManager.createDao(connectionSource, RegisteredPlayer.class);

    manager.unregister("unregister");
    manager.unregister("changepass");
    manager.unregister("2fa");
    manager.unregister("reload");

    manager.register("unregister", new UnregisterCommand(playerDao));
    manager.register("changepass", new ChangePasswordCommand(playerDao));
    manager.register("2fa", new TotpCommand(playerDao));
    manager.register("reload", new AuthReloadCommand());

    Settings.MAIN.AUTH_COORDS authCoords = Settings.IMP.MAIN.AUTH_COORDS;
    VirtualWorld authWorld = factory.createVirtualWorld(
        Dimension.valueOf(Settings.IMP.MAIN.DIMENSION),
        authCoords.X, authCoords.Y, authCoords.Z,
        (float) authCoords.YAW, (float) authCoords.PITCH);

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
        () -> checkCache(cachedAuthChecks, Settings.IMP.MAIN.PURGE_CACHE_MILLIS),
        Settings.IMP.MAIN.PURGE_CACHE_MILLIS,
        Settings.IMP.MAIN.PURGE_CACHE_MILLIS,
        TimeUnit.MILLISECONDS);
  }

  public void cacheAuthUser(Player player) {
    String username = player.getUsername();
    cachedAuthChecks.remove(username);
    InetSocketAddress adr = player.getRemoteAddress();
    cachedAuthChecks.put(username, new CachedUser(adr.getAddress(), System.currentTimeMillis()));
  }

  public boolean needAuth(Player player) {
    String username = player.getUsername();

    if (!Settings.IMP.MAIN.ONLINE_MODE_NEED_AUTH && player.isOnlineMode()) {
      return false;
    }

    if (!cachedAuthChecks.containsKey(username)) {
      return true;
    }

    InetSocketAddress adr = player.getRemoteAddress();
    return !cachedAuthChecks.get(username).getInetAddress().equals(adr.getAddress());
  }

  public void auth(Player player) {
    String nickname = player.getUsername().toLowerCase(Locale.ROOT);
    for (char character : nickname.toCharArray()) {
      if (!Settings.IMP.MAIN.ALLOWED_NICKNAME_CHARS.contains(String.valueOf(character))) {
        player.disconnect(nicknameInvalid);
        return;
      }
    }

    sendToAuthServer(player, nickname);
  }

  private void sendToAuthServer(Player player, String nickname) {
    try {
      AuthSessionHandler authSessionHandler =
          new AuthSessionHandler(playerDao, nickname);

      authServer.spawnPlayer(player, authSessionHandler);
    } catch (Throwable t) {
      logger.error("Error", t);
    }
  }

  public boolean isPremium(String nickname) {
    try {
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create("https://api.mojang.com/users/profiles/minecraft/" + nickname)).build();
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      return response.statusCode() == 200;
    } catch (IOException | InterruptedException e) {
      logger.error("Unable to authenticate with Mojang", e);
      return true;
    }
  }

  private void checkCache(Map<String, CachedUser> userMap, long time) {
    userMap.entrySet().stream()
        .filter(u -> u.getValue().getCheckTime() + time <= System.currentTimeMillis())
        .map(Map.Entry::getKey)
        .forEach(userMap::remove);
  }

  public static AuthPlugin getInstance() {
    return instance;
  }

  @AllArgsConstructor
  @Getter
  private static class CachedUser {
    private InetAddress inetAddress;
    private long checkTime;
  }
}
