package net.elytrium.limboapi.server.item.codec;

import net.elytrium.limboapi.api.world.item.datacomponent.type.Consumable;
import net.elytrium.limboapi.protocol.codec.ByteBufCodecs;
import net.elytrium.limboapi.protocol.codec.StreamCodec;
import net.elytrium.limboapi.server.item.codec.data.ConsumeEffectCodec;
import net.elytrium.limboapi.server.item.codec.data.SoundEventCodec;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface ConsumableCodec {

  StreamCodec<Consumable> CODEC = StreamCodec.composite(
      ByteBufCodecs.FLOAT, Consumable::consumeSeconds,
      ByteBufCodecs.VAR_INT, Consumable::animation,
      SoundEventCodec.HOLDER_CODEC, Consumable::sound,
      ByteBufCodecs.BOOL, Consumable::hasConsumeParticles,
      ConsumeEffectCodec.COLLECTION_CODEC, Consumable::onConsumeEffects,
      Consumable::new
  );
}
