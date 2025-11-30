package net.elytrium.limboapi.api.world.item.datacomponent.type.data;

import net.elytrium.limboapi.api.protocol.data.ComponentHolder;
import org.jspecify.annotations.NullMarked;

/**
 * @param templateItem Removed in version 1.21.5
 */
@NullMarked
public record TrimPattern(String assetId, int/*Holder<Item>*/ templateItem, ComponentHolder description, boolean decal) implements Holder.Direct<TrimPattern> {

  public TrimPattern(String assetId, ComponentHolder description, boolean decal) {
    this(assetId, 0, description, decal);
  }
}
