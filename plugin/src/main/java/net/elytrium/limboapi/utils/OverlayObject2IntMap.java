/*
 * Copyright (C) 2021 - 2024 Elytrium
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.elytrium.limboapi.utils;

import com.google.common.collect.Streams;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import java.util.stream.Collectors;
import net.elytrium.limboapi.api.utils.OverlayMap;
import org.jetbrains.annotations.NotNull;

public class OverlayObject2IntMap<K> extends OverlayMap<K, Integer> implements Object2IntMap<K> {

  public OverlayObject2IntMap(Object2IntMap<K> parent, Object2IntMap<K> overlay) {
    super(parent, overlay);
    overlay.defaultReturnValue(parent.defaultReturnValue());
  }

  @Override
  public int getInt(Object key) {
    int value = ((Object2IntMap<K>) this.overlay).getInt(key);
    if (value == this.defaultReturnValue()) {
      value = ((Object2IntMap<K>) this.parent).getInt(key);
    }

    return value;
  }

  @Override
  public int put(K key, int value) {
    return ((Object2IntMap<K>) this.overlay).put(key, value);
  }

  @Override
  public void defaultReturnValue(int rv) {
    ((Object2IntMap<K>) this.overlay).defaultReturnValue(rv);
  }

  @Override
  public int defaultReturnValue() {
    return ((Object2IntMap<K>) this.overlay).defaultReturnValue();
  }

  @Override
  public ObjectSet<Object2IntMap.Entry<K>> object2IntEntrySet() {
    return new SetIsObjectSet<>(
        Streams
            .concat(((Object2IntMap<K>) this.parent).object2IntEntrySet().stream(), ((Object2IntMap<K>) this.overlay).object2IntEntrySet().stream())
            .collect(Collectors.toSet()));
  }

  @NotNull
  @Override
  public ObjectSet<K> keySet() {
    return new SetIsObjectSet<>(
        Streams
            .concat(((Object2IntMap<K>) this.parent).keySet().stream(), ((Object2IntMap<K>) this.overlay).keySet().stream())
            .collect(Collectors.toSet()));
  }

  @Override
  public IntCollection values() {
    return Streams
        .concat(((Object2IntMap<K>) this.parent).values().intStream(), ((Object2IntMap<K>) this.overlay).values().intStream())
        .boxed()
        .collect(Collectors.toCollection(IntArrayList::new));
  }

  @Override
  public boolean containsValue(int value) {
    return false;
  }
}
