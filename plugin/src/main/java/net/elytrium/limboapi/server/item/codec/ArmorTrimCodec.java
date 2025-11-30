package net.elytrium.limboapi.server.item.codec;

import com.velocitypowered.api.network.ProtocolVersion;
import net.elytrium.limboapi.api.world.item.datacomponent.type.ArmorTrim;
import net.elytrium.limboapi.protocol.codec.ByteBufCodecs;
import net.elytrium.limboapi.protocol.codec.StreamCodec;
import net.elytrium.limboapi.server.item.codec.data.HolderCodec;
import net.elytrium.limboapi.server.item.codec.data.TrimMaterialCodec;
import net.elytrium.limboapi.server.item.codec.data.TrimPatternCodec;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface ArmorTrimCodec {

  StreamCodec<ArmorTrim> CODEC = StreamCodec.composite(
      TrimMaterialCodec.HOLDER_CODEC, ArmorTrim::material,
      HolderCodec.codec(TrimPatternCodec.CODEC), ArmorTrim::pattern,
      StreamCodec.lt(ProtocolVersion.MINECRAFT_1_21_5, ByteBufCodecs.BOOL, true), ArmorTrim::showInTooltip,
      ArmorTrim::new
  );
}
