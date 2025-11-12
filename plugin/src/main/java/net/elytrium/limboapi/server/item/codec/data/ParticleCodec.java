package net.elytrium.limboapi.server.item.codec.data;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import net.elytrium.limboapi.api.protocol.data.BlockPos;
import net.elytrium.limboapi.api.protocol.data.EntityData;
import net.elytrium.limboapi.protocol.codec.ByteBufCodecs;
import net.elytrium.limboapi.protocol.codec.StreamCodec;
import net.elytrium.limboapi.server.world.SimpleParticlesManager;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface ParticleCodec {

  StreamCodec<EntityData.Particle> CODEC = new StreamCodec<>() {

    @Override
    public EntityData.Particle decode(ByteBuf buf, ProtocolVersion version) {
      int type = ProtocolUtils.readVarInt(buf);
      var codec = SimpleParticlesManager.fromProtocolId(version, type).codec();
      return new EntityData.Particle(type, codec == null ? null : codec.decode(buf, version));
    }

    @Override
    public void encode(ByteBuf buf, ProtocolVersion version, EntityData.Particle value) {
      int type = value.type();
      ProtocolUtils.writeVarInt(buf, type);
      var codec = SimpleParticlesManager.fromProtocolId(version, type).codec();
      if (codec != null) {
        codec.encode(buf, version, value.data());
      }
    }
  };
  StreamCodec<EntityData.Particles> COLLECTION_CODEC = ByteBufCodecs.collection(EntityData.Particles::new, ParticleCodec.CODEC);

  interface ParticleDataCodecs {

    StreamCodec<EntityData.Particle.PowerParticleOption> POWER_CODEC = StreamCodec.ge(
        ProtocolVersion.MINECRAFT_1_21_9,
        ByteBufCodecs.FLOAT.map(EntityData.Particle.PowerParticleOption::new, EntityData.Particle.PowerParticleOption::power),
        new EntityData.Particle.PowerParticleOption(1.0F)
    );
    StreamCodec<EntityData.Particle.DustParticleOptions> DUST_PARTICLE_CODEC = new StreamCodec<>() {

      @Override
      public EntityData.Particle.DustParticleOptions decode(ByteBuf buf, ProtocolVersion version) {
        return version.noLessThan(ProtocolVersion.MINECRAFT_1_21_2)
            ? new EntityData.Particle.DustParticleOptions(ProtocolUtils.readVarInt(buf), buf.readFloat())
            : new EntityData.Particle.DustParticleOptions(EntityData.Particle.ColorParticleOption.colorFromFloat(buf.readFloat(), buf.readFloat(), buf.readFloat()), buf.readFloat());
      }

      @Override
      public void encode(ByteBuf buf, ProtocolVersion version, EntityData.Particle.DustParticleOptions value) {
        if (version.noLessThan(ProtocolVersion.MINECRAFT_1_21_2)) {
          ProtocolUtils.writeVarInt(buf, value.color());
        } else {
          int color = value.color();
          buf.writeFloat(EntityData.Particle.ColorParticleOption.redFloat(color));
          buf.writeFloat(EntityData.Particle.ColorParticleOption.greenFloat(color));
          buf.writeFloat(EntityData.Particle.ColorParticleOption.blueFloat(color));
        }

        buf.writeFloat(value.scale());
      }
    };
    StreamCodec<EntityData.Particle.DustColorTransitionOptions> DUST_COLOR_TRANSITION_CODEC = new StreamCodec<>() {

      @Override
      public EntityData.Particle.DustColorTransitionOptions decode(ByteBuf buf, ProtocolVersion version) {
        if (version.noLessThan(ProtocolVersion.MINECRAFT_1_21_2)) {
          return new EntityData.Particle.DustColorTransitionOptions(ProtocolUtils.readVarInt(buf), ProtocolUtils.readVarInt(buf), buf.readFloat());
        }

        float fromRed = buf.readFloat();
        float fromGreen = buf.readFloat();
        float fromBlue = buf.readFloat();
        float toRed;
        float toGreen;
        float toBlue;
        float scale;
        if (version.noLessThan(ProtocolVersion.MINECRAFT_1_20_5)) {
          toRed = buf.readFloat();
          toGreen = buf.readFloat();
          toBlue = buf.readFloat();
          scale = buf.readFloat();
        } else {
          scale = buf.readFloat();
          toRed = buf.readFloat();
          toGreen = buf.readFloat();
          toBlue = buf.readFloat();
        }

        return new EntityData.Particle.DustColorTransitionOptions(
            EntityData.Particle.ColorParticleOption.colorFromFloat(fromRed, fromGreen, fromBlue),
            EntityData.Particle.ColorParticleOption.colorFromFloat(toRed, toGreen, toBlue),
            scale
        );
      }

      @Override
      public void encode(ByteBuf buf, ProtocolVersion version, EntityData.Particle.DustColorTransitionOptions value) {
        if (version.noLessThan(ProtocolVersion.MINECRAFT_1_21_2)) {
          ProtocolUtils.writeVarInt(buf, value.fromColor());
          ProtocolUtils.writeVarInt(buf, value.toColor());
          buf.writeFloat(value.scale());
          return;
        }

        int fromColor = value.fromColor();
        int toColor = value.toColor();
        buf.writeFloat(EntityData.Particle.ColorParticleOption.redFloat(fromColor));
        buf.writeFloat(EntityData.Particle.ColorParticleOption.greenFloat(fromColor));
        buf.writeFloat(EntityData.Particle.ColorParticleOption.blueFloat(fromColor));
        if (version.noLessThan(ProtocolVersion.MINECRAFT_1_20_5)) {
          buf.writeFloat(EntityData.Particle.ColorParticleOption.redFloat(toColor));
          buf.writeFloat(EntityData.Particle.ColorParticleOption.greenFloat(toColor));
          buf.writeFloat(EntityData.Particle.ColorParticleOption.blueFloat(toColor));
          buf.writeFloat(value.scale());
        } else {
          buf.writeFloat(value.scale());
          buf.writeFloat(EntityData.Particle.ColorParticleOption.redFloat(toColor));
          buf.writeFloat(EntityData.Particle.ColorParticleOption.greenFloat(toColor));
          buf.writeFloat(EntityData.Particle.ColorParticleOption.blueFloat(toColor));
        }
      }
    };
    StreamCodec<EntityData.Particle.SpellParticleOption> SPELL_CODEC = StreamCodec.ge(ProtocolVersion.MINECRAFT_1_21_9,
        StreamCodec.composite(
            ByteBufCodecs.INT, EntityData.Particle.SpellParticleOption::color,
            ByteBufCodecs.FLOAT, EntityData.Particle.SpellParticleOption::power,
            EntityData.Particle.SpellParticleOption::new
        ), new EntityData.Particle.SpellParticleOption(-1, 1.0F)
    );
    StreamCodec<EntityData.Particle.ColorParticleOption> COLOR_CODEC = ByteBufCodecs.INT.map(EntityData.Particle.ColorParticleOption::new, EntityData.Particle.ColorParticleOption::color);
    StreamCodec<EntityData.Particle.SculkChargeParticleOptions> SCULK_CHARGE = ByteBufCodecs.FLOAT.map(EntityData.Particle.SculkChargeParticleOptions::new, EntityData.Particle.SculkChargeParticleOptions::roll);
    StreamCodec<EntityData.Particle.VibrationParticle> VIBRATION_CODEC = new StreamCodec<>() {

      @Override
      public EntityData.Particle.VibrationParticle decode(ByteBuf buf, ProtocolVersion version) {
        BlockPos origin = version.noGreaterThan(ProtocolVersion.MINECRAFT_1_18_2) ? BlockPosCodec.CODEC.decode(buf, version) : null;
        EntityData.Particle.VibrationParticle.PositionSource source;
        if (version.noLessThan(ProtocolVersion.MINECRAFT_1_20_3)) {
          int type = ProtocolUtils.readVarInt(buf);
          source = switch (type) {
            case 0 -> BlockPosCodec.CODEC.decode(buf, version);
            case 1 -> EntityPositionSourceCodec.CODEC.decode(buf, version);
            default -> throw new IllegalStateException("Unexpected value: " + type);
          };
        } else {
          String type = ProtocolUtils.readString(buf);
          source = switch (type) {
            case "minecraft:block" -> BlockPosCodec.CODEC.decode(buf, version);
            case "minecraft:entity" -> EntityPositionSourceCodec.CODEC.decode(buf, version);
            default -> throw new IllegalStateException("Unexpected value: " + type);
          };
        }

        return new EntityData.Particle.VibrationParticle(origin, source, ProtocolUtils.readVarInt(buf));
      }

      @Override
      public void encode(ByteBuf buf, ProtocolVersion version, EntityData.Particle.VibrationParticle value) {
        if (version.noGreaterThan(ProtocolVersion.MINECRAFT_1_18_2)) {
          BlockPos origin = value.origin();
          if (origin == null) {
            throw new NullPointerException("origin");
          }

          BlockPosCodec.CODEC.encode(buf, version, origin);
        }

        if (value.destination() instanceof BlockPos blockPos) {
          if (version.noLessThan(ProtocolVersion.MINECRAFT_1_20_3)) {
            ProtocolUtils.writeVarInt(buf, 0);
          } else {
            ProtocolUtils.writeString(buf, "minecraft:block");
          }

          BlockPosCodec.CODEC.encode(buf, version, blockPos);
        } else if (value.destination() instanceof EntityData.Particle.VibrationParticle.EntityPositionSource entity) {
          if (version.noLessThan(ProtocolVersion.MINECRAFT_1_20_3)) {
            ProtocolUtils.writeVarInt(buf, 1);
          } else {
            ProtocolUtils.writeString(buf, "minecraft:entity");
          }

          EntityPositionSourceCodec.CODEC.encode(buf, version, entity);
        }

        ProtocolUtils.writeVarInt(buf, value.arrivalInTicks());
      }
    };
    StreamCodec<EntityData.Particle.TrailParticleOption> TRAIL_CODEC = StreamCodec.composite(
        Vector3Codec.CODEC, EntityData.Particle.TrailParticleOption::target,
        ByteBufCodecs.INT, EntityData.Particle.TrailParticleOption::color,
        StreamCodec.ge(ProtocolVersion.MINECRAFT_1_21_4, ByteBufCodecs.VAR_INT, 44), EntityData.Particle.TrailParticleOption::color,
        EntityData.Particle.TrailParticleOption::new
    );
    StreamCodec<EntityData.Particle.ShriekParticleOption> SHRIEK_CODEC = ByteBufCodecs.VAR_INT.map(EntityData.Particle.ShriekParticleOption::new, EntityData.Particle.ShriekParticleOption::delay);

    interface EntityPositionSourceCodec {

      StreamCodec<EntityData.Particle.VibrationParticle.EntityPositionSource> CODEC = StreamCodec.composite(
          ByteBufCodecs.VAR_INT, EntityData.Particle.VibrationParticle.EntityPositionSource::entityId,
          ByteBufCodecs.FLOAT, EntityData.Particle.VibrationParticle.EntityPositionSource::yOffset,
          EntityData.Particle.VibrationParticle.EntityPositionSource::new
      );
    }
  }
}
