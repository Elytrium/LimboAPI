package net.elytrium.limboapi.server.item.codec.data;

import net.elytrium.limboapi.api.protocol.data.EntityData;
import net.elytrium.limboapi.protocol.codec.ByteBufCodecs;
import net.elytrium.limboapi.protocol.codec.StreamCodec;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface VillagerDataCodec {

  StreamCodec<EntityData.VillagerData> CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT, EntityData.VillagerData::type,
      ByteBufCodecs.VAR_INT, EntityData.VillagerData::profession,
      ByteBufCodecs.VAR_INT, EntityData.VillagerData::level,
      EntityData.VillagerData::new
  );
}
