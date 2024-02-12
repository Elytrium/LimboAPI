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

import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;
import io.netty.util.collection.IntObjectMap;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.elytrium.limboapi.api.utils.OverlayMap;
import org.jetbrains.annotations.NotNull;

public class OverlayIntObjectMap<K> extends OverlayMap<Integer, K> implements IntObjectMap<K> {

  public OverlayIntObjectMap(Map<Integer, K> parent, Map<Integer, K> overlay) {
    super(parent, overlay);
  }

  @Override
  public K get(int key) {
    return super.get(key);
  }

  @Override
  public K put(int key, K value) {
    return super.put(key, value);
  }

  @Override
  public K remove(int key) {
    return super.remove(key);
  }

  @Override
  public Iterable<PrimitiveEntry<K>> entries() {
    return Iterables.concat(((IntObjectMap<K>) parent).entries(), ((IntObjectMap<K>) overlay).entries());
  }

  @Override
  public boolean containsKey(int key) {
    return super.containsKey(key);
  }

  @Override
  public Set<Integer> keySet() {
    return Streams.concat(this.parent.keySet().stream(), this.overlay.keySet().stream()).collect(Collectors.toSet());
  }

  @NotNull
  @Override
  public Collection<K> values() {
    return Streams.concat(this.parent.values().stream(), this.overlay.values().stream()).collect(Collectors.toList());
  }

  @NotNull
  @Override
  public Set<Entry<Integer, K>> entrySet() {
    return Streams.concat(this.parent.entrySet().stream(), this.overlay.entrySet().stream()).collect(Collectors.toSet());
  }
}
