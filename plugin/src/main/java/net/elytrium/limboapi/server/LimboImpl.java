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

package net.elytrium.limboapi.server;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Runnables;
import com.mojang.brigadier.tree.RootCommandNode;
import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.proxy.command.registrar.BrigadierCommandRegistrar;
import com.velocitypowered.proxy.command.registrar.CommandRegistrar;
import com.velocitypowered.proxy.command.registrar.RawCommandRegistrar;
import com.velocitypowered.proxy.command.registrar.SimpleCommandRegistrar;
import com.velocitypowered.proxy.connection.ConnectionTypes;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.connection.registry.DimensionData;
import com.velocitypowered.proxy.connection.registry.DimensionInfo;
import com.velocitypowered.proxy.connection.registry.DimensionRegistry;
import com.velocitypowered.proxy.network.Connections;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.packet.AvailableCommands;
import com.velocitypowered.proxy.protocol.packet.JoinGame;
import com.velocitypowered.proxy.protocol.packet.PlayerListItem;
import com.velocitypowered.proxy.protocol.packet.PluginMessage;
import com.velocitypowered.proxy.protocol.packet.Respawn;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.timeout.ReadTimeoutHandler;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import net.elytrium.java.commons.reflection.ReflectionException;
import net.elytrium.limboapi.LimboAPI;
import net.elytrium.limboapi.Settings;
import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.LimboSessionHandler;
import net.elytrium.limboapi.api.chunk.Dimension;
import net.elytrium.limboapi.api.chunk.VirtualChunk;
import net.elytrium.limboapi.api.chunk.VirtualWorld;
import net.elytrium.limboapi.api.command.LimboCommandMeta;
import net.elytrium.limboapi.api.player.GameMode;
import net.elytrium.limboapi.api.protocol.PreparedPacket;
import net.elytrium.limboapi.injection.packet.PreparedPacketEncoder;
import net.elytrium.limboapi.material.Biome;
import net.elytrium.limboapi.protocol.LimboProtocol;
import net.elytrium.limboapi.protocol.packet.DefaultSpawnPosition;
import net.elytrium.limboapi.protocol.packet.PlayerPositionAndLook;
import net.elytrium.limboapi.protocol.packet.UpdateViewPosition;
import net.elytrium.limboapi.protocol.packet.world.ChunkData;

public class LimboImpl implements Limbo {

  private static final Field partialHashedSeed;
  private static final Field currentDimensionData;
  private static final Field rootNode;

  private final LimboAPI plugin;
  private final VirtualWorld world;
  private final Map<Class<? extends LimboSessionHandler>, PreparedPacket> brandMessages = new HashMap<>();

  private final RootCommandNode<CommandSource> commandNode = new RootCommandNode<>();
  private final ReadWriteLock lock = new ReentrantReadWriteLock();

  private final List<CommandRegistrar<?>> registrars = ImmutableList.of(
      new BrigadierCommandRegistrar(this.commandNode, this.lock.writeLock()),
      new SimpleCommandRegistrar(this.commandNode, this.lock.writeLock()),
      new RawCommandRegistrar(this.commandNode, this.lock.writeLock())
  );

  private String limboName;

  private Integer readTimeout;

  private PreparedPacket joinPackets;
  private PreparedPacket fastRejoinPackets;
  private PreparedPacket safeRejoinPackets;
  private PreparedPacket postJoinPackets;
  private PreparedPacket chunks;
  private PreparedPacket spawnPosition;

  static {
    try {
      partialHashedSeed = JoinGame.class.getDeclaredField("partialHashedSeed");
      partialHashedSeed.setAccessible(true);

      currentDimensionData = JoinGame.class.getDeclaredField("currentDimensionData");
      currentDimensionData.setAccessible(true);

      rootNode = AvailableCommands.class.getDeclaredField("rootNode");
      rootNode.setAccessible(true);
    } catch (NoSuchFieldException e) {
      throw new ReflectionException(e);
    }
  }

  public LimboImpl(LimboAPI plugin, VirtualWorld world) {
    this.plugin = plugin;
    this.world = world;

    this.refresh();
  }

  protected void refresh() {
    // TODO: Fix 1.16+ nether dimension
    JoinGame legacyJoinGame = this.createLegacyJoinGamePacket();
    JoinGame joinGame = this.createJoinGamePacket(false);
    JoinGame joinGameModern = this.createJoinGamePacket(true);

    this.joinPackets = this.plugin.createPreparedPacket()
        .prepare(legacyJoinGame, ProtocolVersion.MINIMUM_VERSION, ProtocolVersion.MINECRAFT_1_15_2)
        .prepare(joinGame, ProtocolVersion.MINECRAFT_1_16, ProtocolVersion.MINECRAFT_1_18)
        .prepare(joinGameModern, ProtocolVersion.MINECRAFT_1_18_2);

    this.fastRejoinPackets = this.plugin.createPreparedPacket();
    this.createFastClientServerSwitch(legacyJoinGame, ProtocolVersion.MINECRAFT_1_7_2)
        .forEach(minecraftPacket -> this.fastRejoinPackets.prepare(minecraftPacket, ProtocolVersion.MINIMUM_VERSION, ProtocolVersion.MINECRAFT_1_15_2));
    this.createFastClientServerSwitch(joinGame, ProtocolVersion.MINECRAFT_1_16)
        .forEach(minecraftPacket -> this.fastRejoinPackets.prepare(minecraftPacket, ProtocolVersion.MINECRAFT_1_16, ProtocolVersion.MINECRAFT_1_18));
    this.createFastClientServerSwitch(joinGameModern, ProtocolVersion.MINECRAFT_1_18_2)
        .forEach(minecraftPacket -> this.fastRejoinPackets.prepare(minecraftPacket, ProtocolVersion.MINECRAFT_1_18_2));

    this.postJoinPackets = this.plugin.createPreparedPacket()
        .prepare(this.createAvailableCommandsPacket(), ProtocolVersion.MINECRAFT_1_13)
        .prepare(this.createDefaultSpawnPositionPacket());

    this.safeRejoinPackets = this.plugin.createPreparedPacket().prepare(this.createSafeClientServerSwitch(legacyJoinGame));

    List<ChunkData> chunkPackets = this.createChunksPackets();
    this.chunks = chunkPackets.size() == 0 ? null : this.plugin.createPreparedPacket().prepare(chunkPackets);

    this.spawnPosition = this.plugin.createPreparedPacket()
        .prepare(
            this.createPlayerPosAndLook(
                this.world.getSpawnX(), this.world.getSpawnY(), this.world.getSpawnZ(), this.world.getYaw(), this.world.getPitch()
            )
        ).prepare(
            this.createUpdateViewPosition((int) this.world.getSpawnX(), (int) this.world.getSpawnZ()),
            ProtocolVersion.MINECRAFT_1_14
        );
  }

  @Override
  public void spawnPlayer(Player apiPlayer, LimboSessionHandler handler) {
    ConnectedPlayer player = (ConnectedPlayer) apiPlayer;
    MinecraftConnection connection = player.getConnection();
    Class<? extends LimboSessionHandler> handlerClass = handler.getClass();
    if (this.limboName == null) {
      this.limboName = handlerClass.getSimpleName();
    }

    connection.eventLoop().execute(() -> {
      ChannelPipeline pipeline = connection.getChannel().pipeline();

      if (Settings.IMP.MAIN.LOGGING_ENABLED) {
        LimboAPI.getLogger().info(player.getUsername() + " (" + player.getRemoteAddress() + ") has connected to the " + this.limboName + " Limbo");
      }

      if (!pipeline.names().contains(LimboProtocol.PREPARED_ENCODER)) {
        // With an abnormally large number of connections from the same nickname,
        // requests don't have time to be processed, and an error occurs that "minecraft-encoder" doesn't exist.
        if (!pipeline.names().contains(Connections.MINECRAFT_ENCODER)) {
          connection.close();
          return;
        }

        if (this.readTimeout != null) {
          pipeline.replace(Connections.READ_TIMEOUT, LimboProtocol.READ_TIMEOUT, new ReadTimeoutHandler(this.readTimeout, TimeUnit.MILLISECONDS));
        }

        pipeline.addAfter(Connections.MINECRAFT_ENCODER, LimboProtocol.PREPARED_ENCODER, new PreparedPacketEncoder(connection.getProtocolVersion()));
      }

      RegisteredServer previousServer = null;
      if (connection.getState() != LimboProtocol.getLimboRegistry()) {
        connection.setState(LimboProtocol.getLimboRegistry());
        VelocityServerConnection server = player.getConnectedServer();

        if (server != null) {
          server.disconnect();
          player.setConnectedServer(null);
          previousServer = server.getServer();
          this.plugin.setLimboJoined(player);
        }
      }

      if (this.plugin.isLimboJoined(player)) {
        if (connection.getType() == ConnectionTypes.LEGACY_FORGE) {
          connection.delayedWrite(this.safeRejoinPackets);
        } else {
          connection.delayedWrite(this.fastRejoinPackets);
        }
      } else {
        connection.delayedWrite(this.joinPackets);
      }

      connection.delayedWrite(
          new PlayerListItem(
              PlayerListItem.ADD_PLAYER,
              List.of(
                  new PlayerListItem.Item(player.getUniqueId())
                      .setName(player.getUsername())
                      .setGameMode(GameMode.ADVENTURE.getId())
                      .setProperties(player.getGameProfileProperties())
              )
          )
      );
      connection.delayedWrite(this.postJoinPackets);
      connection.delayedWrite(this.getBrandMessage(handlerClass));

      this.plugin.setLimboJoined(player);

      LimboSessionHandlerImpl sessionHandler = new LimboSessionHandlerImpl(
          this.plugin,
          player,
          handler,
          connection.getSessionHandler(),
          previousServer,
          () -> this.limboName
      );
      connection.setSessionHandler(sessionHandler);

      connection.flush();

      this.respawnPlayer(player);
      sessionHandler.onSpawn(this, new LimboPlayerImpl(this.plugin, this, player));
    });
  }

  @Override
  public void respawnPlayer(Player player) {
    MinecraftConnection connection = ((ConnectedPlayer) player).getConnection();

    connection.delayedWrite(this.spawnPosition);
    if (this.chunks != null) {
      connection.delayedWrite(this.chunks);
    }

    connection.flush();
  }

  @Override
  public Limbo setName(String name) {
    this.limboName = name;
    if (!this.brandMessages.isEmpty()) {
      this.brandMessages.forEach((handlerClass, packet) ->
          this.brandMessages.replace(handlerClass, packet, this.plugin.createPreparedPacket().prepare(this::createBrandMessage))
      );
    }

    return this;
  }

  @Override
  public Limbo setReadTimeout(int millis) {
    this.readTimeout = millis;

    return this;
  }

  @Override
  @SuppressWarnings("UnstableApiUsage")
  public Limbo registerCommand(LimboCommandMeta commandMeta) {
    return this.registerCommand(commandMeta, (SimpleCommand) Runnables.doNothing());
  }

  @Override
  public Limbo registerCommand(CommandMeta commandMeta, Command command) {
    for (CommandRegistrar<?> registrar : this.registrars) {
      if (this.tryRegister(registrar, commandMeta, command)) {
        this.refresh();
        return this;
      }
    }

    throw new IllegalArgumentException(command + " does not implement a registrable Command sub-interface.");
  }

  // From Velocity.
  private <T extends Command> boolean tryRegister(CommandRegistrar<T> registrar, CommandMeta commandMeta, Command command) {
    Class<T> superInterface = registrar.registrableSuperInterface();
    if (!superInterface.isInstance(command)) {
      return false;
    }

    registrar.register(commandMeta, superInterface.cast(command));
    return true;
  }

  private DimensionData createDimensionData(Dimension dimension, boolean modern) {
    return new DimensionData(
        dimension.getKey(), dimension.getModernId(), true,
        0.0F, false, false, false, true,
        false, false, false, false, 256,
        modern ? "#minecraft:infiniburn_nether" : "minecraft:infiniburn_nether",
        0L, false, 1.0, dimension.getKey(), 0, 256
    );
  }

  private JoinGame createJoinGamePacket(boolean modern) {
    Dimension dimension = this.world.getDimension();

    JoinGame joinGame = new JoinGame();
    joinGame.setEntityId(0);
    short gamemode = (short) GameMode.ADVENTURE.getId();
    joinGame.setGamemode(gamemode);
    joinGame.setPreviousGamemode(gamemode);
    joinGame.setDimension(dimension.getModernId());
    joinGame.setDifficulty((short) 0);
    joinGame.setMaxPlayers(1);

    try {
      partialHashedSeed.set(joinGame, System.currentTimeMillis());
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    }

    joinGame.setLevelType("flat");
    joinGame.setViewDistance(10);
    joinGame.setSimulationDistance(0);
    joinGame.setReducedDebugInfo(true);
    joinGame.setIsHardcore(true);

    String key = dimension.getKey();
    DimensionData dimensionData = this.createDimensionData(dimension, modern);

    joinGame.setDimensionRegistry(new DimensionRegistry(ImmutableSet.of(dimensionData), ImmutableSet.of(key)));
    joinGame.setDimensionInfo(new DimensionInfo(key, key, false, false));

    try {
      currentDimensionData.set(joinGame, dimensionData);
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    }

    joinGame.setBiomeRegistry(Biome.getRegistry());

    return joinGame;
  }

  private JoinGame createLegacyJoinGamePacket() {
    JoinGame joinGame = this.createJoinGamePacket(false);
    joinGame.setDimension(this.world.getDimension().getLegacyId());

    return joinGame;
  }

  private DefaultSpawnPosition createDefaultSpawnPositionPacket() {
    return new DefaultSpawnPosition((int) this.world.getSpawnX(), (int) this.world.getSpawnY(), (int) this.world.getSpawnZ(), 0.0F);
  }

  private AvailableCommands createAvailableCommandsPacket() {
    try {
      AvailableCommands packet = new AvailableCommands();
      rootNode.set(packet, this.commandNode);

      return packet;
    } catch (IllegalAccessException e) {
      e.printStackTrace();
      return null;
    }
  }

  private List<ChunkData> createChunksPackets() {
    List<ChunkData> packets = new ArrayList<>();
    for (VirtualChunk chunk : this.world.getChunks()) {
      packets.add(this.createChunkData(chunk, this.world.getDimension(), (int) this.world.getSpawnY()));
    }

    return packets;
  }

  // From Velocity.
  private List<MinecraftPacket> createFastClientServerSwitch(JoinGame joinGame, ProtocolVersion version) {
    // In order to handle switching to another server, you will need to send two packets:
    //
    // - The join game packet from the backend server, with a different dimension.
    // - A respawn with the correct dimension.
    //
    // Most notably, by having the client accept the join game packet, we can work around the need
    // to perform entity ID rewrites, eliminating potential issues from rewriting packets and
    // improving compatibility with mods.
    List<MinecraftPacket> packets = new ArrayList<>();
    int sentOldDim = joinGame.getDimension();
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_16) < 0) {
      // Before Minecraft 1.16, we could not switch to the same dimension without sending an
      // additional respawn. On older versions of Minecraft this forces the client to perform
      // garbage collection which adds additional latency.
      joinGame.setDimension(sentOldDim == 0 ? -1 : 0);
    }

    packets.add(joinGame);

    packets.add(
        new Respawn(
            sentOldDim, joinGame.getPartialHashedSeed(),
            joinGame.getDifficulty(), joinGame.getGamemode(), joinGame.getLevelType(),
            false, joinGame.getDimensionInfo(), joinGame.getPreviousGamemode(),
            joinGame.getCurrentDimensionData()
        )
    );

    return packets;
  }

  private List<MinecraftPacket> createSafeClientServerSwitch(JoinGame joinGame) {
    // Some clients do not behave well with the "fast" respawn sequence. In this case we will use
    // a "safe" respawn sequence that involves sending three packets to the client. They have the
    // same effect but tend to work better with buggier clients (Forge 1.8 in particular).
    List<MinecraftPacket> packets = new ArrayList<>();

    // Send the JoinGame packet itself, unmodified.
    packets.add(joinGame);

    // Send a respawn packet in a different dimension.
    int tempDim = joinGame.getDimension() == 0 ? -1 : 0;
    packets.add(
        new Respawn(
            tempDim, joinGame.getPartialHashedSeed(), joinGame.getDifficulty(),
            joinGame.getGamemode(), joinGame.getLevelType(),
            false, joinGame.getDimensionInfo(), joinGame.getPreviousGamemode(),
            joinGame.getCurrentDimensionData()
        )
    );

    // Now send a respawn packet in the correct dimension.
    packets.add(
        new Respawn(
            joinGame.getDimension(), joinGame.getPartialHashedSeed(),
            joinGame.getDifficulty(), joinGame.getGamemode(), joinGame.getLevelType(),
            false, joinGame.getDimensionInfo(), joinGame.getPreviousGamemode(),
            joinGame.getCurrentDimensionData()
        )
    );

    return packets;
  }

  private PreparedPacket getBrandMessage(Class<? extends LimboSessionHandler> handlerClass) {
    if (this.brandMessages.containsKey(handlerClass)) {
      return this.brandMessages.get(handlerClass);
    } else {
      PreparedPacket preparedPacket = this.plugin.createPreparedPacket().prepare(this::createBrandMessage);
      this.brandMessages.put(handlerClass, preparedPacket);
      return preparedPacket;
    }
  }

  private PluginMessage createBrandMessage(ProtocolVersion version) {
    String brand = "LimboAPI -> (" + this.limboName + ")";
    ByteBuf bufWithBrandString = Unpooled.buffer();
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_8) < 0) {
      bufWithBrandString.writeCharSequence(brand, StandardCharsets.UTF_8);
    } else {
      ProtocolUtils.writeString(bufWithBrandString, brand);
    }

    return new PluginMessage("MC|Brand", bufWithBrandString);
  }

  private PlayerPositionAndLook createPlayerPosAndLook(double x, double y, double z, float yaw, float pitch) {
    return new PlayerPositionAndLook(x, y, z, yaw, pitch, 44, false, true);
  }

  private UpdateViewPosition createUpdateViewPosition(int x, int z) {
    return new UpdateViewPosition(x >> 4, z >> 4);
  }

  private ChunkData createChunkData(VirtualChunk chunk, Dimension dimension, int skyLightY) {
    chunk.setSkyLight(chunk.getX() & 15, skyLightY, chunk.getZ() & 15, (byte) 1);
    return new ChunkData(chunk.getFullChunkSnapshot(), true, dimension.getMaxSections());
  }

  public Integer getReadTimeout() {
    return this.readTimeout;
  }
}
