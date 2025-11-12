package net.elytrium.limboapi.server.item.codec;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import net.elytrium.limboapi.api.world.item.datacomponent.type.Instrument;
import net.elytrium.limboapi.api.world.item.datacomponent.type.data.Either;
import net.elytrium.limboapi.api.world.item.datacomponent.type.data.Holder;
import net.elytrium.limboapi.protocol.codec.ByteBufCodecs;
import net.elytrium.limboapi.protocol.codec.StreamCodec;
import net.elytrium.limboapi.server.item.codec.data.ComponentHolderCodec;
import net.elytrium.limboapi.server.item.codec.data.HolderCodec;
import net.elytrium.limboapi.server.item.codec.data.SoundEventCodec;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface InstrumentCodec {

  StreamCodec<Instrument> CODEC = StreamCodec.composite(
      SoundEventCodec.HOLDER_CODEC, Instrument::soundEvent,
      new StreamCodec<>() {

        @Override
        public Number decode(ByteBuf buf, ProtocolVersion version) {
          return version.noLessThan(ProtocolVersion.MINECRAFT_1_21_2) ? buf.readFloat() : ProtocolUtils.readVarInt(buf);
        }

        @Override
        public void encode(ByteBuf buf, ProtocolVersion version, Number value) {
          if (version.noLessThan(ProtocolVersion.MINECRAFT_1_21_2)) {
            buf.writeFloat(value.floatValue());
          } else {
            ProtocolUtils.writeVarInt(buf, value.intValue());
          }
        }
      }, Instrument::useDuration,
      ByteBufCodecs.FLOAT, Instrument::range,
      StreamCodec.ge(ProtocolVersion.MINECRAFT_1_21_2, ComponentHolderCodec.CODEC), Instrument::description,
      Instrument::new
  );
  StreamCodec<Holder<Instrument>> HOLDER_CODEC = HolderCodec.codec(InstrumentCodec.CODEC);
  StreamCodec<Either<Holder<Instrument>, String>> EITHER_CODEC = new StreamCodec<>() {

    @Override
    public Either<Holder<Instrument>, String> decode(ByteBuf buf, ProtocolVersion version) {
      return version.lessThan(ProtocolVersion.MINECRAFT_1_21_5) || buf.readBoolean()
          ? Either.left(InstrumentCodec.HOLDER_CODEC.decode(buf, version))
          : Either.right(ByteBufCodecs.STRING_UTF8.decode(buf, version));
    }

    @Override
    public void encode(ByteBuf buf, ProtocolVersion version, Either<Holder<Instrument>, String> value) {
      value.ifLeft(left -> {
        if (version.noLessThan(ProtocolVersion.MINECRAFT_1_21_5)) {
          buf.writeBoolean(true);
        }

        InstrumentCodec.HOLDER_CODEC.encode(buf, version, left);
      }).ifRight(right -> {
        if (version.noLessThan(ProtocolVersion.MINECRAFT_1_21_5)) {
          buf.writeBoolean(false);
          ByteBufCodecs.STRING_UTF8.encode(buf, version, right);
        } else {
          // untested btw
          int id;
          switch (right) {
            case "minecraft:ponder_goat_horn" -> id = 0;
            case "minecraft:sing_goat_horn" -> id = 1;
            case "minecraft:seek_goat_horn" -> id = 2;
            case "minecraft:feel_goat_horn" -> id = 3;
            case "minecraft:admire_goat_horn" -> id = 4;
            case "minecraft:call_goat_horn" -> id = 5;
            case "minecraft:yearn_goat_horn" -> id = 6;
            case "minecraft:dream_goat_horn" -> id = 7;
            default -> id = 0; // TODO warn
          }
          InstrumentCodec.HOLDER_CODEC.encode(buf, version, Holder.ref(id));
        }
      });
    }
  };
}
