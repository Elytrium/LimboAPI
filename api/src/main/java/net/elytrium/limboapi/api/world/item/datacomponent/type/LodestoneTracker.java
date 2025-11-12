package net.elytrium.limboapi.api.world.item.datacomponent.type;

import net.elytrium.limboapi.api.protocol.data.GlobalPos;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record LodestoneTracker(@Nullable GlobalPos target, boolean tracked) {

}
