package net.elytrium.limboapi.api.world.item.datacomponent.type.data;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.jspecify.annotations.NullMarked;

/**
 * @param type Added in version 1.21.9
 */
@NullMarked
public record TypedEntityData(int type, CompoundBinaryTag tag) {

  public TypedEntityData(CompoundBinaryTag tag) {
    this(0, tag);
  }
}
