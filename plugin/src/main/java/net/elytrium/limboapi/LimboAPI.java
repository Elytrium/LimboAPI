/*
 * Copyright (C) 2021 - 2022 Elytrium
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
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.proxy.VelocityServer;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import net.elytrium.java.commons.config.YamlConfig;
import net.elytrium.java.commons.mc.serialization.Serializer;
import net.elytrium.java.commons.mc.serialization.Serializers;
import net.elytrium.java.commons.reflection.ReflectionException;
import net.elytrium.java.commons.updates.UpdatesChecker;
import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.LimboFactory;
import net.elytrium.limboapi.api.chunk.Dimension;
import net.elytrium.limboapi.api.chunk.VirtualBlock;
import net.elytrium.limboapi.api.chunk.VirtualChunk;
import net.elytrium.limboapi.api.chunk.VirtualWorld;
import net.elytrium.limboapi.api.material.Block;
import net.elytrium.limboapi.api.material.Item;
import net.elytrium.limboapi.api.material.VirtualItem;
import net.elytrium.limboapi.api.protocol.PacketDirection;
import net.elytrium.limboapi.api.protocol.PreparedPacket;
import net.elytrium.limboapi.api.protocol.packets.BuiltInPackets;
import net.elytrium.limboapi.api.protocol.packets.PacketMapping;
import net.elytrium.limboapi.injection.disconnect.DisconnectListener;
import net.elytrium.limboapi.injection.event.EventManagerHook;
import net.elytrium.limboapi.injection.login.LoginListener;
import net.elytrium.limboapi.injection.login.LoginTasksQueue;
import net.elytrium.limboapi.injection.packet.PlayerListItemHook;
import net.elytrium.limboapi.injection.packet.PreparedPacketImpl;
import net.elytrium.limboapi.protocol.LimboProtocol;
import net.elytrium.limboapi.server.CachedPackets;
import net.elytrium.limboapi.server.LimboImpl;
import net.elytrium.limboapi.server.world.SimpleBlock;
import net.elytrium.limboapi.server.world.SimpleItem;
import net.elytrium.limboapi.server.world.SimpleWorld;
import net.elytrium.limboapi.server.world.chunk.SimpleChunk;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.ComponentSerializer;
import org.bstats.velocity.Metrics;
import org.slf4j.Logger;

@Plugin(
    id = "limboapi",
    name = "LimboAPI",
    version = BuildConstants.LIMBO_VERSION,
    description = "Velocity plugin for making virtual servers.",
    url = "ely.su",
    authors = {
        "hevav",
        "mdxd44"
    }
)
@SuppressFBWarnings("MS_EXPOSE_REP")
public class LimboAPI implements LimboFactory {

  private static Logger logger;
  private static Serializer serializer;

  private final VelocityServer server;
  private final Metrics.Factory metricsFactory;
  private final File configFile;
  private final List<Player> players;
  private final CachedPackets packets;
  private final HashMap<Player, LoginTasksQueue> loginQueue;
  private final HashMap<Player, RegisteredServer> nextServer;
  private final HashMap<Player, UUID> initialID;

  private ProtocolVersion minVersion;
  private ProtocolVersion maxVersion;
  private LoginListener loginListener;

  @Inject
  @SuppressFBWarnings("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
  public LimboAPI(Logger logger, ProxyServer server, Metrics.Factory metricsFactory, @DataDirectory Path dataDirectory) {
    LimboAPI.logger = logger;

    this.server = (VelocityServer) server;
    this.metricsFactory = metricsFactory;
    this.configFile = new File(dataDirectory.toFile(), "config.yml");
    this.players = new ArrayList<>();
    this.packets = new CachedPackets(this);
    this.loginQueue = new HashMap<>();
    this.nextServer = new HashMap<>();
    this.initialID = new HashMap<>();

    if (ProtocolVersion.MAXIMUM_VERSION.getProtocol() < 758) {
      LimboAPI.logger.error("Please update Velocity. (https://papermc.io/downloads#Velocity)");
      this.server.shutdown();
      return;
    }

    LimboAPI.logger.info("Initializing Simple Virtual Block system...");
    SimpleBlock.init();
    LimboAPI.logger.info("Initializing Simple Virtual Item system...");
    SimpleItem.init();
    LimboAPI.logger.info("Hooking into EventManager, PlayerList and StateRegistry...");
    try {
      EventManagerHook.init(this);
      PlayerListItemHook.init(this);
      LimboProtocol.init();
    } catch (InvocationTargetException | InstantiationException | IllegalAccessException | ExecutionException e) {
      throw new ReflectionException(e);
    }
  }

  @Subscribe
  public void onProxyInitialization(ProxyInitializeEvent event) {
    this.metricsFactory.make(this, 12530);

    Settings.IMP.setLogger(logger);

    this.reload();

    if (Settings.IMP.MAIN.CHECK_FOR_UPDATES) {
      if (!UpdatesChecker.checkVersionByURL("https://raw.githubusercontent.com/Elytrium/LimboAPI/master/VERSION", Settings.IMP.VERSION)) {
        logger.error("****************************************");
        logger.warn("The new LimboAPI update was found, please update.");
        logger.error("https://github.com/Elytrium/LimboAPI/releases/");
        logger.error("****************************************");
      }
    }
  }

  @Subscribe(order = PostOrder.LAST)
  public void postProxyInitialization(ProxyInitializeEvent event) {
    EventManagerHook.postInit();
  }

  @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH", justification = "LEGACY_AMPERSAND can't be null in velocity.")
  public void reload() {
    if (Settings.IMP.reload(this.configFile, Settings.IMP.PREFIX) == YamlConfig.LoadResult.CONFIG_NOT_EXISTS) {
      logger.warn("************* FIRST LAUNCH *************");
      logger.warn("Thanks for installing LimboAPI!");
      logger.warn("(C) 2021 - 2022 Elytrium");
      logger.warn("");
      logger.warn("Check out our plugins here: https://ely.su/github <3");
      logger.warn("Discord: https://ely.su/discord");
      logger.warn("****************************************");
    }

    ComponentSerializer<Component, Component, String> serializer = Serializers.valueOf(Settings.IMP.SERIALIZER).getSerializer();
    if (serializer == null) {
      logger.warn("The specified serializer could not be founded, using default. (LEGACY_AMPERSAND)");
      setSerializer(new Serializer(Objects.requireNonNull(Serializers.LEGACY_AMPERSAND.getSerializer())));
    } else {
      setSerializer(new Serializer(serializer));
    }

    logger.info("Creating and preparing packets...");
    this.reloadVersion();
    this.packets.createPackets();
    this.loginListener = new LoginListener(this, this.server);
    this.server.getEventManager().register(this, this.loginListener);
    this.server.getEventManager().register(this, new DisconnectListener(this));
    logger.info("Loaded!");
  }

  private void reloadVersion() {
    if (Settings.IMP.MAIN.PREPARE_MAX_VERSION.equals("LATEST")) {
      this.maxVersion = ProtocolVersion.MAXIMUM_VERSION;
    } else {
      this.maxVersion = ProtocolVersion.valueOf("MINECRAFT_" + Settings.IMP.MAIN.PREPARE_MAX_VERSION);
    }

    this.minVersion = ProtocolVersion.valueOf("MINECRAFT_" + Settings.IMP.MAIN.PREPARE_MIN_VERSION);

    if (ProtocolVersion.MAXIMUM_VERSION.compareTo(this.maxVersion) > 0 || ProtocolVersion.MINIMUM_VERSION.compareTo(this.minVersion) < 0) {
      logger.warn(
          "Currently working only with "
          + this.minVersion.getVersionIntroducedIn() + " - " + this.maxVersion.getMostRecentSupportedVersion()
          + " versions, modify the plugins/limboapi/config.yml file if you want the plugin to work with other versions."
      );
    }
  }

  @Override
  public VirtualBlock createSimpleBlock(Block block) {
    return SimpleBlock.fromLegacyId((short) block.getId());
  }

  @Override
  public VirtualBlock createSimpleBlock(short legacyId) {
    return this.createSimpleBlock(legacyId, false);
  }

  @Override
  public VirtualBlock createSimpleBlock(String modernId, Map<String, String> properties) {
    return SimpleBlock.fromModernId(modernId, properties);
  }

  @Override
  public VirtualBlock createSimpleBlock(short id, boolean modern) {
    if (modern) {
      return SimpleBlock.solid(id);
    } else {
      return SimpleBlock.fromLegacyId(id);
    }
  }

  @Override
  public VirtualBlock createSimpleBlock(boolean solid, boolean air, boolean motionBlocking, short id) {
    return new SimpleBlock(solid, air, motionBlocking, id);
  }

  @Override
  public VirtualBlock createSimpleBlock(boolean solid, boolean air, boolean motionBlocking, String modernId, Map<String, String> properties) {
    return new SimpleBlock(solid, air, motionBlocking, modernId, properties);
  }

  @Override
  public VirtualWorld createVirtualWorld(Dimension dimension, double x, double y, double z, float yaw, float pitch) {
    return new SimpleWorld(dimension, x, y, z, yaw, pitch);
  }

  @Override
  public VirtualChunk createVirtualChunk(int x, int z) {
    return new SimpleChunk(x, z);
  }

  @Override
  public Limbo createLimbo(VirtualWorld world) {
    return new LimboImpl(this, world);
  }

  @Override
  public PreparedPacket createPreparedPacket() {
    return new PreparedPacketImpl(this.minVersion, this.maxVersion);
  }

  @Override
  public Object instantiatePacket(BuiltInPackets packetType, Object... data) {
    // TODO: Support for constructors with same arguments count.
    try {
      for (Constructor<?> constructor : packetType.getPacketClass().getDeclaredConstructors()) {
        if (constructor.getParameterCount() == data.length) {
          return constructor.newInstance(data);
        }
      }

      throw new IllegalArgumentException("No constructor found with the correct number of arguments!");
    } catch (InstantiationException | InvocationTargetException | IllegalAccessException e) {
      throw new ReflectionException(e);
    }
  }

  @Override
  public void registerPacket(PacketDirection direction, Class<?> packetClass, Supplier<?> packetSupplier, PacketMapping[] packetMappings) {
    LimboProtocol.register(direction, packetClass, packetSupplier, packetMappings);
  }

  @Override
  public void passLoginLimbo(Player player) {
    if (this.loginQueue.containsKey(player)) {
      this.loginQueue.get(player).next();
    }
  }

  @Override
  public VirtualItem getItem(Item item) {
    return SimpleItem.fromItem(item);
  }

  public VelocityServer getServer() {
    return this.server;
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

  public CachedPackets getPackets() {
    return this.packets;
  }

  public void addLoginQueue(Player player, LoginTasksQueue queue) {
    this.loginQueue.put(player, queue);
  }

  public void removeLoginQueue(Player player) {
    this.loginQueue.remove(player);
  }

  public boolean hasLoginQueue(Player player) {
    return this.loginQueue.containsKey(player);
  }

  public LoginTasksQueue getLoginQueue(Player player) {
    return this.loginQueue.get(player);
  }

  public void setNextServer(Player player, RegisteredServer nextServer) {
    this.nextServer.put(player, nextServer);
  }

  public void removeNextServer(Player player) {
    this.nextServer.remove(player);
  }

  public boolean hasNextServer(Player player) {
    return this.nextServer.containsKey(player);
  }

  public RegisteredServer getNextServer(Player player) {
    return this.nextServer.get(player);
  }

  public void setInitialID(Player player, UUID nextServer) {
    this.initialID.put(player, nextServer);
  }

  public void removeInitialID(Player player) {
    this.initialID.remove(player);
  }

  public UUID getInitialID(Player player) {
    return this.initialID.get(player);
  }

  public LoginListener getLoginListener() {
    return this.loginListener;
  }

  public static Logger getLogger() {
    return logger;
  }

  private static void setSerializer(Serializer serializer) {
    LimboAPI.serializer = serializer;
  }

  public static Serializer getSerializer() {
    return serializer;
  }
}

