package net.elytrium.limboapi.api.world.item.datacomponent.type;

import java.util.Collection;
import java.util.Collections;
import net.elytrium.limboapi.api.world.item.datacomponent.DataComponentType;
import net.elytrium.limboapi.api.world.item.datacomponent.type.data.HolderSet;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jspecify.annotations.NullMarked;

/**
 * @param showInTooltip Removed in version 1.21.5
 */
@NullMarked
public record AdventureModePredicate(Collection<BlockPredicate> predicates, boolean showInTooltip) {

  public AdventureModePredicate(Collection<BlockPredicate> predicates) {
    this(predicates, true);
  }

  /**
   * @param components Added in version 1.21.5
   */
  public record BlockPredicate(@Nullable HolderSet blocks, @Nullable Collection<PropertyMatcher> properties, @Nullable CompoundBinaryTag nbt, DataComponentMatchers components) {

    public BlockPredicate(@Nullable HolderSet blocks, @Nullable Collection<PropertyMatcher> properties, @Nullable CompoundBinaryTag nbt) {
      this(blocks, properties, nbt, DataComponentMatchers.ANY);
    }
  }

  public record PropertyMatcher(String name, ValueMatcher valueMatcher) {

  }

  public sealed interface ValueMatcher permits ValueMatcher.Exact, ValueMatcher.Ranged {

    record Exact(String value) implements ValueMatcher {

    }

    record Ranged(@Nullable String minValue, @Nullable String maxValue) implements ValueMatcher {

    }
  }

  public record DataComponentMatchers(Collection<DataComponentExactPredicate> exact, Collection<DataComponentPartialPredicate> partial) {

    public static final DataComponentMatchers ANY = new DataComponentMatchers(Collections.emptyList(), Collections.emptyList());
  }

  public sealed interface DataComponentExactPredicate permits DataComponentExactPredicate.Valued, DataComponentExactPredicate.NonValued { // TODO проверить как сделают в пейпере когда они это реализуют

    DataComponentType type();

    record Valued<T>(DataComponentType.Valued<T> type, T value) implements DataComponentExactPredicate {

    }

    record NonValued(DataComponentType.NonValued type) implements DataComponentExactPredicate {

    }
  }

  public record DataComponentPartialPredicate(int id, BinaryTag predicate) {

  }
}
