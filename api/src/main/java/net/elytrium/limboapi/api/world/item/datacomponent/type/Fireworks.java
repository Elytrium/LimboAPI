package net.elytrium.limboapi.api.world.item.datacomponent.type;

import java.util.Collection;
import org.checkerframework.common.value.qual.IntRange;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record Fireworks(@IntRange(from = 0, to = 255) int flightDuration, Collection<FireworkExplosion> explosions) {

}
