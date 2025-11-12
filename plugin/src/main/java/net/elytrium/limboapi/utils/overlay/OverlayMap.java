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

package net.elytrium.limboapi.utils.overlay;

import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class OverlayMap<K, V> implements Map<K, V> {

  protected final Map<K, V> parent;
  protected final Map<K, V> overlay;

  protected boolean override = false;

  public OverlayMap(Map<K, V> parent, Map<K, V> overlay) {
    this.parent = parent;
    this.overlay = overlay;
  }

  @Override
  public int size() {
    if (this.override) {
      return this.overlay.size();
    }

    return this.overlay.size() + this.parent.size();
  }

  @Override
  public boolean isEmpty() {
    if (this.override) {
      return this.overlay.isEmpty();
    }

    return this.overlay.isEmpty() && this.parent.isEmpty();
  }

  @Override
  public boolean containsKey(Object o) {
    if (this.override) {
      return this.overlay.containsKey(o);
    }

    return this.overlay.containsKey(o) || this.parent.containsKey(o);
  }

  @Override
  public boolean containsValue(Object o) {
    if (this.override) {
      return this.overlay.containsValue(o);
    }

    return this.overlay.containsValue(o) || this.parent.containsValue(o);
  }

  @Override
  public V get(Object o) {
    V value = this.overlay.get(o);
    if (value != null) {
      return value;
    }

    return this.override ? null : this.parent.get(o);
  }

  @Override
  public V put(K k, V v) {
    return this.overlay.put(k, v);
  }

  @Override
  public V remove(Object o) {
    return this.overlay.remove(o);
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> map) {
    this.overlay.putAll(map);
  }

  @Override
  public void clear() {
    this.overlay.clear();
  }

  @Override
  public Set<K> keySet() {
    if (this.override) {
      return this.overlay.keySet();
    }

    return Sets.union(this.overlay.keySet(), this.parent.keySet());
  }

  @Override
  public Collection<V> values() {
    if (this.override) {
      return this.overlay.values();
    }

    return Stream.concat(this.overlay.values().stream(), this.parent.values().stream()).collect(Collectors.toList());
  }

  @Override
  public Set<Entry<K, V>> entrySet() {
    if (this.override) {
      return this.overlay.entrySet();
    }

    return Sets.union(this.overlay.entrySet(), this.parent.entrySet());
  }

  public boolean isOverride() {
    return this.override;
  }

  public void setOverride(boolean override) {
    this.override = override;
  }
}
