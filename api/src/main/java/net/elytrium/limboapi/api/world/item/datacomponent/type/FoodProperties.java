package net.elytrium.limboapi.api.world.item.datacomponent.type;

import java.util.Collection;
import java.util.Collections;
import net.elytrium.limboapi.api.protocol.data.ItemStack;
import net.elytrium.limboapi.api.world.item.datacomponent.type.data.MobEffect;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jspecify.annotations.NullMarked;

/**
 * @param eatSeconds      Removed in version 1.21.2
 * @param usingConvertsTo Added in version 1.21; removed in version 1.21.2
 * @param effects         Removed in version 1.21.2
 */
@NullMarked
public record FoodProperties(@NonNegative int nutrition, float saturation, boolean canAlwaysEat, @NonNegative float eatSeconds, @Nullable ItemStack usingConvertsTo, Collection<PossibleEffect> effects) {

  public FoodProperties(@NonNegative int nutrition, float saturation, boolean canAlwaysEat, @NonNegative float eatSeconds, Collection<PossibleEffect> effects) {
    this(nutrition, saturation, canAlwaysEat, eatSeconds, null, effects);
  }

  public FoodProperties(@NonNegative int nutrition, float saturation, boolean canAlwaysEat) {
    this(nutrition, saturation, canAlwaysEat, 1.6F, null, Collections.emptyList());
  }

  public record PossibleEffect(MobEffect effect, float probability) {

  }
}
