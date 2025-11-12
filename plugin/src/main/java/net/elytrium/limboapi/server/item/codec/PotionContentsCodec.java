package net.elytrium.limboapi.server.item.codec;

import com.velocitypowered.api.network.ProtocolVersion;
import net.elytrium.limboapi.api.world.item.datacomponent.type.PotionContents;
import net.elytrium.limboapi.protocol.codec.ByteBufCodecs;
import net.elytrium.limboapi.protocol.codec.StreamCodec;
import net.elytrium.limboapi.server.item.codec.data.MobEffectCodec;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface PotionContentsCodec {

  StreamCodec<PotionContents> CODEC = StreamCodec.composite(
      ByteBufCodecs.optional(ByteBufCodecs.VAR_INT), PotionContents::potion,
      ByteBufCodecs.optional(ByteBufCodecs.INT), PotionContents::customColor,
      MobEffectCodec.COLLECTION_CODEC, PotionContents::customEffects,
      StreamCodec.ge(ProtocolVersion.MINECRAFT_1_21_2, ByteBufCodecs.OPTIONAL_STRING_UTF8), PotionContents::customName,
      PotionContents::new
  );
}
