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
import java.util.HashMap;
import java.util.List;
import lombok.Getter;
import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.LimboFactory;
import net.elytrium.limboapi.api.chunk.Dimension;
import net.elytrium.limboapi.api.chunk.VirtualBlock;
import net.elytrium.limboapi.api.chunk.VirtualWorld;
import net.elytrium.limboapi.api.material.Block;
import net.elytrium.limboapi.api.material.Item;
import net.elytrium.limboapi.api.material.VirtualItem;
import net.elytrium.limboapi.config.Settings;
import net.elytrium.limboapi.injection.disconnect.DisconnectListener;
import net.elytrium.limboapi.injection.login.LoginListener;
import net.elytrium.limboapi.injection.login.LoginTasksQueue;
import net.elytrium.limboapi.protocol.LimboProtocol;
import net.elytrium.limboapi.server.CachedPackets;
import net.elytrium.limboapi.server.LimboImpl;
import net.elytrium.limboapi.server.world.SimpleBlock;
import net.elytrium.limboapi.server.world.SimpleItem;
import net.elytrium.limboapi.server.world.SimpleWorld;
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
@SuppressFBWarnings("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
public class LimboAPI implements LimboFactory {

  private static LimboAPI instance;
  private final VelocityServer server;
  private final Logger logger;
  private final Metrics.Factory metricsFactory;
  private final Path dataDirectory;
  private final List<Player> players;
  private final HashMap<Player, LoginTasksQueue> loginQueue;

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
    this.loginQueue = new HashMap<>();

    logger.info("Initializing Simple Virtual Block system...");
    SimpleBlock.init();
    logger.info("Initializing Simple Virtual Item system...");
    SimpleItem.init();
    logger.info("Initializing LimboProtocol...");
    LimboProtocol.init();
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
    logger.info("Creating and preparing packets...");
    packets.createPackets();
    server.getEventManager().register(this, new LoginListener(this, server));
    server.getEventManager().register(this, new DisconnectListener(this));
    logger.info("Loaded!");
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

  @Override
  public VirtualBlock createSimpleBlock(Block block) {
    return SimpleBlock
        .fromLegacyId((short) block.getId())
        .setData(block.getData());
  }

  @Override
  public VirtualBlock createSimpleBlock(short legacyId, byte data) {
    return SimpleBlock
        .fromLegacyId(legacyId)
        .setData(data);
  }

  @Override
  public VirtualBlock createSimpleBlock(
      boolean solid, boolean air, boolean motionBlocking, SimpleBlock.BlockInfo... blockInfos) {
    return new SimpleBlock(solid, air, motionBlocking, blockInfos);
  }

  @Override
  public VirtualItem getItem(Item item) {
    return SimpleItem.fromItem(item);
  }

  @Override
  public Limbo createLimbo(VirtualWorld world) {
    return new LimboImpl(this, world);
  }

  @Override
  public VirtualWorld createVirtualWorld(Dimension dimension, double x, double y, double z, float yaw, float pitch) {
    return new SimpleWorld(dimension, x, y, z, yaw, pitch);
  }

  public void setLimboJoined(Player player) {
    players.add(player);
  }

  public void unsetLimboJoined(Player player) {
    players.remove(player);
  }

  public boolean isLimboJoined(Player player) {
    return players.contains(player);
  }

  public LoginTasksQueue getLoginQueue(Player player) {
    return loginQueue.get(player);
  }

  public void addQueue(Player player, LoginTasksQueue q) {
    loginQueue.put(player, q);
  }

  public void removeQueue(Player player) {
    loginQueue.remove(player);
  }

  public static LimboAPI getInstance() {
    return instance;
  }
}
