package net.elytrium.limboapi.server.item.codec.data;

import com.velocitypowered.api.network.ProtocolVersion;
import net.elytrium.limboapi.api.world.item.datacomponent.type.data.TrimPattern;
import net.elytrium.limboapi.protocol.codec.ByteBufCodecs;
import net.elytrium.limboapi.protocol.codec.StreamCodec;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface TrimPatternCodec {

  StreamCodec<TrimPattern> CODEC = StreamCodec.composite(
      ByteBufCodecs.STRING_UTF8, TrimPattern::assetId,
      StreamCodec.lt(ProtocolVersion.MINECRAFT_1_21_5, ByteBufCodecs.VAR_INT, 0), TrimPattern::templateItem,
      ComponentHolderCodec.CODEC, TrimPattern::description,
      ByteBufCodecs.BOOL, TrimPattern::decal,
      TrimPattern::new
  );
}
