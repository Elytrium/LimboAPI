package net.elytrium.limboapi.api.world.item.datacomponent.type;

import java.util.Collection;
import java.util.UUID;
import net.elytrium.limboapi.api.protocol.data.ComponentHolder;
import org.jspecify.annotations.NullMarked;

/**
 * @param showInTooltip Removed in version 1.21.5
 */
@NullMarked
public record AttributeModifiers(Collection<Entry> modifiers, boolean showInTooltip) {

  public AttributeModifiers(Collection<Entry> modifiers) {
    this(modifiers, true);
  }

  /**
   * @param display Added in version 1.21.6
   */
  public record Entry(int attribute, AttributeModifier modifier, int/*enum EquipmentSlotGroup*/ slot, Display display) {

    public Entry(int attribute, AttributeModifier modifier, int slot) {
      this(attribute, modifier, slot, Display.Default.INSTANCE);
    }
  }

  /**
   * @param uuid Removed in version 1.21
   */
  public record AttributeModifier(UUID uuid, String name, double amount, int/*enum AttributeModifier.Operation*/ operation) {

  }

  public sealed interface Display permits Display.Default, Display.Hidden, Display.OverrideText {

    record Default() implements Display {

      public static final Default INSTANCE = new Default();
    }

    record Hidden() implements Display {

      public static final Hidden INSTANCE = new Hidden();
    }

    record OverrideText(ComponentHolder component) implements Display {

    }
  }
}
