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
import com.velocitypowered.api.plugin.PluginContainer;
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
import java.sql.SQLException;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import net.elytrium.limboapi.BuildConstants;
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
import net.elytrium.limboauth.handler.AuthSessionHandler;
import net.elytrium.limboauth.listener.AuthListener;
import net.elytrium.limboauth.model.RegisteredPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.slf4j.Logger;

@Plugin(
    id = "limboauth",
    name = "LimboAuth",
    version = BuildConstants.LIMBO_VERSION,
    url = "ely.su",
    authors = {"hevav", "mdxd44"},
    dependencies = {@Dependency(id = "limboapi")}
)

public class AuthPlugin {

  private static AuthPlugin instance;

  private final HttpClient client = HttpClient.newHttpClient();
  private final Path dataDirectory;
  private final Logger logger;
  private final ProxyServer server;
  private final LimboFactory factory;

  private Dao<RegisteredPlayer, String> playerDao;
  private Limbo authServer;
  private Map<String, CachedUser> cachedAuthChecks;
  private Component nicknameInvalid;
  private Pattern nicknameValidationPattern;

  @Inject
  @SuppressWarnings("OptionalGetWithoutIsPresent")
  public AuthPlugin(ProxyServer server, Logger logger, @Named("limboapi") PluginContainer factory, @DataDirectory Path dataDirectory) {
    setInstance(this);

    this.server = server;
    this.logger = logger;
    this.dataDirectory = dataDirectory;
    this.factory = (LimboFactory) factory.getInstance().get();
  }

  @Subscribe
  public void onProxyInitialization(ProxyInitializeEvent event) throws SQLException {
    this.server.getEventManager().register(this, new AuthListener());
    this.reload();
  }

  private static void setInstance(AuthPlugin thisInst) {
    instance = thisInst;
  }

  @SuppressWarnings("SwitchStatementWithTooFewBranches")
  public void reload() throws SQLException {
    Settings.IMP.reload(new File(this.dataDirectory.toFile().getAbsoluteFile(), "config.yml"));

    this.cachedAuthChecks = new ConcurrentHashMap<>();

    System.setProperty("com.j256.simplelogging.level", "ERROR");

    Settings.DATABASE dbConfig = Settings.IMP.DATABASE;

    JdbcPooledConnectionSource connectionSource;
    switch (dbConfig.STORAGE_TYPE.toLowerCase(Locale.ROOT)) {
      case "sqlite": {
        connectionSource = new JdbcPooledConnectionSource("jdbc:sqlite:" + this.dataDirectory.toFile().getAbsoluteFile() + "/" + "limboauth.db");
        break;
      }
      case "h2": {
        connectionSource = new JdbcPooledConnectionSource("jdbc:h2:" + this.dataDirectory.toFile().getAbsoluteFile() + "/" + "limboauth");
        break;
      }
      case "mysql": {
        connectionSource = new JdbcPooledConnectionSource(
            "jdbc:mysql://" + dbConfig.HOSTNAME + "/" + dbConfig.DATABASE
                + "?autoReconnect=true&initialTimeout=1&useSSL=false", dbConfig.USER, dbConfig.PASSWORD);
        break;
      }
      case "postgresql": {
        connectionSource = new JdbcPooledConnectionSource(
            "jdbc:postgresql://" + dbConfig.HOSTNAME + "/" + dbConfig.DATABASE + "?autoReconnect=true", dbConfig.USER, dbConfig.PASSWORD);
        break;
      }
      default: {
        this.logger.error("WRONG DATABASE TYPE.");
        this.server.shutdown();
        return;
      }
    }

    TableUtils.createTableIfNotExists(connectionSource, RegisteredPlayer.class);

    this.playerDao = DaoManager.createDao(connectionSource, RegisteredPlayer.class);
    this.nicknameValidationPattern = Pattern.compile(Settings.IMP.MAIN.ALLOWED_NICKNAME_REGEX);

    CommandManager manager = this.server.getCommandManager();
    manager.unregister("unregister");
    manager.unregister("changepass");
    manager.unregister("2fa");
    manager.unregister("authreload");

    manager.register("unregister", new UnregisterCommand(this.playerDao));
    manager.register("changepass", new ChangePasswordCommand(this.playerDao));
    if (Settings.IMP.MAIN.ENABLE_TOTP) {
      manager.register("2fa", new TotpCommand(this.playerDao));
    }
    manager.register("authreload", new AuthReloadCommand());

    Settings.MAIN.AUTH_COORDS authCoords = Settings.IMP.MAIN.AUTH_COORDS;
    VirtualWorld authWorld = this.factory.createVirtualWorld(
        Dimension.valueOf(Settings.IMP.MAIN.DIMENSION),
        authCoords.X, authCoords.Y, authCoords.Z,
        (float) authCoords.YAW, (float) authCoords.PITCH
    );

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

    this.authServer = this.factory.createLimbo(authWorld);

    this.nicknameInvalid = LegacyComponentSerializer.legacyAmpersand().deserialize(Settings.IMP.MAIN.STRINGS.NICKNAME_INVALID);

    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, task -> new Thread(task, "purge-cache"));

    scheduler.scheduleAtFixedRate(() ->
        this.checkCache(this.cachedAuthChecks, Settings.IMP.MAIN.PURGE_CACHE_MILLIS),
        Settings.IMP.MAIN.PURGE_CACHE_MILLIS, Settings.IMP.MAIN.PURGE_CACHE_MILLIS, TimeUnit.MILLISECONDS);
  }

  public void cacheAuthUser(Player player) {
    String username = player.getUsername();
    this.cachedAuthChecks.remove(username);
    InetSocketAddress adr = player.getRemoteAddress();
    this.cachedAuthChecks.put(username, new CachedUser(adr.getAddress(), System.currentTimeMillis()));
  }

  public boolean needAuth(Player player) {
    String username = player.getUsername();

    if (!Settings.IMP.MAIN.ONLINE_MODE_NEED_AUTH && player.isOnlineMode()) {
      return false;
    }

    if (!this.cachedAuthChecks.containsKey(username)) {
      return true;
    }

    InetSocketAddress adr = player.getRemoteAddress();
    return !this.cachedAuthChecks.get(username).getInetAddress().equals(adr.getAddress());
  }

  public void auth(Player player) {
    String nickname = player.getUsername();
    if (!this.nicknameValidationPattern.matcher(nickname).matches()) {
      player.disconnect(this.nicknameInvalid);
      return;
    }

    this.sendToAuthServer(player, nickname);
  }

  private void sendToAuthServer(Player player, String nickname) {
    try {
      this.authServer.spawnPlayer(player, new AuthSessionHandler(this.playerDao, player, nickname));
    } catch (Throwable t) {
      this.logger.error("Error", t);
    }
  }

  public boolean isPremium(String nickname) {
    try {
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(String.format(Settings.IMP.MAIN.ISPREMIUM_AUTH_URL, nickname)))
          .build();
      HttpResponse<String> response = this.client.send(request, HttpResponse.BodyHandlers.ofString());
      return response.statusCode() == 200;
    } catch (IOException | InterruptedException e) {
      this.logger.error("Unable to authenticate with Mojang", e);
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
