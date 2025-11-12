package net.elytrium.limboapi.api.world.item.datacomponent.type;

import java.util.Collection;
import net.elytrium.limboapi.api.world.item.datacomponent.type.data.MobEffect;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jspecify.annotations.NullMarked;

/**
 * @param customName Added in version 1.21.2
 */
@NullMarked
public record PotionContents(@Nullable Integer potion, @Nullable Integer customColor, Collection<MobEffect> customEffects, @Nullable String customName) {

  public PotionContents(@Nullable Integer potion, @Nullable Integer customColor, Collection<MobEffect> customEffects) {
    this(potion, customColor, customEffects, null);
  }
}
