package net.elytrium.limboapi.api.world.item.datacomponent.type;

import net.elytrium.limboapi.api.protocol.data.ComponentHolder;
import net.elytrium.limboapi.api.world.item.datacomponent.type.data.Holder;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.value.qual.IntRange;
import org.jspecify.annotations.NullMarked;

/**
 * @param title  Added in version 1.21.2
 * @param author Added in version 1.21.2
 */
@NullMarked
public record PaintingVariant(@IntRange(from = 1, to = 16) int width, @IntRange(from = 1, to = 16) int height, String assetId,
    @Nullable ComponentHolder title, @Nullable ComponentHolder author) implements Holder.Direct<PaintingVariant> {

  public PaintingVariant(@IntRange(from = 1, to = 16) int width, @IntRange(from = 1, to = 16) int height, String assetId) {
    this(width, height, assetId, null, null);
  }
}
