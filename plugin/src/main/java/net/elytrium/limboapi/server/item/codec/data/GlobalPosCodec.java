package net.elytrium.limboapi.server.item.codec.data;

import net.elytrium.limboapi.api.protocol.data.GlobalPos;
import net.elytrium.limboapi.protocol.codec.ByteBufCodecs;
import net.elytrium.limboapi.protocol.codec.StreamCodec;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface GlobalPosCodec {

  StreamCodec<GlobalPos> CODEC = StreamCodec.composite(
      ByteBufCodecs.STRING_UTF8, GlobalPos::dimension,
      BlockPosCodec.CODEC, GlobalPos::blockPos,
      GlobalPos::new
  );
  StreamCodec<@Nullable GlobalPos> OPTIONAL_CODEC = ByteBufCodecs.optional(GlobalPosCodec.CODEC);
}
