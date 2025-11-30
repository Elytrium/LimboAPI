package net.elytrium.limboapi.server.item.codec.data;

import net.elytrium.limboapi.api.protocol.data.EntityData;
import net.elytrium.limboapi.protocol.codec.ByteBufCodecs;
import net.elytrium.limboapi.protocol.codec.StreamCodec;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface RotationsCodec {

  StreamCodec<EntityData.Rotations> CODEC = StreamCodec.composite(
      ByteBufCodecs.FLOAT, EntityData.Rotations::posX,
      ByteBufCodecs.FLOAT, EntityData.Rotations::posY,
      ByteBufCodecs.FLOAT, EntityData.Rotations::posZ,
      EntityData.Rotations::new
  );
}
