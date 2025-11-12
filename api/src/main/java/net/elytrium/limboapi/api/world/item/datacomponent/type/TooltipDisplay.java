package net.elytrium.limboapi.api.world.item.datacomponent.type;

import java.util.Set;
import net.elytrium.limboapi.api.world.item.datacomponent.DataComponentType;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record TooltipDisplay(boolean hideTooltip, Set/*TODO SequencedSet*/<DataComponentType> hiddenComponents) {

}
