package net.elytrium.limboapi.api.world.item.datacomponent.type;

import net.elytrium.limboapi.api.world.item.datacomponent.type.data.Holder;
import net.elytrium.limboapi.api.world.item.datacomponent.type.data.HolderSet;
import net.elytrium.limboapi.api.world.item.datacomponent.type.data.SoundEvent;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jspecify.annotations.NullMarked;

/**
 * @param equipOnInteract Added in version 1.21.5
 * @param canBeSheared    Added in version 1.21.6
 * @param shearingSound   Added in version 1.21.6
 */
@NullMarked
public record Equippable(int/*enum EquipmentSlot*/ slot, Holder<SoundEvent> equipSound, @Nullable String assetId, @Nullable String cameraOverlay, @Nullable HolderSet allowedEntities,
    boolean dispensable, boolean swappable, boolean damageOnHurt, boolean equipOnInteract, boolean canBeSheared, Holder<SoundEvent> shearingSound) {

  public Equippable(int/*enum EquipmentSlot*/ slot, Holder<SoundEvent> equipSound, @Nullable String assetId, @Nullable String cameraOverlay, @Nullable HolderSet allowedEntities,
      boolean dispensable, boolean swappable, boolean damageOnHurt) {
    this(slot, equipSound, assetId, cameraOverlay, allowedEntities, dispensable, swappable, damageOnHurt, true);
  }

  public Equippable(int/*enum EquipmentSlot*/ slot, Holder<SoundEvent> equipSound, @Nullable String assetId, @Nullable String cameraOverlay, @Nullable HolderSet allowedEntities,
      boolean dispensable, boolean swappable, boolean damageOnHurt, boolean equipOnInteract) {
    this(slot, equipSound, assetId, cameraOverlay, allowedEntities, dispensable, swappable, damageOnHurt, equipOnInteract, false, Holder.ref(0));
  }
}
