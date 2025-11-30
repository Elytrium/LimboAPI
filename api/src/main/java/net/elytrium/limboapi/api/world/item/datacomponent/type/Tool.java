package net.elytrium.limboapi.api.world.item.datacomponent.type;

import java.util.Collection;
import net.elytrium.limboapi.api.world.item.datacomponent.type.data.HolderSet;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jspecify.annotations.NullMarked;

/**
 * @param canDestroyBlocksInCreative Added in version 1.21.5
 */
@NullMarked
public record Tool(Collection<Rule> rules, float defaultMiningSpeed, @NonNegative int damagePerBlock, boolean canDestroyBlocksInCreative) {

  public Tool(Collection<Rule> rules, float defaultMiningSpeed, @NonNegative int damagePerBlock) {
    this(rules, defaultMiningSpeed, damagePerBlock, true);
  }

  public record Rule(HolderSet blocks, @Nullable Float speed, @Nullable Boolean correctForDrops) {

  }
}
