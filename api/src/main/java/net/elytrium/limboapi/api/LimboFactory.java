/*
 * Copyright (C) 2021 - 2022 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.Player;
import java.util.Map;
import java.util.function.Supplier;
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

public interface LimboFactory {

  /**
   * Creates new virtual block from Block enum.
   *
   * @param block Block from Block enum.
   *
   * @return new virtual block.
   */
  VirtualBlock createSimpleBlock(Block block);

  /**
   * Creates new virtual block from id and data.
   *
   * @param legacyID Legacy block id. (1.12.2 and lower)
   *
   * @return new virtual block.
   */
  VirtualBlock createSimpleBlock(short legacyID);

  /**
   * Creates new virtual block from id and data.
   *
   * @param modernID Modern block id.
   * @param properties Modern properties like {"waterlogged": "true"}.
   *
   * @return new virtual block.
   */
  VirtualBlock createSimpleBlock(String modernID, Map<String, String> properties);

  /**
   * Creates new virtual block from id and data.
   *
   * @param legacyID Block id.
   * @param modern   Use the latest supported version ids or 1.12.2 and lower.
   *
   * @return new virtual block.
   */
  VirtualBlock createSimpleBlock(short legacyID, boolean modern);

  /**
   * Creates new virtual customizable block.
   *
   * @param solid          Defines if the block is solid or not.
   * @param air            Defines if the block is the air.
   * @param motionBlocking Defines if the block blocks motions. (1.14+)
   * @param id             Block id.
   *
   * @return new virtual block.
   */
  VirtualBlock createSimpleBlock(boolean solid, boolean air, boolean motionBlocking, short id);

  /**
   * Creates new virtual customizable block.
   *
   * @param solid          Defines if the block is solid or not.
   * @param air            Defines if the block is the air.
   * @param motionBlocking Defines if the block blocks motions. (1.14+)
   * @param modernID       Block id.
   * @param properties     Modern properties like {"waterlogged": "true"}.
   *
   * @return new virtual block.
   */
  VirtualBlock createSimpleBlock(boolean solid, boolean air, boolean motionBlocking, String modernID, Map<String, String> properties);

  /**
   * Creates new virtual world.
   *
   * @param dimension World dimension.
   * @param posX      Spawn location. (X)
   * @param posY      Spawn location. (Y)
   * @param posZ      Spawn location. (Z)
   * @param yaw       Spawn rotation. (Yaw)
   * @param pitch     Spawn rotation. (Pitch)
   *
   * @return new virtual world.
   */
  VirtualWorld createVirtualWorld(Dimension dimension, double posX, double posY, double posZ, float yaw, float pitch);

  /**
   * Creates new virtual chunk with plain biomes set as default.
   * You need to provide the chunk location, you can get it using {@code blockCoordinate >> 4}.
   *
   * @param posX Chunk location. (X)
   * @param posZ Chunk location. (Z)
   *
   * @return new virtual chunk.
   */
  @Deprecated
  VirtualChunk createVirtualChunk(int posX, int posZ);

  /**
   * Creates new virtual chunk.
   * You need to provide the chunk location, you can get it using {@code blockCoordinate >> 4}.
   *
   * @param posX         Chunk location. (X)
   * @param posZ         Chunk location. (Z)
   * @param defaultBiome Default biome to fill it.
   *
   * @return new virtual chunk.
   */
  VirtualChunk createVirtualChunk(int posX, int posZ, VirtualBiome defaultBiome);

  /**
   * Creates new virtual chunk.
   * You need to provide the chunk location, you can get it using (block_coordinate >> 4)
   *
   * @param posX         Chunk location. (X)
   * @param posZ         Chunk location. (Z)
   * @param defaultBiome Default biome to fill it.
   *
   * @return new virtual chunk.
   */
  VirtualChunk createVirtualChunk(int posX, int posZ, BuiltInBiome defaultBiome);

  /**
   * Creates new virtual server.
   *
   * @param world Virtual world.
   *
   * @return new virtual server.
   */
  Limbo createLimbo(VirtualWorld world);


  /**
   * Releases a thread after PreparedPacket#build executions.
   * Used to free compression libraries.
   */
  void releasePreparedPacketThread(Thread thread);

  /**
   * Creates new prepared packet builder.
   *
   * @return new prepared packet.
   */
  PreparedPacket createPreparedPacket();

  /**
   * Creates new prepared packet builder.
   *
   * @param minVersion Minimum version to prepare.
   * @param maxVersion Maximum version to prepare.
   *
   * @return new prepared packet.
   */
  PreparedPacket createPreparedPacket(ProtocolVersion minVersion, ProtocolVersion maxVersion);

  /**
   * Instantiates new MinecraftPacket object.
   *
   * @param data You can find data arguments at the constructors
   *     <a href="https://github.com/Elytrium/LimboAPI/blob/master/plugin/src/main/java/net/elytrium/limboapi/protocol/packets/s2c/">here</a>.
   *
   * @return MinecraftPacket object.
   *
   * @deprecated See {@link LimboFactory#getPacketFactory}.
   */
  @Deprecated(forRemoval = true)
  Object instantiatePacket(BuiltInPackets packetType, Object... data);

  /**
   * Registers self-made packet.
   *
   * @param direction      Packet direction.
   * @param packetClass    Packet class.
   * @param packetSupplier Packet supplier to make a new instance. (::new)
   * @param packetMappings Packet id mappings.
   */
  void registerPacket(PacketDirection direction, Class<?> packetClass, Supplier<?> packetSupplier, PacketMapping[] packetMappings);

  /**
   * Pass the player to the next Login Limbo, without spawning at current Limbo.
   *
   * @param player Player to pass.
   */
  void passLoginLimbo(Player player);

  /**
   * Creates new virtual item from Item enum.
   *
   * @param item Item from item enum.
   *
   * @return new virtual item.
   */
  VirtualItem getItem(Item item);

  /**
   * A factory to instantiate Minecraft packet objects.
   */
  PacketFactory getPacketFactory();

  ProtocolVersion getPrepareMinVersion();

  ProtocolVersion getPrepareMaxVersion();
}
