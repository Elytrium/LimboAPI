package net.elytrium.limboapi.api.world.item.datacomponent.type;

import java.util.Map;
import org.checkerframework.common.value.qual.IntRange;
import org.jspecify.annotations.NullMarked;

/**
 * @param showInTooltip Removed in version 1.21.5
 */
@NullMarked
public record Enchantments(Map<Integer, @IntRange(from = 1, to = 255) Integer> enchantments, boolean showInTooltip) {

  public Enchantments(Map<Integer, @IntRange(from = 1, to = 255) Integer> enchantments) {
    this(enchantments, true);
  }
}
