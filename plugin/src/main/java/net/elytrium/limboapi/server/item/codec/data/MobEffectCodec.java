package net.elytrium.limboapi.server.item.codec.data;

import java.util.Collection;
import net.elytrium.limboapi.api.world.item.datacomponent.type.data.MobEffect;
import net.elytrium.limboapi.protocol.codec.ByteBufCodecs;
import net.elytrium.limboapi.protocol.codec.StreamCodec;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface MobEffectCodec {

  StreamCodec<MobEffect> CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT, MobEffect::effect,
      DetailsCodec.CODEC, MobEffect::details,
      MobEffect::new
  );
  StreamCodec<Collection<MobEffect>> COLLECTION_CODEC = ByteBufCodecs.collection(MobEffectCodec.CODEC);

  interface DetailsCodec {

    StreamCodec<MobEffect.Details> CODEC = StreamCodec.recursive(self -> StreamCodec.composite(
        ByteBufCodecs.VAR_INT, MobEffect.Details::amplifier,
        ByteBufCodecs.VAR_INT, MobEffect.Details::duration,
        ByteBufCodecs.BOOL, MobEffect.Details::ambient,
        ByteBufCodecs.BOOL, MobEffect.Details::visible,
        ByteBufCodecs.BOOL, MobEffect.Details::showIcon,
        ByteBufCodecs.<MobEffect.@Nullable Details>optional(self), MobEffect.Details::hiddenEffect,
        MobEffect.Details::new
    ));
  }
}
