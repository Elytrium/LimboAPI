package net.elytrium.limboapi.api.world.item.datacomponent.type.data;

import java.util.Map;
import net.elytrium.limboapi.api.protocol.data.ComponentHolder;
import org.jspecify.annotations.NullMarked;

/**
 * @param ingredient     Removed in version 1.21.5
 * @param itemModelIndex Removed in version 1.21.4
 * @param overrides      For versions prior to 1.21.2, the map key is an {@code Integer}.
 *                       Since version 1.21.2, the map key is a {@code String}
 */
@NullMarked
public record TrimMaterial(String assetName, int/*Holder<Item>*/ ingredient, float itemModelIndex, Map<Object, String> overrides, ComponentHolder description) implements Holder.Direct<TrimMaterial> {

  public TrimMaterial(String assetName, int ingredient, Map<Object, String> overrides, ComponentHolder description) {
    this(assetName, ingredient, 0.0F, overrides, description);
  }

  public TrimMaterial(String assetName, Map<Object, String> overrides, ComponentHolder description) {
    this(assetName, 0, 0.0F, overrides, description);
  }
}
