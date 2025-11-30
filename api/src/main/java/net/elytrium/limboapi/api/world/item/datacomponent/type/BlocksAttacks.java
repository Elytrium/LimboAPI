package net.elytrium.limboapi.api.world.item.datacomponent.type;

import java.util.Collection;
import net.elytrium.limboapi.api.world.item.datacomponent.type.data.Holder;
import net.elytrium.limboapi.api.world.item.datacomponent.type.data.HolderSet;
import net.elytrium.limboapi.api.world.item.datacomponent.type.data.SoundEvent;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record BlocksAttacks(float blockDelaySeconds, float disableCooldownScale, Collection<DamageReduction> damageReductions, BlocksAttacks.ItemDamageFunction itemDamage,
    @Nullable String bypassedBy, @Nullable Holder<SoundEvent> blockSound, @Nullable Holder<SoundEvent> disableSound) {

  public record DamageReduction(float horizontalBlockingAngle, @Nullable HolderSet type, float base, float factor) {

  }

  public record ItemDamageFunction(float threshold, float base, float factor) {

  }
}
