package net.elytrium.limboapi.server.item.codec;

import net.elytrium.limboapi.api.world.item.datacomponent.type.UseCooldown;
import net.elytrium.limboapi.protocol.codec.ByteBufCodecs;
import net.elytrium.limboapi.protocol.codec.StreamCodec;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface UseCooldownCodec {

  StreamCodec<UseCooldown> CODEC = StreamCodec.composite(
      ByteBufCodecs.FLOAT, UseCooldown::seconds,
      ByteBufCodecs.OPTIONAL_STRING_UTF8, UseCooldown::cooldownGroup,
      UseCooldown::new
  );
}
