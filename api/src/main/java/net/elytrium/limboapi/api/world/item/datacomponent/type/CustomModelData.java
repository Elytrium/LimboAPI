package net.elytrium.limboapi.api.world.item.datacomponent.type;

import java.util.Collections;
import java.util.List;
import org.jspecify.annotations.NullMarked;

/**
 * @param floats  For versions prior to 1.21.4, only a single {@code Integer} is expected.
 *                Since version 1.21.4, any number of {@code Float} values can be provided
 * @param flags   Added in version 1.21.4
 * @param strings Added in version 1.21.4
 * @param colors  Added in version 1.21.4
 */
@NullMarked
public record CustomModelData(List<Number> floats, List<Boolean> flags, List<String> strings, List<Integer> colors) {

  public CustomModelData(int value) {
    this(Collections.singletonList(value), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
  }
}
