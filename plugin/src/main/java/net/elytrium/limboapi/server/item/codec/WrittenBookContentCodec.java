package net.elytrium.limboapi.server.item.codec;

import net.elytrium.limboapi.api.world.item.datacomponent.type.WrittenBookContent;
import net.elytrium.limboapi.protocol.codec.ByteBufCodecs;
import net.elytrium.limboapi.protocol.codec.StreamCodec;
import net.elytrium.limboapi.server.item.codec.data.ComponentHolderCodec;
import net.elytrium.limboapi.server.item.codec.data.FilterableCodec;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface WrittenBookContentCodec {

  StreamCodec<WrittenBookContent> CODEC = StreamCodec.composite(
      FilterableCodec.codec(ByteBufCodecs.stringUtf8(32)), WrittenBookContent::title,
      ByteBufCodecs.STRING_UTF8, WrittenBookContent::author,
      ByteBufCodecs.VAR_INT, WrittenBookContent::generation,
      ByteBufCodecs.collection(FilterableCodec.codec(ComponentHolderCodec.CODEC)), WrittenBookContent::pages,
      ByteBufCodecs.BOOL, WrittenBookContent::resolved,
      WrittenBookContent::new
  );
}
