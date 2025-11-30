package net.elytrium.limboapi.api.world.item.datacomponent.type.data;

import org.jspecify.annotations.NullMarked;

@NullMarked
@SuppressWarnings("unused")
public sealed interface Holder<T extends Holder.Direct<T>> permits Holder.Direct, Holder.Reference {

  static <T extends Holder.Direct<T>> Holder.Reference<T> ref(int id) {
    return new Reference<>(id);
  }

  non-sealed interface Direct<T extends Direct<T>> extends Holder<T> {

  }

  record Reference<T extends Holder.Direct<T>>(int id) implements Holder<T> {

  }
}
