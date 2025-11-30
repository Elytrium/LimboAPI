package net.elytrium.limboapi.api.world.item.datacomponent.type;

import java.util.Collection;
import net.elytrium.limboapi.api.world.item.datacomponent.type.data.ConsumeEffect;
import net.elytrium.limboapi.api.world.item.datacomponent.type.data.Holder;
import net.elytrium.limboapi.api.world.item.datacomponent.type.data.SoundEvent;
import org.checkerframework.checker.index.qual.NonNegative;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record Consumable(@NonNegative float consumeSeconds, int/*enum ItemUseAnimation*/ animation, Holder<SoundEvent> sound, boolean hasConsumeParticles, Collection<ConsumeEffect> onConsumeEffects) {

}
