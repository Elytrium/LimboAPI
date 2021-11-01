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
import com.velocitypowered.proxy.protocol.StateRegistry;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.LimboFactory;
import net.elytrium.limboapi.api.chunk.Dimension;
import net.elytrium.limboapi.api.chunk.VirtualBlock;
import net.elytrium.limboapi.api.chunk.VirtualWorld;
import net.elytrium.limboapi.api.material.Block;
import net.elytrium.limboapi.api.material.Item;
import net.elytrium.limboapi.api.material.VirtualItem;
import net.elytrium.limboapi.api.protocol.PacketDirection;
import net.elytrium.limboapi.api.protocol.PreparedPacket;
import net.elytrium.limboapi.injection.disconnect.DisconnectListener;
import net.elytrium.limboapi.injection.login.LoginListener;
import net.elytrium.limboapi.injection.login.LoginTasksQueue;
import net.elytrium.limboapi.injection.packet.PreparedPacketImpl;
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
    version = BuildConstants.LIMBO_VERSION,
    description = "Velocity plugin for making virtual servers ",
    url = "ely.su",
    authors = {"hevav", "mdxd44"}
)

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
  public LimboAPI(ProxyServer server, Logger logger, Metrics.Factory metricsFactory, @DataDirectory Path dataDirectory) {
    setInstance(this);

    this.server = (VelocityServer) server;
    this.logger = logger;
    this.metricsFactory = metricsFactory;
    this.dataDirectory = dataDirectory;
    this.players = new ArrayList<>();
    this.loginQueue = new HashMap<>();

    this.logger.info("Initializing Simple Virtual Block system...");
    SimpleBlock.init();
    this.logger.info("Initializing Simple Virtual Item system...");
    SimpleItem.init();
    this.logger.info("Initializing LimboProtocol...");
    try {
      LimboProtocol.init();
    } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
      e.printStackTrace();
    }
  }

  private static void setInstance(LimboAPI thisInst) {
    instance = thisInst;
  }

  @Subscribe
  public void onProxyInitialization(ProxyInitializeEvent event) {
    this.metricsFactory.make(this, 12530);
    this.packets = new CachedPackets();
    this.players.clear();

    this.reload();
    this.checkForUpdates();
  }

  public void reload() {
    Settings.IMP.reload(new File(this.dataDirectory.toFile().getAbsoluteFile(), "config.yml"));
    this.logger.info("Creating and preparing packets...");
    this.packets.createPackets();
    this.server.getEventManager().register(this, new LoginListener(this, this.server));
    this.server.getEventManager().register(this, new DisconnectListener(this));
    this.logger.info("Loaded!");
  }

  private void checkForUpdates() {
    if (!Settings.IMP.VERSION.contains("-DEV") && !Settings.IMP.VERSION.contains("-rc")) {
      try {
        URL url = new URL("https://raw.githubusercontent.com/Elytrium/LimboAPI/master/VERSION");
        URLConnection conn = url.openConnection();
        int timeout = (int) TimeUnit.SECONDS.toMillis(4);
        conn.setConnectTimeout(timeout);
        conn.setReadTimeout(timeout);
        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
          String version = in.readLine();
          if (version != null && !version.trim().equalsIgnoreCase(Settings.IMP.VERSION)) {
            this.logger.error("****************************************");
            this.logger.warn("The new LimboAPI update was found, please update.");
            this.logger.error("https://github.com/Elytrium/LimboAPI/releases/");
            this.logger.error("****************************************");
          }
        }
      } catch (IOException ex) {
        this.logger.warn("Unable to check for updates.", ex);
      }
    }
  }

  @Override
  public VirtualBlock createSimpleBlock(Block block) {
    return SimpleBlock.fromLegacyId((short) block.getId()).setData(block.getData());
  }

  @Override
  public VirtualBlock createSimpleBlock(short legacyId, byte data) {
    return SimpleBlock.fromLegacyId(legacyId).setData(data);
  }

  @Override
  public VirtualBlock createSimpleBlock(boolean solid, boolean air, boolean motionBlocking, SimpleBlock.BlockInfo... blockInfos) {
    return new SimpleBlock(solid, air, motionBlocking, blockInfos);
  }

  @Override
  public Limbo createLimbo(VirtualWorld world) {
    return new LimboImpl(this, world);
  }

  @Override
  public VirtualWorld createVirtualWorld(Dimension dimension, double x, double y, double z, float yaw, float pitch) {
    return new SimpleWorld(dimension, x, y, z, yaw, pitch);
  }

  @Override
  public PreparedPacket createPreparedPacket() {
    return new PreparedPacketImpl();
  }

  @Override
  public void registerPacket(PacketDirection direction, Class<?> packetClass, Supplier<?> packetSupplier, StateRegistry.PacketMapping[] packetMappings) {
    LimboProtocol.register(direction, packetClass, packetSupplier, packetMappings);
  }

  @Override
  public VirtualItem getItem(Item item) {
    return SimpleItem.fromItem(item);
  }

  public void setLimboJoined(Player player) {
    this.players.add(player);
  }

  public void unsetLimboJoined(Player player) {
    this.players.remove(player);
  }

  public boolean isLimboJoined(Player player) {
    return this.players.contains(player);
  }

  public LoginTasksQueue getLoginQueue(Player player) {
    return this.loginQueue.get(player);
  }

  public boolean hasLoginQueue(Player player) {
    return this.loginQueue.containsKey(player);
  }

  public void addLoginQueue(Player player, LoginTasksQueue q) {
    this.loginQueue.put(player, q);
  }

  public void removeLoginQueue(Player player) {
    this.loginQueue.remove(player);
  }

  public static LimboAPI getInstance() {
    return instance;
  }

  public VelocityServer getServer() {
    return this.server;
  }

  public Logger getLogger() {
    return this.logger;
  }

  public CachedPackets getPackets() {
    return this.packets;
  }
}
