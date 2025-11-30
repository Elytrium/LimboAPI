package net.elytrium.limboapi.server.item.codec;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import java.util.function.Function;
import net.elytrium.limboapi.api.world.item.datacomponent.type.CustomModelData;
import net.elytrium.limboapi.protocol.codec.ByteBufCodecs;
import net.elytrium.limboapi.protocol.codec.StreamCodec;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface CustomModelDataCodec {

  StreamCodec<CustomModelData> CODEC = new StreamCodec<>() {

    private static final StreamCodec<CustomModelData> CODEC = StreamCodec.composite(
        ByteBufCodecs.list(ByteBufCodecs.FLOAT.map(Function.identity(), Number::floatValue)), CustomModelData::floats,
        ByteBufCodecs.list(ByteBufCodecs.BOOL), CustomModelData::flags,
        ByteBufCodecs.list(ByteBufCodecs.STRING_UTF8), CustomModelData::strings,
        ByteBufCodecs.list(ByteBufCodecs.INT), CustomModelData::colors,
        CustomModelData::new
    );

    @Override
    public CustomModelData decode(ByteBuf buf, ProtocolVersion version) {
      return version.noLessThan(ProtocolVersion.MINECRAFT_1_21_4) ? CODEC.decode(buf, version) : new CustomModelData(ProtocolUtils.readVarInt(buf));
    }

    @Override
    public void encode(ByteBuf buf, ProtocolVersion version, CustomModelData value) {
      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_21_4)) {
        CODEC.encode(buf, version, value);
      } else {
        ProtocolUtils.writeVarInt(buf, value.floats().get(0).intValue());
      }
    }
  };
}
