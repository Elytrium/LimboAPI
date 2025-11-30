/*
 * Copyright (C) 2021 - 2024 Elytrium
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

package net.elytrium.limboapi.protocol.packets.s2c;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.handler.codec.DecoderException;
import io.netty.util.ReferenceCounted;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntObjectPair;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Base64;
import java.util.EnumMap;
import java.util.Map;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.function.Function;
import net.elytrium.limboapi.LimboAPI;
import net.elytrium.limboapi.api.protocol.data.BlockPos;
import net.elytrium.limboapi.api.protocol.data.EntityData;
import net.elytrium.limboapi.api.protocol.data.GlobalPos;
import net.elytrium.limboapi.api.protocol.data.ItemStack;
import net.elytrium.limboapi.api.protocol.data.ComponentHolder;
import net.elytrium.limboapi.api.world.item.datacomponent.type.ResolvableProfile;
import net.elytrium.limboapi.api.world.item.datacomponent.type.data.Holder;
import net.elytrium.limboapi.protocol.codec.ByteBufCodecs;
import net.elytrium.limboapi.protocol.codec.StreamCodec;
import net.elytrium.limboapi.protocol.codec.StreamDecoder;
import net.elytrium.limboapi.server.item.codec.PaintingVariantCodec;
import net.elytrium.limboapi.server.item.codec.ResolvableProfileCodec;
import net.elytrium.limboapi.server.item.codec.data.BlockPosCodec;
import net.elytrium.limboapi.server.item.codec.data.BlockStateCodec;
import net.elytrium.limboapi.server.item.codec.data.ComponentHolderCodec;
import net.elytrium.limboapi.server.item.codec.data.GlobalPosCodec;
import net.elytrium.limboapi.server.item.codec.data.HolderSetCodec;
import net.elytrium.limboapi.server.item.codec.data.ItemStackCodec;
import net.elytrium.limboapi.server.item.codec.data.OptionalBlockStateCodec;
import net.elytrium.limboapi.server.item.codec.data.ParticleCodec;
import net.elytrium.limboapi.server.item.codec.data.QuaternionCodec;
import net.elytrium.limboapi.server.item.codec.data.RotationsCodec;
import net.elytrium.limboapi.server.item.codec.data.Vector3Codec;
import net.elytrium.limboapi.server.item.codec.data.VillagerDataCodec;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.EndBinaryTag;

/**
 * See OptionalUUID codec
 */
public class SetEntityDataPacket implements MinecraftPacket, ReferenceCounted { // TODO check for uuid in nbts

  public static final int EOF_MARKER_LEGACY = Byte.MAX_VALUE;
  public static final int EOF_MARKER = -Byte.MIN_VALUE + Byte.MAX_VALUE;

  private int id;
  private EntityData data;

  private ByteBuf fallbackBuf; // TODO remove after proper tests

  public SetEntityDataPacket(int id, EntityData data) {
    this.id = id;
    this.data = data;
  }

  public SetEntityDataPacket() {

  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    int readerIndex = buf.readerIndex();
    try {
      var registry = RegistryBuilder.CODECS.get(protocolVersion).getKey();
      boolean v1_7_x = protocolVersion.noGreaterThan(ProtocolVersion.MINECRAFT_1_7_6);
      if (v1_7_x || protocolVersion == ProtocolVersion.MINECRAFT_1_8) {
        this.id = v1_7_x ? buf.readInt() : ProtocolUtils.readVarInt(buf);
        byte packedId;
        while ((packedId = buf.readByte()) != SetEntityDataPacket.EOF_MARKER_LEGACY) {
          if (this.data == null) {
            this.data = new EntityData();
          }

          short id = (short) (packedId & 0b00011111);
          int type = (packedId & 0b11100000) >> 5;
          var decoder = registry.get(type);
          if (decoder == null) {
            throw new IllegalArgumentException("Don't know how to decode type " + type + " (id: " + id + ", entityId: " + this.id + ")");
          }
          this.data.put(id, decoder.decode(buf, protocolVersion));
        }
      } else {
        this.id = ProtocolUtils.readVarInt(buf);
        short id;
        while ((id = buf.readUnsignedByte()) != SetEntityDataPacket.EOF_MARKER) {
          if (this.data == null) {
            this.data = new EntityData();
          }

          int type = ProtocolUtils.readVarInt(buf);
          var decoder = registry.get(type);
          if (decoder == null) {
            throw new IllegalArgumentException("Don't know how to decode type " + type + " (id: " + id + ", entityId: " + this.id + ")");
          }
          this.data.put(id, decoder.decode(buf, protocolVersion));
        }
      }
    } catch (Throwable t) {
      this.fallbackBuf = buf.retainedDuplicate().readerIndex(readerIndex);
      LimboAPI.getLogger().error("Failed to read SetEntityDataPacket (direction={}, version={}, data=\"{}\")", direction, protocolVersion, Base64.getEncoder().encodeToString(ByteBufUtil.getBytes(this.fallbackBuf)), t);
      buf.readerIndex(buf.writerIndex());
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    if (this.fallbackBuf != null) {
      buf.writeBytes(this.fallbackBuf);
      return;
    }

    var registry = RegistryBuilder.CODECS.get(protocolVersion).getValue();
    boolean v1_7_x = protocolVersion.noGreaterThan(ProtocolVersion.MINECRAFT_1_7_6);
    if (v1_7_x || protocolVersion == ProtocolVersion.MINECRAFT_1_8) {
      if (v1_7_x) {
        buf.writeInt(this.id);
      } else {
        ProtocolUtils.writeVarInt(buf, this.id);
      }
      if (this.data != null) {
        this.data.short2ObjectEntrySet().fastForEach(entry -> {
          Object value = entry.getValue();
          var pair = registry.get(value.getClass());
          if (pair == null) {
            throw new IllegalArgumentException("Don't know how to encode " + value + " (" + value.getClass() + ") for " + protocolVersion);
          }
          buf.writeByte((pair.leftInt() << 5 | entry.getShortKey() & 0b00011111) & 0xFF);
          ((StreamCodec<Object>) pair.right()).encode(buf, protocolVersion, value);
        });
      }

      buf.writeByte(SetEntityDataPacket.EOF_MARKER_LEGACY);
    } else {
      ProtocolUtils.writeVarInt(buf, this.id);
      if (this.data != null) {
        this.data.short2ObjectEntrySet().fastForEach(entry -> {
          buf.writeByte(entry.getShortKey());
          Object value = entry.getValue();
          var pair = registry.get(value.getClass());
          if (pair == null) {
            throw new IllegalArgumentException("Don't know how to encode " + value + " (" + value.getClass() + ") for " + protocolVersion);
          }
          ProtocolUtils.writeVarInt(buf, pair.leftInt());
          ((StreamCodec<Object>) pair.right()).encode(buf, protocolVersion, value);
        });
      }

      buf.writeByte(SetEntityDataPacket.EOF_MARKER);
    }
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    this.retain();
    return false; // forward it to the player
  }

  @Override
  public int refCnt() {
    return this.fallbackBuf == null ? -1 : this.fallbackBuf.refCnt();
  }

  @Override
  public ReferenceCounted retain() {
    if (this.fallbackBuf != null) {
      this.fallbackBuf.retain();
    }

    return this;
  }

  @Override
  public ReferenceCounted retain(int increment) {
    if (this.fallbackBuf != null) {
      this.fallbackBuf.retain(increment);
    }

    return this;
  }

  @Override
  public ReferenceCounted touch() {
    if (this.fallbackBuf != null) {
      this.fallbackBuf.touch();
    }

    return this;
  }

  @Override
  public ReferenceCounted touch(Object hint) {
    if (this.fallbackBuf != null) {
      this.fallbackBuf.touch(hint);
    }

    return this;
  }

  @Override
  public boolean release() {
    return this.fallbackBuf != null && this.fallbackBuf.release();
  }

  @Override
  public boolean release(int decrement) {
    return this.fallbackBuf != null && this.fallbackBuf.release(decrement);
  }

  static {
    final Function<Long, Integer> long2Int = Long::intValue;
    final Function<OptionalInt, Integer> optionalInt2Int = OptionalInt::orElseThrow;
    try (var builder = new RegistryBuilder<>(ProtocolVersion.MINECRAFT_1_7_2)) {
      builder.register(Byte.class, ByteBufCodecs.BYTE).map(Boolean.class, value -> (byte) (value ? 1 : 0));
      builder.register(Short.class, ByteBufCodecs.SHORT);
      builder.register(Integer.class, ByteBufCodecs.INT).map(Long.class, long2Int).map(OptionalInt.class, optionalInt2Int);
      builder.register(Float.class, ByteBufCodecs.FLOAT);
      builder.register(String.class, ByteBufCodecs.STRING_UTF8)
          .map(EntityData.OptionalUUID.class, optional -> LimboAPI.getClientUniqueId(optional.orElseThrow()).toString())
          .map(ComponentHolder.class, component -> component.getJson(ProtocolVersion.MINECRAFT_1_7_2))
          .map(EntityData.OptionalComponentHolder.class, optional -> optional.orElseThrow().getJson(ProtocolVersion.MINECRAFT_1_7_2));
      builder.register(ItemStack.class, ItemStackCodec.OPTIONAL_CODEC);
      builder.register(BlockPos.class, BlockPosCodec.CODEC).map(EntityData.OptionalBlockPos.class, EntityData.OptionalBlockPos::orElseThrow);
    }
    try (var builder = new RegistryBuilder<>(ProtocolVersion.MINECRAFT_1_8).inheritAllFrom(ProtocolVersion.MINECRAFT_1_7_2)) {
      builder.register(EntityData.Rotations.class, RotationsCodec.CODEC);
    }
    try (var builder = new RegistryBuilder<>(ProtocolVersion.MINECRAFT_1_9)) {
      builder.inherit(Byte.class);
      builder.register(Integer.class, ByteBufCodecs.VAR_INT).map(Short.class, Short::intValue).map(Long.class, long2Int).map(OptionalInt.class, optionalInt2Int);
      builder.inherit(Float.class);
      builder.inherit(String.class);
      builder.register(ComponentHolder.class, ComponentHolderCodec.CODEC).map(EntityData.OptionalComponentHolder.class, EntityData.OptionalComponentHolder::orElseThrow);
      builder.inherit(ItemStack.class);
      builder.register(Boolean.class, ByteBufCodecs.BOOL);
      builder.inherit(EntityData.Rotations.class);
      builder.inherit(BlockPos.class);
      builder.register(EntityData.OptionalBlockPos.class, BlockPosCodec.OPTIONAL_CODEC.map(EntityData.OptionalBlockPos::of, EntityData.OptionalBlockPos::blockPos));
      builder.register(EntityData.Direction.class, ByteBufCodecs.VAR_INT.map(id -> EntityData.Direction.VALUES[id], EntityData.Direction::ordinal));
      builder.register(EntityData.OptionalUUID.class, ByteBufCodecs.OPTIONAL_UUID.map(EntityData.OptionalUUID::of, optional -> {
        UUID uuid = optional.uuid();
        return uuid == null ? null : LimboAPI.getClientUniqueId(uuid);
      }));
      builder.register(EntityData.OptionalBlockState.class, OptionalBlockStateCodec.CODEC).map(EntityData.BlockState.class, BlockStateCodec.CODEC);
    }
    Class<?> CompoundBinaryTagImpl = CompoundBinaryTag.empty().getClass();
    Class<?> EndBinaryTagImpl = EndBinaryTag.endBinaryTag().getClass();
    try (var builder = new RegistryBuilder<>(ProtocolVersion.MINECRAFT_1_12).inheritAllFrom(ProtocolVersion.MINECRAFT_1_9)) {
      builder.register(CompoundBinaryTag.class, ByteBufCodecs.OPTIONAL_COMPOUND_TAG.map((tag, version) -> {
        if (tag == null) {
          if (version.noLessThan(ProtocolVersion.MINECRAFT_1_20_5)) {
            throw new DecoderException("Expected non-null compound tag");
          }

          return EndBinaryTag.endBinaryTag();
        }

        return tag;
      }, (tag, version) -> (CompoundBinaryTag) tag)).alias(CompoundBinaryTagImpl).map(EndBinaryTag.class, ByteBufCodecs.OPTIONAL_TAG.map((tag, version) -> tag, (tag, version) -> {
        if (version.noLessThan(ProtocolVersion.MINECRAFT_1_20_5)) {
          throw new DecoderException("Expected non-null compound tag");
        }

        return tag;
      })).alias(EndBinaryTagImpl);
    }
    try (var builder = new RegistryBuilder<>(ProtocolVersion.MINECRAFT_1_13)) {
      builder.inherit(Byte.class);
      builder.inherit(Integer.class).inheritMap(Short.class).inheritMap(Long.class).inheritMap(OptionalInt.class);
      builder.inherit(Float.class);
      builder.inherit(String.class);
      builder.inherit(ComponentHolder.class);
      builder.register(EntityData.OptionalComponentHolder.class,
          ComponentHolderCodec.OPTIONAL_CODEC.map(EntityData.OptionalComponentHolder::of, EntityData.OptionalComponentHolder::component)
      );
      builder.inherit(ItemStack.class);
      builder.inherit(Boolean.class);
      builder.inherit(EntityData.Rotations.class);
      builder.inherit(BlockPos.class);
      builder.inherit(EntityData.OptionalBlockPos.class);
      builder.inherit(EntityData.Direction.class);
      builder.inherit(EntityData.OptionalUUID.class);
      builder.inherit(EntityData.OptionalBlockState.class).inheritMap(EntityData.BlockState.class);
      builder.inherit(CompoundBinaryTag.class).alias(CompoundBinaryTagImpl).inheritMap(EndBinaryTag.class).alias(EndBinaryTagImpl);
      builder.register(EntityData.Particle.class, ParticleCodec.CODEC);
    }
    try (var builder = new RegistryBuilder<>(ProtocolVersion.MINECRAFT_1_14)) {
      builder.inherit(Byte.class);
      builder.inherit(Integer.class).inheritMap(Short.class).inheritMap(Long.class);
      builder.inherit(Float.class);
      builder.inherit(String.class);
      builder.inherit(ComponentHolder.class);
      builder.inherit(EntityData.OptionalComponentHolder.class);
      builder.inherit(ItemStack.class);
      builder.inherit(Boolean.class);
      builder.inherit(EntityData.Rotations.class);
      builder.inherit(BlockPos.class);
      builder.inherit(EntityData.OptionalBlockPos.class);
      builder.inherit(EntityData.Direction.class);
      builder.inherit(EntityData.OptionalUUID.class);
        builder.inherit(EntityData.OptionalBlockState.class).inheritMap(EntityData.BlockState.class);
      builder.inherit(CompoundBinaryTag.class).alias(CompoundBinaryTagImpl).inheritMap(EndBinaryTag.class).alias(EndBinaryTagImpl);
      builder.inherit(EntityData.Particle.class);
      builder.register(EntityData.VillagerData.class, VillagerDataCodec.CODEC);
      builder.register(OptionalInt.class, ByteBufCodecs.VAR_INT.map(result -> result == 0 ? OptionalInt.empty() : OptionalInt.of(result - 1), value -> value.orElse(-1) + 1));
      builder.register(EntityData.Pose.class, ByteBufCodecs.VAR_INT.map(EntityData.Pose::fromProtocolId, EntityData.Pose::getProtocolId));
    }
    try (var builder = new RegistryBuilder<>(ProtocolVersion.MINECRAFT_1_19).inheritAllFrom(ProtocolVersion.MINECRAFT_1_14)) {
      builder.register(EntityData.CatVariant.class, ByteBufCodecs.VAR_INT.map(EntityData.CatVariant::new, EntityData.CatVariant::id));
      builder.register(EntityData.FrogVariant.class, ByteBufCodecs.VAR_INT.map(EntityData.FrogVariant::new, EntityData.FrogVariant::id));
      builder.register(EntityData.OptionalGlobalPos.class, GlobalPosCodec.OPTIONAL_CODEC.map(EntityData.OptionalGlobalPos::new, EntityData.OptionalGlobalPos::globalPos))
          .map(GlobalPos.class, GlobalPosCodec.OPTIONAL_CODEC);
      builder.register(EntityData.PaintingVariantHolder.class,
          ByteBufCodecs.VAR_INT.map(id -> new EntityData.PaintingVariantHolder(Holder.ref(id)), holder -> holder.painting() instanceof Holder.Reference<?> reference ? reference.id() : 0)
      );
    }
    try (var builder = new RegistryBuilder<>(ProtocolVersion.MINECRAFT_1_19_3)) {
      builder.inherit(Byte.class);
      builder.inherit(Integer.class).inheritMap(Short.class);
      builder.register(Long.class, ByteBufCodecs.VAR_LONG);
      builder.inherit(Float.class);
      builder.inherit(String.class);
      builder.inherit(ComponentHolder.class);
      builder.inherit(EntityData.OptionalComponentHolder.class);
      builder.inherit(ItemStack.class);
      builder.inherit(Boolean.class);
      builder.inherit(EntityData.Rotations.class);
      builder.inherit(BlockPos.class);
      builder.inherit(EntityData.OptionalBlockPos.class);
      builder.inherit(EntityData.Direction.class);
      builder.inherit(EntityData.OptionalUUID.class);
      builder.inherit(EntityData.OptionalBlockState.class).inheritMap(EntityData.BlockState.class);
      builder.inherit(CompoundBinaryTag.class).alias(CompoundBinaryTagImpl).inheritMap(EndBinaryTag.class).alias(EndBinaryTagImpl);
      builder.inherit(EntityData.Particle.class);
      builder.inherit(EntityData.VillagerData.class);
      builder.inherit(OptionalInt.class);
      builder.inherit(EntityData.Pose.class);
      builder.inherit(EntityData.CatVariant.class);
      builder.inherit(EntityData.FrogVariant.class);
      builder.inherit(EntityData.OptionalGlobalPos.class).inheritMap(GlobalPos.class);
      builder.inherit(EntityData.PaintingVariantHolder.class);
    }
    try (var builder = new RegistryBuilder<>(ProtocolVersion.MINECRAFT_1_19_4)) {
      builder.inherit(Byte.class);
      builder.inherit(Integer.class).inheritMap(Short.class);
      builder.inherit(Long.class);
      builder.inherit(Float.class);
      builder.inherit(String.class);
      builder.inherit(ComponentHolder.class);
      builder.inherit(EntityData.OptionalComponentHolder.class);
      builder.inherit(ItemStack.class);
      builder.inherit(Boolean.class);
      builder.inherit(EntityData.Rotations.class);
      builder.inherit(BlockPos.class);
      builder.inherit(EntityData.OptionalBlockPos.class);
      builder.inherit(EntityData.Direction.class);
      builder.inherit(EntityData.OptionalUUID.class);
      builder.inherit(EntityData.BlockState.class); // <-- The reason why inheritAllFrom not used
      builder.inherit(EntityData.OptionalBlockState.class);
      builder.inherit(CompoundBinaryTag.class).alias(CompoundBinaryTagImpl).inheritMap(EndBinaryTag.class).alias(EndBinaryTagImpl);
      builder.inherit(EntityData.Particle.class);
      builder.inherit(EntityData.VillagerData.class);
      builder.inherit(OptionalInt.class);
      builder.inherit(EntityData.Pose.class);
      builder.inherit(EntityData.CatVariant.class);
      builder.inherit(EntityData.FrogVariant.class);
      builder.inherit(EntityData.OptionalGlobalPos.class).inheritMap(GlobalPos.class);
      builder.inherit(EntityData.PaintingVariantHolder.class);
      builder.register(EntityData.SnifferState.class, ByteBufCodecs.VAR_INT.map(id -> EntityData.SnifferState.VALUES[id], EntityData.SnifferState::ordinal));
      builder.register(EntityData.Vector3.class, Vector3Codec.CODEC);
      builder.register(EntityData.Quaternion.class, QuaternionCodec.CODEC);
    }
    StreamCodec<EntityData.WolfVariant> wolfVariantCodec = ByteBufCodecs.VAR_INT.map(EntityData.WolfVariant.Reference::new,
        variant -> variant instanceof EntityData.WolfVariant.Reference reference ? reference.id() : 0
    );
    try (var builder = new RegistryBuilder<>(ProtocolVersion.MINECRAFT_1_20_5)) {
      builder.inherit(Byte.class);
      builder.inherit(Integer.class).inheritMap(Short.class);
      builder.inherit(Long.class);
      builder.inherit(Float.class);
      builder.inherit(String.class);
      builder.inherit(ComponentHolder.class);
      builder.inherit(EntityData.OptionalComponentHolder.class);
      builder.inherit(ItemStack.class);
      builder.inherit(Boolean.class);
      builder.inherit(EntityData.Rotations.class);
      builder.inherit(BlockPos.class);
      builder.inherit(EntityData.OptionalBlockPos.class);
      builder.inherit(EntityData.Direction.class);
      builder.inherit(EntityData.OptionalUUID.class);
      builder.inherit(EntityData.BlockState.class);
      builder.inherit(EntityData.OptionalBlockState.class);
      builder.inherit(CompoundBinaryTag.class).alias(CompoundBinaryTagImpl).inheritMap(EndBinaryTag.class).alias(EndBinaryTagImpl);
      builder.inherit(EntityData.Particle.class);
      builder.register(EntityData.Particles.class, ParticleCodec.COLLECTION_CODEC);
      builder.inherit(EntityData.VillagerData.class);
      builder.inherit(OptionalInt.class);
      builder.inherit(EntityData.Pose.class);
      builder.inherit(EntityData.CatVariant.class);
      builder.register(EntityData.WolfVariant.class, wolfVariantCodec).map(EntityData.WolfVariant.Reference.class, wolfVariantCodec).map(EntityData.WolfVariant.Direct.class, wolfVariantCodec);
      builder.inherit(EntityData.FrogVariant.class);
      builder.inherit(EntityData.OptionalGlobalPos.class).inheritMap(GlobalPos.class);
      builder.inherit(EntityData.PaintingVariantHolder.class);
      builder.inherit(EntityData.SnifferState.class);
      builder.register(EntityData.ArmadilloState.class, ByteBufCodecs.VAR_INT.map(id -> EntityData.ArmadilloState.VALUES[id], EntityData.ArmadilloState::ordinal));
      builder.inherit(EntityData.Vector3.class);
      builder.inherit(EntityData.Quaternion.class);
    }
    try (var builder = new RegistryBuilder<>(ProtocolVersion.MINECRAFT_1_21)) {
      builder.inherit(Byte.class);
      builder.inherit(Integer.class).inheritMap(Short.class);
      builder.inherit(Long.class);
      builder.inherit(Float.class);
      builder.inherit(String.class);
      builder.inherit(ComponentHolder.class);
      builder.inherit(EntityData.OptionalComponentHolder.class);
      builder.inherit(ItemStack.class);
      builder.inherit(Boolean.class);
      builder.inherit(EntityData.Rotations.class);
      builder.inherit(BlockPos.class);
      builder.inherit(EntityData.OptionalBlockPos.class);
      builder.inherit(EntityData.Direction.class);
      builder.inherit(EntityData.OptionalUUID.class);
      builder.inherit(EntityData.BlockState.class);
      builder.inherit(EntityData.OptionalBlockState.class);
      builder.inherit(CompoundBinaryTag.class).alias(CompoundBinaryTagImpl).inheritMap(EndBinaryTag.class).alias(EndBinaryTagImpl);
      builder.inherit(EntityData.Particle.class);
      builder.inherit(EntityData.Particles.class);
      builder.inherit(EntityData.VillagerData.class);
      builder.inherit(OptionalInt.class);
      builder.inherit(EntityData.Pose.class);
      builder.inherit(EntityData.CatVariant.class);
      StreamCodec<EntityData.WolfVariant> wolfVariantHolder = new StreamCodec<>() {

        private static final StreamCodec<EntityData.WolfVariant.Direct> DIRECT_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, EntityData.WolfVariant.Direct::wildTexture,
            ByteBufCodecs.STRING_UTF8, EntityData.WolfVariant.Direct::tameTexture,
            ByteBufCodecs.STRING_UTF8, EntityData.WolfVariant.Direct::angryTexture,
            HolderSetCodec.CODEC, EntityData.WolfVariant.Direct::biomes,
            EntityData.WolfVariant.Direct::new
        );

        @Override
        public EntityData.WolfVariant decode(ByteBuf buf, ProtocolVersion version) {
          int i = ProtocolUtils.readVarInt(buf);
          return i == 0 ? DIRECT_CODEC.decode(buf, version) : EntityData.WolfVariant.of(i - 1);
        }

        @Override
        public void encode(ByteBuf buf, ProtocolVersion version, EntityData.WolfVariant value) {
          if (value instanceof EntityData.WolfVariant.Reference reference) {
            ProtocolUtils.writeVarInt(buf, reference.id() + 1);
          } else if (value instanceof EntityData.WolfVariant.Direct direct) {
            ProtocolUtils.writeVarInt(buf, 0);
            DIRECT_CODEC.encode(buf, version, direct);
          } else {
            throw new IllegalArgumentException(value.getClass().getName());
          }
        }
      };
      builder.register(EntityData.WolfVariant.class, wolfVariantHolder).map(EntityData.WolfVariant.Reference.class, wolfVariantHolder).map(EntityData.WolfVariant.Direct.class, wolfVariantHolder);
      builder.inherit(EntityData.FrogVariant.class);
      builder.inherit(EntityData.OptionalGlobalPos.class).inheritMap(GlobalPos.class);
      builder.register(EntityData.PaintingVariantHolder.class, StreamCodec.composite(PaintingVariantCodec.HOLDER_CODEC, EntityData.PaintingVariantHolder::painting, EntityData.PaintingVariantHolder::new));
      builder.inherit(EntityData.SnifferState.class);
      builder.inherit(EntityData.ArmadilloState.class);
      builder.inherit(EntityData.Vector3.class);
      builder.inherit(EntityData.Quaternion.class);
    }
    try (var builder = new RegistryBuilder<>(ProtocolVersion.MINECRAFT_1_21_5)) {
      builder.inherit(Byte.class);
      builder.inherit(Integer.class).inheritMap(Short.class);
      builder.inherit(Long.class);
      builder.inherit(Float.class);
      builder.inherit(String.class);
      builder.inherit(ComponentHolder.class);
      builder.inherit(EntityData.OptionalComponentHolder.class);
      builder.inherit(ItemStack.class);
      builder.inherit(Boolean.class);
      builder.inherit(EntityData.Rotations.class);
      builder.inherit(BlockPos.class);
      builder.inherit(EntityData.OptionalBlockPos.class);
      builder.inherit(EntityData.Direction.class);
      builder.inherit(EntityData.OptionalUUID.class);
      builder.inherit(EntityData.BlockState.class);
      builder.inherit(EntityData.OptionalBlockState.class);
      builder.inherit(CompoundBinaryTag.class).alias(CompoundBinaryTagImpl).inheritMap(EndBinaryTag.class).alias(EndBinaryTagImpl);
      builder.inherit(EntityData.Particle.class);
      builder.inherit(EntityData.Particles.class);
      builder.inherit(EntityData.VillagerData.class);
      builder.inherit(OptionalInt.class);
      builder.inherit(EntityData.Pose.class);
      builder.inherit(EntityData.CatVariant.class);
      builder.register(EntityData.CowVariant.class, ByteBufCodecs.VAR_INT.map(EntityData.CowVariant::new, EntityData.CowVariant::id));
      builder.register(EntityData.WolfVariant.class, wolfVariantCodec).map(EntityData.WolfVariant.Reference.class, wolfVariantCodec).map(EntityData.WolfVariant.Direct.class, wolfVariantCodec);
      builder.register(EntityData.WolfSoundVariant.class, ByteBufCodecs.VAR_INT.map(EntityData.WolfSoundVariant::new, EntityData.WolfSoundVariant::id));
      builder.inherit(EntityData.FrogVariant.class);
      builder.register(EntityData.PigVariant.class, ByteBufCodecs.VAR_INT.map(EntityData.PigVariant::new, EntityData.PigVariant::id));
      builder.register(EntityData.ChickenVariant.class, ByteBufCodecs.VAR_INT.map(EntityData.ChickenVariant::new, EntityData.ChickenVariant::id));
      builder.inherit(EntityData.OptionalGlobalPos.class).inheritMap(GlobalPos.class);
      builder.inherit(EntityData.PaintingVariantHolder.class);
      builder.inherit(EntityData.SnifferState.class);
      builder.inherit(EntityData.ArmadilloState.class);
      builder.inherit(EntityData.Vector3.class);
      builder.inherit(EntityData.Quaternion.class);
    }
    try (var builder = new RegistryBuilder<>(ProtocolVersion.MINECRAFT_1_21_9)) {
      builder.inherit(Byte.class);
      builder.inherit(Integer.class).inheritMap(Short.class);
      builder.inherit(Long.class);
      builder.inherit(Float.class);
      builder.inherit(String.class);
      builder.inherit(ComponentHolder.class);
      builder.inherit(EntityData.OptionalComponentHolder.class);
      builder.inherit(ItemStack.class);
      builder.inherit(Boolean.class);
      builder.inherit(EntityData.Rotations.class);
      builder.inherit(BlockPos.class);
      builder.inherit(EntityData.OptionalBlockPos.class);
      builder.inherit(EntityData.Direction.class);
      builder.inherit(EntityData.OptionalUUID.class);
      builder.inherit(EntityData.BlockState.class);
      builder.inherit(EntityData.OptionalBlockState.class);
      builder.inherit(EntityData.Particle.class);
      builder.inherit(EntityData.Particles.class);
      builder.inherit(EntityData.VillagerData.class);
      builder.inherit(OptionalInt.class);
      builder.inherit(EntityData.Pose.class);
      builder.inherit(EntityData.CatVariant.class);
      builder.inherit(EntityData.CowVariant.class);
      builder.inherit(EntityData.WolfVariant.class).inheritMap(EntityData.WolfVariant.Reference.class).inheritMap(EntityData.WolfVariant.Direct.class);
      builder.inherit(EntityData.WolfSoundVariant.class);
      builder.inherit(EntityData.FrogVariant.class);
      builder.inherit(EntityData.PigVariant.class);
      builder.inherit(EntityData.ChickenVariant.class);
      builder.inherit(EntityData.OptionalGlobalPos.class).inheritMap(GlobalPos.class);
      builder.inherit(EntityData.PaintingVariantHolder.class);
      builder.inherit(EntityData.SnifferState.class);
      builder.inherit(EntityData.ArmadilloState.class);
      builder.register(EntityData.CopperGolemState.class, ByteBufCodecs.VAR_INT.map(id -> EntityData.CopperGolemState.VALUES[id], EntityData.CopperGolemState::ordinal));
      builder.register(EntityData.WeatheringCopperState.class, ByteBufCodecs.VAR_INT.map(id -> EntityData.WeatheringCopperState.VALUES[id], EntityData.WeatheringCopperState::ordinal));
      builder.inherit(EntityData.Vector3.class);
      builder.inherit(EntityData.Quaternion.class);
      builder.register(ResolvableProfile.class, ResolvableProfileCodec.CODEC);
    }
  }

  private static class RegistryBuilder<T> implements AutoCloseable {

    private static final EnumMap<ProtocolVersion, Map.Entry<Int2ObjectOpenHashMap<StreamDecoder<?>>, Object2ObjectOpenHashMap<Class<?>, IntObjectPair<StreamCodec<?>>>>> CODECS = new EnumMap<>(ProtocolVersion.class);
    private static final ProtocolVersion[] VERSIONS = ProtocolVersion.values();

    private final Int2ObjectOpenHashMap<StreamDecoder<?>> id2Codec = new Int2ObjectOpenHashMap<>();
    private final Object2ObjectOpenHashMap<Class<?>, IntObjectPair<StreamCodec<?>>> type2Codec = new Object2ObjectOpenHashMap<>();
    private final ProtocolVersion version;

    private int index = -1;

    private IntObjectPair<StreamCodec<?>> currentPair;

    private RegistryBuilder(ProtocolVersion version) {
      this.version = version;
    }

    private RegistryBuilder<T> inheritAllFrom(ProtocolVersion version) {
      var entry = RegistryBuilder.CODECS.get(version);
      var key = entry.getKey();
      this.id2Codec.putAll(key);
      this.type2Codec.putAll(entry.getValue());
      this.index = key.size() - 1;
      return this;
    }

    @CanIgnoreReturnValue
    @SuppressWarnings({"unchecked", "rawtypes"})
    private RegistryBuilder<T> inherit(Class<?> clazz) {
      var pair = this.codecFromPreviousVersion(clazz);
      StreamCodec<?> codec = pair.right();
      if (this.index + 1 == pair.firstInt()) {
        this.id2Codec.put(++this.index, codec);
        this.type2Codec.put(clazz, this.currentPair = pair);
        return this;
      }

      return this.register(clazz, (StreamCodec) codec);
    }

    @CanIgnoreReturnValue
    @SuppressWarnings("unchecked")
    private <R> RegistryBuilder<R> register(Class<? extends R> clazz, StreamCodec<R> codec) {
      this.id2Codec.put(++this.index, codec);
      this.type2Codec.put(clazz, this.currentPair = IntObjectPair.of(this.index, codec));
      return (RegistryBuilder<R>) this;
    }

    @CanIgnoreReturnValue
    private RegistryBuilder<T> inheritMap(Class<?> clazz) {
      var pair = this.codecFromPreviousVersion(clazz);
      if (pair.firstInt() == this.index) {
        this.type2Codec.put(clazz, this.currentPair = pair);
      } else {
        this.map(clazz, pair.right());
      }

      return this;
    }

    @CanIgnoreReturnValue
    private <F> RegistryBuilder<T> alias(Class<F> clazz) {
      this.type2Codec.put(clazz, this.currentPair);
      return this;
    }

    @CanIgnoreReturnValue
    @SuppressWarnings({"unchecked", "rawtypes"})
    private <F> RegistryBuilder<T> map(Class<F> clazz, Function<F, T> mapper) {
      this.type2Codec.put(clazz, IntObjectPair.of(this.index, this.currentPair.right().map(Function.identity(), (Function) mapper)));
      return this;
    }

    @CanIgnoreReturnValue
    private <F> RegistryBuilder<T> map(Class<? extends F> clazz, StreamCodec<? extends F> codec) {
      this.type2Codec.put(clazz, IntObjectPair.of(this.index, codec));
      return this;
    }

    private IntObjectPair<StreamCodec<?>> codecFromPreviousVersion(Class<?> clazz) {
      return RegistryBuilder.CODECS.get(RegistryBuilder.VERSIONS[this.version.ordinal() - 1]).getValue().get(clazz);
    }

    @Override
    public void close() {
      this.id2Codec.trim();
      this.type2Codec.trim();
      var entry = Map.entry(this.id2Codec, this.type2Codec);
      ProtocolVersion[] versions = RegistryBuilder.VERSIONS;
      for (int i = this.version.ordinal(); i < versions.length; ++i) {
        RegistryBuilder.CODECS.put(versions[i], entry);
      }
    }
  }
}
