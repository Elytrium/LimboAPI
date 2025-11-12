package net.elytrium.limboapi.api.world.item.datacomponent.type;

import org.jspecify.annotations.NullMarked;

/**
 * @param showInTooltip Removed in version 1.21.5
 */
@NullMarked
public record Unbreakable(boolean showInTooltip) {

  public static final Unbreakable TRUE = new Unbreakable(true);
  public static final Unbreakable FALSE = new Unbreakable(false);

  public Unbreakable() {
    this(true);
  }
}
