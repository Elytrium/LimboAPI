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
import io.netty.channel.ChannelPipeline;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.elytrium.limboapi.LimboAPI;
import net.elytrium.limboapi.Settings;
import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.LimboSessionHandler;
import net.elytrium.limboapi.api.chunk.Dimension;
import net.elytrium.limboapi.api.chunk.VirtualChunk;
import net.elytrium.limboapi.api.chunk.VirtualWorld;
import net.elytrium.limboapi.api.protocol.PreparedPacket;
import net.elytrium.limboapi.injection.packet.PreparedPacketEncoder;
import net.elytrium.limboapi.material.Biome;
import net.elytrium.limboapi.protocol.LimboProtocol;
import net.elytrium.limboapi.protocol.packet.PlayerPositionAndLook;
import net.elytrium.limboapi.protocol.packet.UpdateViewPosition;
import net.elytrium.limboapi.protocol.packet.world.ChunkData;

public class LimboImpl implements Limbo {

  private static Field partialHashedSeed;
  private static Field currentDimensionData;

  private final LimboAPI limboAPI;
  private final VirtualWorld world;

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

    this.refresh();
  }

  public void refresh() {
    JoinGame legacyJoinGame = this.createLegacyJoinGamePacket();
    JoinGame joinGame = this.createJoinGamePacket();

    this.joinPackets = LimboAPI.getInstance().createPreparedPacket()
        .prepare(legacyJoinGame, ProtocolVersion.MINIMUM_VERSION, ProtocolVersion.MINECRAFT_1_15_2)
        .prepare(joinGame, ProtocolVersion.MINECRAFT_1_16);

    this.fastRejoinPackets = LimboAPI.getInstance().createPreparedPacket();
    this.createFastClientServerSwitch(legacyJoinGame, ProtocolVersion.MINECRAFT_1_7_2)
        .forEach(minecraftPacket -> this.fastRejoinPackets.prepare(minecraftPacket, ProtocolVersion.MINIMUM_VERSION, ProtocolVersion.MINECRAFT_1_15_2));
    this.createFastClientServerSwitch(joinGame, ProtocolVersion.MINECRAFT_1_16)
        .forEach(minecraftPacket -> this.fastRejoinPackets.prepare(minecraftPacket, ProtocolVersion.MINECRAFT_1_16));

    this.safeRejoinPackets = LimboAPI.getInstance().createPreparedPacket().prepare(this.createSafeClientServerSwitch(legacyJoinGame));

    this.chunks = LimboAPI.getInstance().createPreparedPacket().prepare(this.createChunksPackets());
    this.spawnPosition = LimboAPI.getInstance().createPreparedPacket()
        .prepare(
            this.createPlayerPosAndLookPacket(
                this.world.getSpawnX(), this.world.getSpawnY(), this.world.getSpawnZ(), this.world.getYaw(), this.world.getPitch()
            )
        ).prepare(
            this.createUpdateViewPosition((int) this.world.getSpawnX(), (int) this.world.getSpawnZ()), ProtocolVersion.MINECRAFT_1_14
        );
  }

  @Override
  public void spawnPlayer(Player apiPlayer, LimboSessionHandler handler) {
    ConnectedPlayer player = (ConnectedPlayer) apiPlayer;
    MinecraftConnection connection = player.getConnection();

    connection.eventLoop().execute(() -> {
      ChannelPipeline pipeline = connection.getChannel().pipeline();

      if (Settings.IMP.MAIN.LOGGING_ENABLED) {
        this.limboAPI.getLogger().info(
            player.getUsername() + " (" + player.getRemoteAddress() + ") has connected to the " + handler.getClass().getSimpleName() + " Limbo"
        );
      }

      if (!pipeline.names().contains("prepared-encoder")) {
        // With an abnormally large number of connections from the same nickname,
        // requests don't have time to be processed,
        // and an error occurs that "minecraft-encoder" doesn't exist.
        if (!pipeline.names().contains(Connections.MINECRAFT_ENCODER)) {
          connection.close();
          return;
        }

        pipeline.addAfter(Connections.MINECRAFT_ENCODER, "prepared-encoder", new PreparedPacketEncoder(connection.getProtocolVersion()));
      }

      if (connection.getState() != LimboProtocol.getLimboRegistry()) {
        connection.setState(LimboProtocol.getLimboRegistry());
        if (player.getConnectedServer() != null) {
          player.getConnectedServer().disconnect();
        }
      }

      if (this.limboAPI.isLimboJoined(player)) {
        if (connection.getType() == ConnectionTypes.LEGACY_FORGE) {
          connection.delayedWrite(this.safeRejoinPackets);
        } else {
          connection.delayedWrite(this.fastRejoinPackets);
        }
      } else {
        connection.delayedWrite(this.joinPackets);
      }

      this.limboAPI.setLimboJoined(player);

      LimboSessionHandlerImpl limboSessionHandlerImpl = new LimboSessionHandlerImpl(this.limboAPI, player, handler, connection.getSessionHandler());

      connection.setSessionHandler(limboSessionHandlerImpl);

      connection.flush();
      this.respawnPlayer(player);
      limboSessionHandlerImpl.onSpawn(this, new LimboPlayerImpl(player, this));
    });
  }

  @Override
  public void respawnPlayer(Player player) {
    MinecraftConnection connection = ((ConnectedPlayer) player).getConnection();

    connection.write(this.spawnPosition);
    connection.write(this.chunks);
  }

  private DimensionData createDimensionData(Dimension dimension) {
    return new DimensionData(dimension.getKey(), dimension.getModernId(), true,
        0.0f, false, false, false, true,
        false, false, false, false, 256,
        "minecraft:infiniburn_nether",
        0L, false, 1.0, dimension.getKey(), 0, 256
    );
  }

  private JoinGame createJoinGamePacket() {
    Dimension dimension = this.world.getDimension();

    JoinGame joinGame = new JoinGame();
    joinGame.setEntityId(0);
    joinGame.setGamemode((short) 2);
    joinGame.setPreviousGamemode((short) 2);
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
    joinGame.setReducedDebugInfo(true);
    joinGame.setIsHardcore(true);

    String key = dimension.getKey();
    DimensionData dimensionData = this.createDimensionData(dimension);

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
    JoinGame joinGame = this.createJoinGamePacket();
    joinGame.setDimension(this.world.getDimension().getLegacyId());

    return joinGame;
  }

  private List<ChunkData> createChunksPackets() {
    List<ChunkData> packets = new ArrayList<>();
    for (VirtualChunk chunk : this.world.getChunks()) {
      packets.add(this.createChunkDataPacket(chunk, (int) this.world.getSpawnY()));
    }

    return packets;
  }

  // Velocity backport
  private List<MinecraftPacket> createFastClientServerSwitch(JoinGame joinGame, ProtocolVersion version) {
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
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_16) < 0) {
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
            joinGame.getCurrentDimensionData())
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
        new Respawn(tempDim, joinGame.getPartialHashedSeed(), joinGame.getDifficulty(),
            joinGame.getGamemode(), joinGame.getLevelType(),
            false, joinGame.getDimensionInfo(), joinGame.getPreviousGamemode(),
            joinGame.getCurrentDimensionData())
    );

    // Now send a respawn packet in the correct dimension.
    packets.add(
        new Respawn(joinGame.getDimension(), joinGame.getPartialHashedSeed(),
            joinGame.getDifficulty(), joinGame.getGamemode(), joinGame.getLevelType(),
            false, joinGame.getDimensionInfo(), joinGame.getPreviousGamemode(),
            joinGame.getCurrentDimensionData())
    );

    return packets;
  }

  public ChunkData createChunkDataPacket(VirtualChunk chunk, int skyLightY) {
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
