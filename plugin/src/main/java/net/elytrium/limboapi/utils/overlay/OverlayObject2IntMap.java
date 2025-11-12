/*
 * Copyright (C) 2021 - 2025 Elytrium
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

package net.elytrium.limboapi.utils.overlay;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class OverlayObject2IntMap<K> extends OverlayMap<K, Integer> implements Object2IntMap<K> {

  public OverlayObject2IntMap(Object2IntMap<K> parent, Object2IntMap<K> overlay) {
    super(parent, overlay);
    overlay.defaultReturnValue(parent.defaultReturnValue());
  }

  @Override
  public Integer get(Object o) {
    int value = this.getInt(o);
    return value == this.defaultReturnValue() ? null : value;
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
  public ObjectSet<K> keySet() {
    if (this.override) {
      return ((Object2IntMap<K>) this.overlay).keySet();
    }

    return Stream
        .concat(((Object2IntMap<K>) this.overlay).keySet().stream(), ((Object2IntMap<K>) this.parent).keySet().stream())
        .collect(Collectors.toCollection(ObjectArraySet::new));
  }

  @Override
  public IntCollection values() {
    if (this.override) {
      return ((Object2IntMap<K>) this.overlay).values();
    }

    return IntStream
        .concat(((Object2IntMap<K>) this.overlay).values().intStream(), ((Object2IntMap<K>) this.parent).values().intStream())
        .collect(IntArrayList::new, IntArrayList::add, IntArrayList::addAll);
  }

  @Override
  @SuppressWarnings("deprecation")
  public ObjectSet<Map.Entry<K, Integer>> entrySet() {
    if (this.override) {
      return ((Object2IntMap<K>) this.overlay).entrySet();
    }

    return Stream.concat(this.overlay.entrySet().stream(), this.parent.entrySet().stream()).collect(Collectors.toCollection(ObjectArraySet::new));
  }

  @Override
  public ObjectSet<Object2IntMap.Entry<K>> object2IntEntrySet() {
    if (this.override) {
      return ((Object2IntMap<K>) this.overlay).object2IntEntrySet();
    }

    return Stream
        .concat(((Object2IntMap<K>) this.overlay).object2IntEntrySet().stream(), ((Object2IntMap<K>) this.parent).object2IntEntrySet().stream())
        .collect(Collectors.toCollection(ObjectArraySet::new));
  }

  @Override
  public boolean containsValue(int value) {
    if (this.override) {
      return ((Object2IntMap<K>) this.overlay).containsValue(value);
    }

    return ((Object2IntMap<K>) this.parent).containsValue(value) || ((Object2IntMap<K>) this.overlay).containsValue(value);
  }
}
