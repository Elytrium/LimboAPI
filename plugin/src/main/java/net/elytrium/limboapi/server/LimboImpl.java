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

package net.elytrium.limboapi.server;

import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_16;

import com.google.common.collect.ImmutableSet;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.proxy.connection.ConnectionTypes;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.connection.registry.DimensionData;
import com.velocitypowered.proxy.connection.registry.DimensionInfo;
import com.velocitypowered.proxy.connection.registry.DimensionRegistry;
import com.velocitypowered.proxy.network.Connections;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.packet.JoinGame;
import com.velocitypowered.proxy.protocol.packet.Respawn;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.netty.channel.ChannelPipeline;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import net.elytrium.limboapi.LimboAPI;
import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.LimboSessionHandler;
import net.elytrium.limboapi.api.chunk.Dimension;
import net.elytrium.limboapi.api.chunk.VirtualChunk;
import net.elytrium.limboapi.api.chunk.VirtualWorld;
import net.elytrium.limboapi.config.Settings;
import net.elytrium.limboapi.injection.PreparedPacketEncoder;
import net.elytrium.limboapi.injection.packet.PreparedPacket;
import net.elytrium.limboapi.material.Biome;
import net.elytrium.limboapi.protocol.LimboProtocol;
import net.elytrium.limboapi.protocol.packet.PlayerPositionAndLook;
import net.elytrium.limboapi.protocol.packet.UpdateViewPosition;
import net.elytrium.limboapi.protocol.packet.world.ChunkData;

@Getter
@Setter
@SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
public class LimboImpl implements Limbo {
  private static Field partialHashedSeed;
  private static Field currentDimensionData;

  private final LimboAPI limboAPI;
  private VirtualWorld world;

  private PreparedPacket joinPackets;
  private PreparedPacket fastRejoinPackets;
  private PreparedPacket safeRejoinPackets;
  private PreparedPacket chunks;
  private PreparedPacket spawnPosition;

  static {
    try {
      partialHashedSeed = JoinGame.class.getDeclaredField("partialHashedSeed");
      partialHashedSeed.setAccessible(true);

      currentDimensionData = JoinGame.class.getDeclaredField("currentDimensionData");
      currentDimensionData.setAccessible(true);
    } catch (NoSuchFieldException e) {
      e.printStackTrace();
    }
  }

  public LimboImpl(LimboAPI limboAPI, VirtualWorld world) {
    this.limboAPI = limboAPI;
    this.world = world;

    refresh();
  }

  public void refresh() {
    JoinGame legacyJoinGame = createLegacyJoinGamePacket();
    JoinGame joinGame = createJoinGamePacket();

    joinPackets = new PreparedPacket()
        .prepare(legacyJoinGame, ProtocolVersion.MINIMUM_VERSION, ProtocolVersion.MINECRAFT_1_15_2)
        .prepare(joinGame, ProtocolVersion.MINECRAFT_1_16);

    fastRejoinPackets = new PreparedPacket();
    createFastClientServerSwitch(legacyJoinGame, ProtocolVersion.MINECRAFT_1_7_2)
        .forEach(minecraftPacket ->
            fastRejoinPackets.prepare(
                minecraftPacket, ProtocolVersion.MINIMUM_VERSION, ProtocolVersion.MINECRAFT_1_15_2));
    createFastClientServerSwitch(joinGame, ProtocolVersion.MINECRAFT_1_16)
        .forEach(minecraftPacket -> fastRejoinPackets.prepare(minecraftPacket, ProtocolVersion.MINECRAFT_1_16));

    safeRejoinPackets = new PreparedPacket()
        .prepare(createSafeClientServerSwitch(legacyJoinGame));

    chunks = new PreparedPacket().prepare(createChunksPackets());
    spawnPosition = new PreparedPacket()
        .prepare(createPlayerPosAndLookPacket(
            world.getSpawnX(), world.getSpawnY(), world.getSpawnZ(), getWorld().getYaw(), getWorld().getPitch()))
        .prepare(createUpdateViewPosition((int) world.getSpawnX(), (int) world.getSpawnZ()),
            ProtocolVersion.MINECRAFT_1_14);
  }

  public void spawnPlayer(Player apiPlayer, LimboSessionHandler handler) {
    ConnectedPlayer player = (ConnectedPlayer) apiPlayer;
    MinecraftConnection connection = player.getConnection();

    connection.eventLoop().execute(() -> {
      ChannelPipeline pipeline = connection.getChannel().pipeline();
      pipeline.names().forEach(System.out::println);

      if (Settings.IMP.MAIN.LOGGING_ENABLED) {
        limboAPI.getLogger().info(
            player.getUsername() + " (" + player.getRemoteAddress()
                + ") has connected to VirtualServer " + handler.getClass().getSimpleName());
      }

      if (!pipeline.names().contains("prepared-encoder")) {
        pipeline.addAfter(Connections.MINECRAFT_ENCODER,
            "prepared-encoder", new PreparedPacketEncoder(connection.getProtocolVersion()));
      }

      limboAPI.setVirtualServerJoined(player);

      if (connection.getState() != LimboProtocol.getLimboRegistry()) {
        connection.setState(LimboProtocol.getLimboRegistry());
        if (player.getConnectedServer() != null) {
          player.getConnectedServer().disconnect();
        }
      }

      if (limboAPI.isVirtualServerJoined(player)) {
        if (connection.getType() == ConnectionTypes.LEGACY_FORGE) {
          connection.delayedWrite(getSafeRejoinPackets());
        } else {
          connection.delayedWrite(getFastRejoinPackets());
        }
      } else {
        connection.delayedWrite(getJoinPackets());
      }

      LimboSessionHandlerImpl limboSessionHandlerImpl =
          new LimboSessionHandlerImpl(limboAPI, player, handler, connection.getSessionHandler());

      connection.setSessionHandler(limboSessionHandlerImpl);

      connection.flush();
      respawnPlayer(player);
      limboSessionHandlerImpl.onSpawn(this, new LimboPlayerImpl(player, this));
    });
  }

  public void respawnPlayer(Player player) {
    MinecraftConnection connection = ((ConnectedPlayer) player).getConnection();

    connection.write(getChunks());
    connection.write(getSpawnPosition());
  }

  private DimensionData createDimensionData(Dimension dimension) {
    return new DimensionData(dimension.getKey(), dimension.getModernId(), true,
        0.0f, false, false, false, true,
        false, false, false, false, 256,
        "minecraft:infiniburn_nether",
        0L, false, 1.0, dimension.getKey(), 0, 256);
  }

  @SneakyThrows
  private JoinGame createJoinGamePacket() {
    Dimension dimension = world.getDimension();

    JoinGame joinGame = new JoinGame();
    joinGame.setEntityId(0);
    joinGame.setGamemode((short) 2);
    joinGame.setPreviousGamemode((short) 2);
    joinGame.setDimension(dimension.getModernId());
    joinGame.setDifficulty((short) 0);
    joinGame.setMaxPlayers(1);
    partialHashedSeed.set(joinGame, System.currentTimeMillis());
    joinGame.setLevelType("flat");
    joinGame.setViewDistance(12);
    joinGame.setReducedDebugInfo(true);
    joinGame.setIsHardcore(true);

    String key = dimension.getKey();
    DimensionData dimensionData = createDimensionData(dimension);

    joinGame.setDimensionRegistry(new DimensionRegistry(ImmutableSet.of(dimensionData), ImmutableSet.of(key)));
    joinGame.setDimensionInfo(new DimensionInfo(key, key, false, false));
    currentDimensionData.set(joinGame, dimensionData);
    joinGame.setBiomeRegistry(Biome.getRegistry());

    return joinGame;
  }

  private JoinGame createLegacyJoinGamePacket() {
    JoinGame joinGame = createJoinGamePacket();
    joinGame.setDimension(world.getDimension().getLegacyId());
    return joinGame;
  }

  private List<ChunkData> createChunksPackets() {
    List<ChunkData> packets = new ArrayList<>();
    for (VirtualChunk chunk : world.getChunks()) {
      packets.add(createChunkDataPacket(chunk, (int) world.getSpawnY()));
    }
    return packets;
  }

  // Ported from Velocity
  public static List<MinecraftPacket> createFastClientServerSwitch(JoinGame joinGame, ProtocolVersion version) {
    // In order to handle switching to another server, you will need to send two packets:
    //
    // - The join game packet from the backend server, with a different dimension
    // - A respawn with the correct dimension
    //
    // Most notably, by having the client accept the join game packet, we can work around the need
    // to perform entity ID rewrites, eliminating potential issues from rewriting packets and
    // improving compatibility with mods.
    List<MinecraftPacket> packets = new ArrayList<>();
    int sentOldDim = joinGame.getDimension();
    if (version.compareTo(MINECRAFT_1_16) < 0) {
      // Before Minecraft 1.16, we could not switch to the same dimension without sending an
      // additional respawn. On older versions of Minecraft this forces the client to perform
      // garbage collection which adds additional latency.
      joinGame.setDimension(joinGame.getDimension() == 0 ? -1 : 0);
    }
    packets.add(joinGame);

    packets.add(
        new Respawn(sentOldDim, joinGame.getPartialHashedSeed(),
            joinGame.getDifficulty(), joinGame.getGamemode(), joinGame.getLevelType(),
            false, joinGame.getDimensionInfo(), joinGame.getPreviousGamemode(),
            joinGame.getCurrentDimensionData()));

    return packets;
  }

  public static List<MinecraftPacket> createSafeClientServerSwitch(JoinGame joinGame) {
    // Some clients do not behave well with the "fast" respawn sequence. In this case we will use
    // a "safe" respawn sequence that involves sending three packets to the client. They have the
    // same effect but tend to work better with buggier clients (Forge 1.8 in particular).
    List<MinecraftPacket> packets = new ArrayList<>();

    // Send the JoinGame packet itself, unmodified.
    packets.add(joinGame);

    // Send a respawn packet in a different dimension.
    int tempDim = joinGame.getDimension() == 0 ? -1 : 0;
    packets.add(
        new Respawn(tempDim, joinGame.getPartialHashedSeed(), joinGame.getDifficulty(),
            joinGame.getGamemode(), joinGame.getLevelType(),
            false, joinGame.getDimensionInfo(), joinGame.getPreviousGamemode(),
            joinGame.getCurrentDimensionData()));

    // Now send a respawn packet in the correct dimension.
    packets.add(
        new Respawn(joinGame.getDimension(), joinGame.getPartialHashedSeed(),
            joinGame.getDifficulty(), joinGame.getGamemode(), joinGame.getLevelType(),
            false, joinGame.getDimensionInfo(), joinGame.getPreviousGamemode(),
            joinGame.getCurrentDimensionData()));

    return packets;
  }

  private ChunkData createChunkDataPacket(VirtualChunk chunk, int skyLightY) {
    chunk.setSkyLight(chunk.getX() % 16, skyLightY, chunk.getZ() % 16, (byte) 1);
    return new ChunkData(chunk.getFullChunkSnapshot(), true);
  }

  public PlayerPositionAndLook createPlayerPosAndLookPacket(double x, double y, double z, float yaw, float pitch) {
    return new PlayerPositionAndLook(x, y, z, yaw, pitch, -133, false, true);
  }

  public UpdateViewPosition createUpdateViewPosition(int x, int z) {
    return new UpdateViewPosition(x >> 4, z >> 4);
  }
}
