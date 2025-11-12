package net.elytrium.limboapi.server.item.codec.data;

import net.elytrium.limboapi.api.protocol.data.EntityData;
import net.elytrium.limboapi.protocol.codec.ByteBufCodecs;
import net.elytrium.limboapi.protocol.codec.StreamCodec;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface OptionalBlockStateCodec {

  StreamCodec<EntityData.OptionalBlockState> CODEC = ByteBufCodecs.VAR_INT.map(
      id -> EntityData.OptionalBlockState.of(id == 0 ? null : new EntityData.BlockState(id)),
      optional -> {
        EntityData.BlockState value = optional.blockState();
        return value == null ? 0 : value.blockState();
      }
  );
}
