package net.elytrium.limboapi.api.world.item.datacomponent.type.data;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record Filterable<T>(T raw, @Nullable T filtered) {

}
