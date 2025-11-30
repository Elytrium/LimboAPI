package net.elytrium.limboapi.api.world.item.datacomponent.type;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record UseCooldown(float seconds, @Nullable String cooldownGroup) {

}
