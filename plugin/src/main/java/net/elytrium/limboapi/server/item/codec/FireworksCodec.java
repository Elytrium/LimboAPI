package net.elytrium.limboapi.server.item.codec;

import net.elytrium.limboapi.api.world.item.datacomponent.type.Fireworks;
import net.elytrium.limboapi.protocol.codec.ByteBufCodecs;
import net.elytrium.limboapi.protocol.codec.StreamCodec;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface FireworksCodec {

  StreamCodec<Fireworks> CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT, Fireworks::flightDuration,
      ByteBufCodecs.collection(FireworkExplosionCodec.CODEC, 256), Fireworks::explosions,
      Fireworks::new
  );
}
