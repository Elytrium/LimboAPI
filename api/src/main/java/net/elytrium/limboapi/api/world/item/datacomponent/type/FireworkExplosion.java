package net.elytrium.limboapi.api.world.item.datacomponent.type;

import java.util.Collection;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record FireworkExplosion(int/*enum FireworkExplosion.Shape*/ shape, Collection<Integer> colors, Collection<Integer> fadeColors, boolean hasTrail, boolean hasTwinkle) {

}
