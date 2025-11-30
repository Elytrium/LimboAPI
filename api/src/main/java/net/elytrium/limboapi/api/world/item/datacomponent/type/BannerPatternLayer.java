package net.elytrium.limboapi.api.world.item.datacomponent.type;

import net.elytrium.limboapi.api.world.item.datacomponent.type.data.Holder;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record BannerPatternLayer(Holder<BannerPattern> pattern, int/*enum DyeColor*/ color) {

  public record BannerPattern(String assetId, String translationKey) implements Holder.Direct<BannerPattern> {

  }
}
