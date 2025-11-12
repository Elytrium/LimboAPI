package net.elytrium.limboapi.api.world.item.datacomponent.type.data;

import org.jspecify.annotations.NullMarked;

@NullMarked
public sealed interface HolderSet permits HolderSet.Named, HolderSet.Direct {

  record Named(String key) implements HolderSet {

  }

  record Direct(int[] contents) implements HolderSet {

  }
}
