package net.elytrium.limboapi.api.world.item.datacomponent.type;

import net.elytrium.limboapi.api.protocol.data.ComponentHolder;
import net.elytrium.limboapi.api.world.item.datacomponent.type.data.Holder;
import net.elytrium.limboapi.api.world.item.datacomponent.type.data.SoundEvent;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.NullMarked;

/**
 * @param useDuration For versions prior to 1.21.2, an {@code int} value representing ticks is expected.
 *                    Since version 1.21.2, a {@code float} value representing seconds is expected
 * @param description Added in version 1.21.2
 */
@NullMarked
public record Instrument(Holder<SoundEvent> soundEvent, Number useDuration, float range, ComponentHolder description) implements Holder.Direct<Instrument> {

  public Instrument(Holder<SoundEvent> soundEvent, int useDuration, float range) {
    this(soundEvent, useDuration / 20.0F, range, new ComponentHolder(Component.empty()));
  }
}
