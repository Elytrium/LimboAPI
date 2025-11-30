package net.elytrium.limboapi.server.item.codec;

import net.elytrium.limboapi.api.world.item.datacomponent.type.FireworkExplosion;
import net.elytrium.limboapi.protocol.codec.ByteBufCodecs;
import net.elytrium.limboapi.protocol.codec.StreamCodec;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface FireworkExplosionCodec {

  StreamCodec<FireworkExplosion> CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT, FireworkExplosion::shape,
      ByteBufCodecs.INT_COLLECTION, FireworkExplosion::colors,
      ByteBufCodecs.INT_COLLECTION, FireworkExplosion::fadeColors,
      ByteBufCodecs.BOOL, FireworkExplosion::hasTrail,
      ByteBufCodecs.BOOL, FireworkExplosion::hasTwinkle,
      FireworkExplosion::new
  );
}
