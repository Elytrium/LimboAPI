package net.elytrium.limboapi.api.world.item.datacomponent.type;

import net.elytrium.limboapi.api.world.item.datacomponent.type.data.TrimMaterial;
import net.elytrium.limboapi.api.world.item.datacomponent.type.data.TrimPattern;
import net.elytrium.limboapi.api.world.item.datacomponent.type.data.Holder;
import org.jspecify.annotations.NullMarked;

/**
 * @param showInTooltip Removed in version 1.21.5
 */
@NullMarked
public record ArmorTrim(Holder<TrimMaterial> material, Holder<TrimPattern> pattern, boolean showInTooltip) {

  public ArmorTrim(Holder<TrimMaterial> material, Holder<TrimPattern> pattern) {
    this(material, pattern, true);
  }
}
