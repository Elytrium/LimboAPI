package net.elytrium.limboapi.api.world.item.datacomponent.type.data;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record MobEffect(int effect, Details details) {

  public record Details(int amplifier, int duration, boolean ambient, boolean visible, boolean showIcon, @Nullable Details hiddenEffect) {

  }
}
