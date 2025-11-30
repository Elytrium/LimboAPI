package net.elytrium.limboapi.api.world.item.datacomponent.type;

import net.elytrium.limboapi.api.protocol.data.ComponentHolder;
import net.elytrium.limboapi.api.world.item.datacomponent.type.data.Either;
import net.elytrium.limboapi.api.world.item.datacomponent.type.data.Holder;
import net.elytrium.limboapi.api.world.item.datacomponent.type.data.SoundEvent;
import org.jspecify.annotations.NullMarked;

/**
 * @param showInTooltip Removed in version 1.21.5
 */
@NullMarked
public record JukeboxPlayable(Either<Holder<JukeboxSong>, String> song, boolean showInTooltip) {

  public JukeboxPlayable(Either<Holder<JukeboxSong>, String> song) {
    this(song, true);
  }

  public record JukeboxSong(Holder<SoundEvent> soundEvent, ComponentHolder description, float lengthInSeconds, int comparatorOutput) implements Holder.Direct<JukeboxSong> {

  }
}
