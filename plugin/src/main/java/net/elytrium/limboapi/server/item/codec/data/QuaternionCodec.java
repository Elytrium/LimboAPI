package net.elytrium.limboapi.server.item.codec.data;

import net.elytrium.limboapi.api.protocol.data.EntityData;
import net.elytrium.limboapi.protocol.codec.ByteBufCodecs;
import net.elytrium.limboapi.protocol.codec.StreamCodec;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface QuaternionCodec {

  StreamCodec<EntityData.Quaternion> CODEC = StreamCodec.composite(
      ByteBufCodecs.FLOAT, EntityData.Quaternion::x,
      ByteBufCodecs.FLOAT, EntityData.Quaternion::y,
      ByteBufCodecs.FLOAT, EntityData.Quaternion::z,
      ByteBufCodecs.FLOAT, EntityData.Quaternion::w,
      EntityData.Quaternion::new
  );
}
