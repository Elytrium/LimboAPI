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

package net.elytrium.limboapi;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.proxy.VelocityServer;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import net.elytrium.limboapi.config.Settings;
import net.elytrium.limboapi.injection.DisconnectListener;
import net.elytrium.limboapi.injection.HandshakeListener;
import net.elytrium.limboapi.server.CachedPackets;
import org.bstats.velocity.Metrics;
import org.slf4j.Logger;

@Plugin(
    id = "limboapi",
    name = "LimboAPI",
    version = "@version@",
    description = "Velocity plugin for making virtual servers ",
    url = "ely.su",
    authors = {"hevav", "mdxd44"}
)

@Getter
public class LimboAPI {

  private static LimboAPI instance;
  private final VelocityServer server;
  private final Logger logger;
  private final Metrics.Factory metricsFactory;
  private final Path dataDirectory;
  private final List<Player> players;

  private CachedPackets packets;

  @Inject
  public LimboAPI(
      ProxyServer server, Logger logger, Metrics.Factory metricsFactory, @DataDirectory Path dataDirectory) {
    instance = this;

    this.server = (VelocityServer) server;
    this.logger = logger;
    this.metricsFactory = metricsFactory;
    this.dataDirectory = dataDirectory;
    this.players = new ArrayList<>();
  }

  @Subscribe
  public void onProxyInitialization(ProxyInitializeEvent event) {
    metricsFactory.make(this, 12530);
    packets = new CachedPackets(this);
    players.clear();

    reload();
    checkForUpdates();
  }

  public void reload() {
    Settings.IMP.reload(new File(dataDirectory.toFile().getAbsoluteFile(), "config.yml"));
    packets.createPackets();
    server.getEventManager().register(this, new HandshakeListener(this));
    server.getEventManager().register(this, new DisconnectListener(this));
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
          if (!in.readLine().trim().equalsIgnoreCase(Settings.IMP.VERSION)) {
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

  public void setVirtualServerJoined(Player player) {
    players.add(player);
  }

  public void unsetVirtualServerJoined(Player player) {
    players.remove(player);
  }

  public boolean isVirtualServerJoined(Player player) {
    return players.contains(player);
  }

  public static LimboAPI getInstance() {
    return instance;
  }
}
