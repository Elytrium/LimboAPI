package net.elytrium.limboapi.api.world.item.datacomponent.type;

import java.util.Collection;
import net.elytrium.limboapi.api.protocol.data.ComponentHolder;
import net.elytrium.limboapi.api.world.item.datacomponent.type.data.Filterable;
import org.checkerframework.common.value.qual.IntRange;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record WrittenBookContent(Filterable<String> title, String author, @IntRange(from = 0, to = 3) int generation, Collection<Filterable<ComponentHolder>> pages, boolean resolved) {

}
