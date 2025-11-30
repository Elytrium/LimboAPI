package net.elytrium.limboapi.api.world.item.datacomponent.type;

import org.jspecify.annotations.NullMarked;

/**
 * @param showInTooltip Removed in version 1.21.5
 */
@NullMarked
public record DyedColor(int rgb, boolean showInTooltip) {

  public DyedColor(int rgb) {
    this(rgb, true);
  }
}
