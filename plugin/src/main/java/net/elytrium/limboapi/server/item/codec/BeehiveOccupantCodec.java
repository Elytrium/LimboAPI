package net.elytrium.limboapi.server.item.codec;

import net.elytrium.limboapi.api.world.item.datacomponent.type.BeehiveOccupant;
import net.elytrium.limboapi.protocol.codec.ByteBufCodecs;
import net.elytrium.limboapi.protocol.codec.StreamCodec;
import net.elytrium.limboapi.server.item.codec.data.TypedEntityDataCodec;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface BeehiveOccupantCodec {

  StreamCodec<BeehiveOccupant> CODEC = StreamCodec.composite(
      TypedEntityDataCodec.TYPED_ENTITY_DATA_CODEC, BeehiveOccupant::entityData,
      ByteBufCodecs.VAR_INT, BeehiveOccupant::ticksInHive,
      ByteBufCodecs.VAR_INT, BeehiveOccupant::minTicksInHive,
      BeehiveOccupant::new
  );
}
