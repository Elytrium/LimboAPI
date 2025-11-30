package net.elytrium.limboapi.server.item.codec.data;

import net.elytrium.limboapi.api.protocol.data.EntityData;
import net.elytrium.limboapi.protocol.codec.ByteBufCodecs;
import net.elytrium.limboapi.protocol.codec.StreamCodec;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface BlockStateCodec {

  StreamCodec<EntityData.BlockState> CODEC = ByteBufCodecs.VAR_INT.map(EntityData.BlockState::new, EntityData.BlockState::blockState);
}
