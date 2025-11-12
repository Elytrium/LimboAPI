package net.elytrium.limboapi.server.item.codec.data;

import net.elytrium.limboapi.api.world.item.datacomponent.type.data.Holder;
import net.elytrium.limboapi.api.world.item.datacomponent.type.data.SoundEvent;
import net.elytrium.limboapi.protocol.codec.ByteBufCodecs;
import net.elytrium.limboapi.protocol.codec.StreamCodec;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public interface SoundEventCodec {

  StreamCodec<SoundEvent> CODEC = StreamCodec.composite(
      ByteBufCodecs.STRING_UTF8, SoundEvent::soundId,
      ByteBufCodecs.FLOAT, SoundEvent::range,
      SoundEvent::new
  );
  StreamCodec<Holder<SoundEvent>> HOLDER_CODEC = HolderCodec.codec(SoundEventCodec.CODEC);
  StreamCodec<@Nullable Holder<SoundEvent>> OPTIONAL_HOLDER_CODEC = ByteBufCodecs.optional(SoundEventCodec.HOLDER_CODEC);
}
