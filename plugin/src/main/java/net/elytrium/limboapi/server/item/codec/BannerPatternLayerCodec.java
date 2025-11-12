package net.elytrium.limboapi.server.item.codec;

import net.elytrium.limboapi.api.world.item.datacomponent.type.BannerPatternLayer;
import net.elytrium.limboapi.protocol.codec.ByteBufCodecs;
import net.elytrium.limboapi.protocol.codec.StreamCodec;
import net.elytrium.limboapi.server.item.codec.data.HolderCodec;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface BannerPatternLayerCodec {

  StreamCodec<BannerPatternLayer> CODEC = StreamCodec.composite(
      HolderCodec.codec(BannerPatternCodec.CODEC), BannerPatternLayer::pattern,
      ByteBufCodecs.VAR_INT, BannerPatternLayer::color,
      BannerPatternLayer::new
  );

  interface BannerPatternCodec {

    StreamCodec<BannerPatternLayer.BannerPattern> CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, BannerPatternLayer.BannerPattern::assetId,
        ByteBufCodecs.STRING_UTF8, BannerPatternLayer.BannerPattern::translationKey,
        BannerPatternLayer.BannerPattern::new
    );
  }
}
