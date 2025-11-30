package net.elytrium.limboapi.server.item.codec.data;

import net.elytrium.limboapi.api.protocol.data.EntityData;
import net.elytrium.limboapi.protocol.codec.ByteBufCodecs;
import net.elytrium.limboapi.protocol.codec.StreamCodec;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface Vector3Codec {

  StreamCodec<EntityData.Vector3> CODEC = StreamCodec.composite(
      ByteBufCodecs.FLOAT, EntityData.Vector3::x,
      ByteBufCodecs.FLOAT, EntityData.Vector3::y,
      ByteBufCodecs.FLOAT, EntityData.Vector3::z,
      EntityData.Vector3::new
  );
}
