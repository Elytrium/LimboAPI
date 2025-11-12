package net.elytrium.limboapi.api.world.item.datacomponent.type.data;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record SoundEvent(String soundId, @Nullable Float range) implements Holder.Direct<SoundEvent> {

}
