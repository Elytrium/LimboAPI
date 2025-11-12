package net.elytrium.limboapi.server.item.codec;

import com.velocitypowered.api.network.ProtocolVersion;
import net.elytrium.limboapi.api.world.item.datacomponent.type.PaintingVariant;
import net.elytrium.limboapi.api.world.item.datacomponent.type.data.Holder;
import net.elytrium.limboapi.protocol.codec.ByteBufCodecs;
import net.elytrium.limboapi.protocol.codec.StreamCodec;
import net.elytrium.limboapi.server.item.codec.data.ComponentHolderCodec;
import net.elytrium.limboapi.server.item.codec.data.HolderCodec;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface PaintingVariantCodec {

  StreamCodec<PaintingVariant> CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT, PaintingVariant::width,
      ByteBufCodecs.VAR_INT, PaintingVariant::height,
      ByteBufCodecs.STRING_UTF8, PaintingVariant::assetId,
      StreamCodec.ge(ProtocolVersion.MINECRAFT_1_21_2, ComponentHolderCodec.OPTIONAL_CODEC), PaintingVariant::title,
      StreamCodec.ge(ProtocolVersion.MINECRAFT_1_21_2, ComponentHolderCodec.OPTIONAL_CODEC), PaintingVariant::author,
      PaintingVariant::new
  );
  StreamCodec<Holder<PaintingVariant>> HOLDER_CODEC = HolderCodec.codec(PaintingVariantCodec.CODEC);
}
