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
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.natives.compression.VelocityCompressor;
import com.velocitypowered.natives.util.Natives;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.event.VelocityEventManager;
import com.velocitypowered.proxy.network.Connections;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.netty.MinecraftCompressDecoder;
import com.velocitypowered.proxy.protocol.netty.MinecraftCompressorAndLengthEncoder;
import com.velocitypowered.proxy.protocol.netty.MinecraftVarintLengthEncoder;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import net.elytrium.fastprepare.PreparedPacketFactory;
import net.elytrium.java.commons.config.YamlConfig;
import net.elytrium.java.commons.mc.serialization.Serializer;
import net.elytrium.java.commons.mc.serialization.Serializers;
import net.elytrium.java.commons.reflection.ReflectionException;
import net.elytrium.java.commons.updates.UpdatesChecker;
import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.LimboFactory;
import net.elytrium.limboapi.api.chunk.BuiltInBiome;
import net.elytrium.limboapi.api.chunk.Dimension;
import net.elytrium.limboapi.api.chunk.VirtualBiome;
import net.elytrium.limboapi.api.chunk.VirtualBlock;
import net.elytrium.limboapi.api.chunk.VirtualChunk;
import net.elytrium.limboapi.api.chunk.VirtualWorld;
import net.elytrium.limboapi.api.material.Block;
import net.elytrium.limboapi.api.material.Item;
import net.elytrium.limboapi.api.material.VirtualItem;
import net.elytrium.limboapi.api.protocol.PacketDirection;
import net.elytrium.limboapi.api.protocol.PreparedPacket;
import net.elytrium.limboapi.api.protocol.packets.BuiltInPackets;
import net.elytrium.limboapi.api.protocol.packets.PacketFactory;
import net.elytrium.limboapi.api.protocol.packets.PacketMapping;
import net.elytrium.limboapi.injection.disconnect.DisconnectListener;
import net.elytrium.limboapi.injection.event.EventManagerHook;
import net.elytrium.limboapi.injection.kick.KickListener;
import net.elytrium.limboapi.injection.login.LoginListener;
import net.elytrium.limboapi.injection.login.LoginTasksQueue;
import net.elytrium.limboapi.injection.packet.MinecraftDiscardCompressDecoder;
import net.elytrium.limboapi.injection.packet.MinecraftLimitedCompressDecoder;
import net.elytrium.limboapi.injection.packet.PlayerListItemHook;
import net.elytrium.limboapi.injection.packet.PreparedPacketImpl;
import net.elytrium.limboapi.material.Biome;
import net.elytrium.limboapi.protocol.LimboProtocol;
import net.elytrium.limboapi.protocol.packets.PacketFactoryImpl;
import net.elytrium.limboapi.server.CachedPackets;
import net.elytrium.limboapi.server.LimboImpl;
import net.elytrium.limboapi.server.world.SimpleBlock;
import net.elytrium.limboapi.server.world.SimpleItem;
import net.elytrium.limboapi.server.world.SimpleWorld;
import net.elytrium.limboapi.server.world.chunk.SimpleChunk;
import net.elytrium.limboapi.utils.ReloadListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.ComponentSerializer;
import org.bstats.velocity.Metrics;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
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

  private static final Map<Class<?>, Class<?>> PRIMITIVE_WRAPPER_MAP = new HashMap<>();

  @MonotonicNonNull
  private static Logger LOGGER;
  @MonotonicNonNull
  private static Serializer SERIALIZER;

  private final VelocityServer server;
  private final Metrics.Factory metricsFactory;
  private final File configFile;
  private final Set<Player> players;
  private final CachedPackets packets;
  private final PacketFactory packetFactory;
  private final HashMap<Player, LoginTasksQueue> loginQueue;
  private final HashMap<Player, Function<KickedFromServerEvent, Boolean>> kickCallback;
  private final HashMap<Player, RegisteredServer> nextServer;
  private final HashMap<Player, UUID> initialID;

  private PreparedPacketFactory preparedPacketFactory;
  private PreparedPacketFactory loginPreparedPacketFactory;
  private ProtocolVersion minVersion;
  private ProtocolVersion maxVersion;
  private LoginListener loginListener;
  private boolean compressionEnabled;

  @Inject
  public LimboAPI(Logger logger, ProxyServer server, Metrics.Factory metricsFactory, @DataDirectory Path dataDirectory) {
    setLogger(logger);

    this.server = (VelocityServer) server;
    this.metricsFactory = metricsFactory;
    this.configFile = dataDirectory.resolve("config.yml").toFile();
    this.players = new HashSet<>();
    this.packetFactory = new PacketFactoryImpl();
    this.packets = new CachedPackets(this);
    this.loginQueue = new HashMap<>();
    this.kickCallback = new HashMap<>();
    this.nextServer = new HashMap<>();
    this.initialID = new HashMap<>();

    if (ProtocolVersion.MAXIMUM_VERSION.getProtocol() < 759) {
      LOGGER.error("Please update Velocity (https://papermc.io/downloads#Velocity). LimboAPI support: https://ely.su/discord");
      this.server.shutdown();
    } else {
      LOGGER.info("Initializing Simple Virtual Block system...");
      SimpleBlock.init();
      LOGGER.info("Initializing Simple Virtual Item system...");
      SimpleItem.init();
      LOGGER.info("Hooking into EventManager, PlayerList and StateRegistry...");
      try {
        EventManagerHook.init(this);
        PlayerListItemHook.init(this, StateRegistry.PLAY.clientbound);
        LimboProtocol.init();
      } catch (ReflectiveOperationException e) {
        throw new ReflectionException(e);
      }
    }
  }

  @Subscribe
  public void onProxyInitialization(ProxyInitializeEvent event) {
    Settings.IMP.setLogger(LOGGER);

    int level = this.server.getConfiguration().getCompressionLevel();
    int threshold = this.server.getConfiguration().getCompressionThreshold();
    this.preparedPacketFactory =
        new PreparedPacketFactory(PreparedPacketImpl::new, LimboProtocol.getLimboStateRegistry(), this.compressionEnabled, level, threshold);
    this.loginPreparedPacketFactory =
        new PreparedPacketFactory(PreparedPacketImpl::new, StateRegistry.LOGIN, this.compressionEnabled, level, threshold);
    this.reloadPreparedPacketFactory();
    this.reload();

    this.metricsFactory.make(this, 12530);

    if (Settings.IMP.MAIN.CHECK_FOR_UPDATES) {
      if (!UpdatesChecker.checkVersionByURL("https://raw.githubusercontent.com/Elytrium/LimboAPI/master/VERSION", Settings.IMP.VERSION)) {
        LOGGER.error("****************************************");
        LOGGER.warn("The new LimboAPI update was found, please update.");
        LOGGER.error("https://github.com/Elytrium/LimboAPI/releases/");
        LOGGER.error("****************************************");
      }
    }
  }

  @Subscribe(order = PostOrder.LAST)
  public void postProxyInitialization(ProxyInitializeEvent event) throws IllegalAccessException {
    ((EventManagerHook) this.server.getEventManager()).reloadHandlers();
  }

  @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH", justification = "LEGACY_AMPERSAND can't be null in velocity.")
  public void reload() {
    if (Settings.IMP.reload(this.configFile, Settings.IMP.PREFIX) == YamlConfig.LoadResult.CONFIG_NOT_EXISTS) {
      LOGGER.warn("************* FIRST LAUNCH *************");
      LOGGER.warn("Thanks for installing LimboAPI!");
      LOGGER.warn("(C) 2021 - 2022 Elytrium");
      LOGGER.warn("");
      LOGGER.warn("Check out our plugins here: https://ely.su/github <3");
      LOGGER.warn("Discord: https://ely.su/discord");
      LOGGER.warn("****************************************");
    }

    ComponentSerializer<Component, Component, String> serializer = Serializers.valueOf(Settings.IMP.SERIALIZER.toUpperCase(Locale.ROOT)).getSerializer();
    if (serializer == null) {
      LOGGER.warn("The specified serializer could not be founded, using default. (LEGACY_AMPERSAND)");
      setSerializer(new Serializer(Objects.requireNonNull(Serializers.LEGACY_AMPERSAND.getSerializer())));
    } else {
      setSerializer(new Serializer(serializer));
    }

    LOGGER.info("Creating and preparing packets...");
    this.reloadVersion();
    this.packets.createPackets();
    this.loginListener = new LoginListener(this, this.server);
    VelocityEventManager eventManager = this.server.getEventManager();
    eventManager.unregisterListeners(this);
    eventManager.register(this, this.loginListener);
    eventManager.register(this, new KickListener(this));
    eventManager.register(this, new DisconnectListener(this));
    eventManager.register(this, new ReloadListener(this));

    LOGGER.info("Loaded!");
  }

  private void reloadVersion() {
    if (Settings.IMP.MAIN.PREPARE_MAX_VERSION.equals("LATEST")) {
      this.maxVersion = ProtocolVersion.MAXIMUM_VERSION;
    } else {
      this.maxVersion = ProtocolVersion.valueOf("MINECRAFT_" + Settings.IMP.MAIN.PREPARE_MAX_VERSION);
    }

    this.minVersion = ProtocolVersion.valueOf("MINECRAFT_" + Settings.IMP.MAIN.PREPARE_MIN_VERSION);

    if (ProtocolVersion.MAXIMUM_VERSION.compareTo(this.maxVersion) > 0 || ProtocolVersion.MINIMUM_VERSION.compareTo(this.minVersion) < 0) {
      LOGGER.warn(
          "Currently working only with "
              + this.minVersion.getVersionIntroducedIn() + " - " + this.maxVersion.getMostRecentSupportedVersion()
              + " versions, modify the plugins/limboapi/config.yml file if you want the plugin to work with other versions."
      );
    }
  }

  public void reloadPreparedPacketFactory() {
    int level = this.server.getConfiguration().getCompressionLevel();
    int threshold = this.server.getConfiguration().getCompressionThreshold();
    this.compressionEnabled = threshold != -1;

    this.preparedPacketFactory.updateCompressor(this.compressionEnabled, level, threshold);
    this.loginPreparedPacketFactory.updateCompressor(this.compressionEnabled, level, threshold);
  }

  @Override
  public VirtualBlock createSimpleBlock(Block block) {
    return SimpleBlock.fromLegacyID((short) block.getID());
  }

  @Override
  public VirtualBlock createSimpleBlock(short legacyID) {
    return this.createSimpleBlock(legacyID, false);
  }

  @Override
  public VirtualBlock createSimpleBlock(String modernID, Map<String, String> properties) {
    return SimpleBlock.fromModernID(modernID, properties);
  }

  @Override
  public VirtualBlock createSimpleBlock(short legacyID, boolean modern) {
    if (modern) {
      return SimpleBlock.solid(legacyID);
    } else {
      return SimpleBlock.fromLegacyID(legacyID);
    }
  }

  @Override
  public VirtualBlock createSimpleBlock(boolean solid, boolean air, boolean motionBlocking, short id) {
    return new SimpleBlock(solid, air, motionBlocking, id);
  }

  @Override
  public VirtualBlock createSimpleBlock(boolean solid, boolean air, boolean motionBlocking, String modernID, Map<String, String> properties) {
    return new SimpleBlock(solid, air, motionBlocking, modernID, properties);
  }

  @Override
  public VirtualWorld createVirtualWorld(Dimension dimension, double posX, double posY, double posZ, float yaw, float pitch) {
    return new SimpleWorld(dimension, posX, posY, posZ, yaw, pitch);
  }

  @Override
  public VirtualChunk createVirtualChunk(int posX, int posZ) {
    return new SimpleChunk(posX, posZ);
  }

  @Override
  public VirtualChunk createVirtualChunk(int posX, int posZ, VirtualBiome defaultBiome) {
    return new SimpleChunk(posX, posZ, defaultBiome);
  }

  @Override
  public VirtualChunk createVirtualChunk(int posX, int posZ, BuiltInBiome defaultBiome) {
    return new SimpleChunk(posX, posZ, Biome.of(defaultBiome));
  }

  @Override
  public Limbo createLimbo(VirtualWorld world) {
    return new LimboImpl(this, world);
  }

  @Override
  public PreparedPacket createPreparedPacket() {
    return (PreparedPacket) this.preparedPacketFactory.createPreparedPacket(this.minVersion, this.maxVersion);
  }

  @Override
  public PreparedPacket createPreparedPacket(ProtocolVersion minVersion, ProtocolVersion maxVersion) {
    return (PreparedPacket) this.preparedPacketFactory.createPreparedPacket(minVersion, maxVersion);
  }

  public ByteBuf encodeSingle(MinecraftPacket packet, ProtocolVersion version) {
    return this.preparedPacketFactory.encodeSingle(packet, version);
  }

  public ByteBuf encodeSingleLogin(MinecraftPacket packet, ProtocolVersion version) {
    return this.loginPreparedPacketFactory.encodeSingle(packet, version);
  }

  public void inject3rdParty(Player player, MinecraftConnection connection, ChannelPipeline pipeline) {
    this.preparedPacketFactory.inject(player, connection, pipeline);
  }

  public void deject3rdParty(ChannelPipeline pipeline) {
    this.preparedPacketFactory.deject(pipeline);
  }

  public void fixDecompressor(ChannelPipeline pipeline, int threshold, boolean onLogin) {
    ChannelHandler decoder;
    if (onLogin && Settings.IMP.MAIN.DISCARD_COMPRESSION_ON_LOGIN) {
      decoder = new MinecraftDiscardCompressDecoder();
    } else if (!onLogin && Settings.IMP.MAIN.DISCARD_COMPRESSION_AFTER_LOGIN) {
      decoder = new MinecraftDiscardCompressDecoder();
    } else {
      int level = this.server.getConfiguration().getCompressionLevel();
      VelocityCompressor compressor = Natives.compress.get().create(level);
      decoder = new MinecraftLimitedCompressDecoder(threshold, compressor);
    }

    pipeline.addBefore(Connections.MINECRAFT_DECODER, LimboProtocol.DECOMPRESSOR, decoder);
  }

  public void fixCompressor(ChannelPipeline pipeline, ProtocolVersion version) {
    int compressionThreshold = this.server.getConfiguration().getCompressionThreshold();
    if (compressionThreshold == -1 || version.compareTo(ProtocolVersion.MINECRAFT_1_8) < 0) {
      pipeline.addBefore(Connections.MINECRAFT_DECODER, Connections.FRAME_ENCODER, MinecraftVarintLengthEncoder.INSTANCE);
    } else {
      int level = this.server.getConfiguration().getCompressionLevel();
      VelocityCompressor compressor = Natives.compress.get().create(level);
      MinecraftCompressorAndLengthEncoder encoder = new MinecraftCompressorAndLengthEncoder(compressionThreshold, compressor);
      pipeline.addBefore(Connections.MINECRAFT_ENCODER, Connections.COMPRESSION_ENCODER, encoder);

      if (pipeline.names().contains(LimboProtocol.DECOMPRESSOR)) {
        MinecraftCompressDecoder decoder = new MinecraftCompressDecoder(compressionThreshold, compressor);
        pipeline.replace(LimboProtocol.DECOMPRESSOR, Connections.COMPRESSION_DECODER, decoder);
      }
    }
  }

  @Override
  public Object instantiatePacket(BuiltInPackets packetType, Object... data) {
    try {
      for (Constructor<?> constructor : packetType.getPacketClass().getDeclaredConstructors()) {
        if (this.compareData(constructor.getParameterTypes(), data)) {
          return constructor.newInstance(data);
        }
      }

      throw new IllegalArgumentException("No constructor found with the correct arguments!");
    } catch (InstantiationException | InvocationTargetException | IllegalAccessException e) {
      throw new ReflectionException(e);
    }
  }

  @Deprecated(forRemoval = true)
  private boolean compareData(Class<?>[] parameterTypes, Object[] data) {
    if (parameterTypes.length != data.length) {
      return false;
    } else {
      for (int i = 0; i < parameterTypes.length; ++i) {
        if (data[i] != null && !this.primitiveToWrapper(parameterTypes[i]).isAssignableFrom(this.primitiveToWrapper(data[i].getClass()))) {
          return false;
        }
      }

      return true;
    }
  }

  // From apache commons.
  private Class<?> primitiveToWrapper(Class<?> cls) {
    if (cls.isPrimitive()) {
      return PRIMITIVE_WRAPPER_MAP.get(cls);
    } else {
      return cls;
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

  @Override
  public PacketFactory getPacketFactory() {
    return this.packetFactory;
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

  public void setKickCallback(Player player, Function<KickedFromServerEvent, Boolean> queue) {
    this.kickCallback.put(player, queue);
  }

  public void removeKickCallback(Player player) {
    this.kickCallback.remove(player);
  }

  public boolean hasKickCallback(Player player) {
    return this.kickCallback.containsKey(player);
  }

  public Function<KickedFromServerEvent, Boolean> getKickCallback(Player player) {
    return this.kickCallback.get(player);
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

  public boolean isCompressionEnabled() {
    return this.compressionEnabled;
  }

  public ProtocolVersion getPrepareMinVersion() {
    return this.minVersion;
  }

  public ProtocolVersion getPrepareMaxVersion() {
    return this.maxVersion;
  }

  static {
    PRIMITIVE_WRAPPER_MAP.put(Boolean.TYPE, Boolean.class);
    PRIMITIVE_WRAPPER_MAP.put(Byte.TYPE, Byte.class);
    PRIMITIVE_WRAPPER_MAP.put(Character.TYPE, Character.class);
    PRIMITIVE_WRAPPER_MAP.put(Short.TYPE, Short.class);
    PRIMITIVE_WRAPPER_MAP.put(Integer.TYPE, Integer.class);
    PRIMITIVE_WRAPPER_MAP.put(Long.TYPE, Long.class);
    PRIMITIVE_WRAPPER_MAP.put(Double.TYPE, Double.class);
    PRIMITIVE_WRAPPER_MAP.put(Float.TYPE, Float.class);
    PRIMITIVE_WRAPPER_MAP.put(Void.TYPE, Void.TYPE);
  }

  private static void setLogger(Logger logger) {
    LOGGER = logger;
  }

  public static Logger getLogger() {
    return LOGGER;
  }

  private static void setSerializer(Serializer serializer) {
    SERIALIZER = serializer;
  }

  public static Serializer getSerializer() {
    return SERIALIZER;
  }
}
