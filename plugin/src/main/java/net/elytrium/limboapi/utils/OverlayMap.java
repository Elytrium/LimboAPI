/*
 * Copyright (C) 2021 Elytrium
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

import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class OverlayMap<K, V> implements Map<K, V> {

  protected final Map<K, V> parent;
  protected final Map<K, V> overlay;

  public OverlayMap(Map<K, V> parent, Map<K, V> overlay) {
    this.parent = parent;
    this.overlay = overlay;
  }

  @Override
  public int size() {
    return this.parent.size() + this.overlay.size();
  }

  @Override
  public boolean isEmpty() {
    return this.parent.isEmpty() && this.overlay.isEmpty();
  }

  @Override
  public boolean containsKey(Object o) {
    return this.parent.containsKey(o) || this.overlay.containsKey(o);
  }

  @Override
  public boolean containsValue(Object o) {
    return this.parent.containsValue(o) || this.overlay.containsValue(o);
  }

  @Override
  @SuppressWarnings("SuspiciousMethodCalls")
  public V get(Object o) {
    if (this.overlay.containsKey(o)) {
      return this.overlay.get(o);
    }

    return this.parent.get(o);
  }

  @Nullable
  @Override
  public V put(K k, V v) {
    return this.overlay.put(k, v);
  }

  @Override
  public V remove(Object o) {
    return this.overlay.remove(o);
  }

  @Override
  public void putAll(@NotNull Map<? extends K, ? extends V> map) {
    this.overlay.putAll(map);
  }

  @Override
  public void clear() {
    this.overlay.clear();
  }
}
