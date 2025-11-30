package net.elytrium.limboapi.server.item.codec.data;

import net.elytrium.limboapi.api.world.item.datacomponent.type.data.Filterable;
import net.elytrium.limboapi.protocol.codec.ByteBufCodecs;
import net.elytrium.limboapi.protocol.codec.StreamCodec;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface FilterableCodec {

  static <T> StreamCodec<Filterable<T>> codec(StreamCodec<T> codec) {
    return FilterableCodec.codec(codec, ByteBufCodecs.optional(codec));
  }

  static <T> StreamCodec<Filterable<T>> codec(StreamCodec<T> codec, StreamCodec<@Nullable T> optionalCodec) {
    return StreamCodec.composite(
        codec, Filterable::raw,
        optionalCodec, Filterable::filtered,
        Filterable::new
    );
  }
}
