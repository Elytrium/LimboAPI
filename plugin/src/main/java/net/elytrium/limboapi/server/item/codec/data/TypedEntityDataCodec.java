package net.elytrium.limboapi.server.item.codec.data;

import com.velocitypowered.api.network.ProtocolVersion;
import net.elytrium.limboapi.api.world.item.datacomponent.type.data.TypedEntityData;
import net.elytrium.limboapi.protocol.codec.ByteBufCodecs;
import net.elytrium.limboapi.protocol.codec.StreamCodec;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface TypedEntityDataCodec {

  StreamCodec<TypedEntityData> TYPED_ENTITY_DATA_CODEC = StreamCodec.composite(
      StreamCodec.ge(ProtocolVersion.MINECRAFT_1_21_9, ByteBufCodecs.VAR_INT, 0), TypedEntityData::type,
      ByteBufCodecs.COMPOUND_TAG, TypedEntityData::tag,
      TypedEntityData::new
  );
}
