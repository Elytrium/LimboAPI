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

import com.google.common.collect.Iterables;
import io.netty.util.collection.IntObjectMap;
import java.util.Map;

public class OverlayIntObjectMap<V> extends OverlayMap<Integer, V> implements IntObjectMap<V> {

  public OverlayIntObjectMap(Map<Integer, V> parent, Map<Integer, V> overlay) {
    super(parent, overlay);
  }

  @Override
  public V get(int key) {
    V value = ((IntObjectMap<V>) this.overlay).get(key);
    if (value != null) {
      return value;
    }

    return this.override ? null : this.parent.get(key);
  }

  @Override
  public V put(int key, V value) {
    return ((IntObjectMap<V>) this.overlay).put(key, value);
  }

  @Override
  public V remove(int key) {
    return ((IntObjectMap<V>) this.overlay).remove(key);
  }

  @Override
  public Iterable<PrimitiveEntry<V>> entries() {
    if (this.override) {
      return ((IntObjectMap<V>) this.overlay).entries();
    }

    return Iterables.concat(((IntObjectMap<V>) this.overlay).entries(), ((IntObjectMap<V>) this.parent).entries());
  }

  @Override
  public boolean containsKey(int key) {
    if (this.override) {
      return ((IntObjectMap<V>) this.overlay).containsKey(key);
    }

    return ((IntObjectMap<V>) this.overlay).containsKey(key) || ((IntObjectMap<V>) this.parent).containsKey(key);
  }
}
