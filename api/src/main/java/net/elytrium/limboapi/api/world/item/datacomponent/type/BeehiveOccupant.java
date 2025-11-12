package net.elytrium.limboapi.api.world.item.datacomponent.type;

import net.elytrium.limboapi.api.world.item.datacomponent.type.data.TypedEntityData;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record BeehiveOccupant(TypedEntityData entityData, int ticksInHive, int minTicksInHive)  {

  public BeehiveOccupant(CompoundBinaryTag tag, int ticksInHive, int minTicksInHive) {
    this(new TypedEntityData(11/*bee entity id as of 1.21.9*/, tag), ticksInHive, minTicksInHive);
  }
}
