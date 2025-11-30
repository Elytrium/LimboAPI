package net.elytrium.limboapi.server.item.codec;

import net.elytrium.limboapi.api.world.item.datacomponent.type.SuspiciousStewEffect;
import net.elytrium.limboapi.protocol.codec.ByteBufCodecs;
import net.elytrium.limboapi.protocol.codec.StreamCodec;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface SuspiciousStewEffectCodec {

  StreamCodec<SuspiciousStewEffect> CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT, SuspiciousStewEffect::effect,
      ByteBufCodecs.VAR_INT, SuspiciousStewEffect::duration,
      SuspiciousStewEffect::new
  );
}
