package net.elytrium.limboapi.server.item.codec.data;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import net.elytrium.limboapi.api.world.item.datacomponent.type.data.TypedEntityData;
import net.elytrium.limboapi.protocol.codec.ByteBufCodecs;
import net.elytrium.limboapi.protocol.codec.StreamCodec;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface TypedEntityDataCodec {

  StreamCodec<TypedEntityData> TYPED_ENTITY_DATA_CODEC = new StreamCodec<>() {

    @Override
    public TypedEntityData decode(ByteBuf buf, ProtocolVersion version) {
      return new TypedEntityData(version.noLessThan(ProtocolVersion.MINECRAFT_1_21_9) ? ProtocolUtils.readVarInt(buf) : 0, ByteBufCodecs.COMPOUND_TAG.decode(buf, version));
    }

    @Override
    public void encode(ByteBuf buf, ProtocolVersion version, TypedEntityData value) {
      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_21_9)) {
        ProtocolUtils.writeVarInt(buf, value.type());
      }

      ByteBufCodecs.COMPOUND_TAG.encode(buf, version, value.tag());
    }
  };
}
