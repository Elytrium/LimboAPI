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

import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.NonNull;

public class SetIsObjectSet<K> implements ObjectSet<K> {

  private final Set<K> set;

  public SetIsObjectSet(Set<K> set) {
    this.set = set;
  }

  @Override
  public int size() {
    return this.set.size();
  }

  @Override
  public boolean isEmpty() {
    return this.set.isEmpty();
  }

  @Override
  public boolean contains(Object o) {
    return this.set.contains(o);
  }

  @Override
  public ObjectIterator<K> iterator() {
    return new IteratorIsObjectIterator<>(this.set.iterator());
  }

  @NonNull
  @Override
  public Object[] toArray() {
    return this.set.toArray();
  }

  @NonNull
  @Override
  public <T> T[] toArray(T @NonNull [] ts) {
    return this.set.toArray(ts);
  }

  @Override
  public boolean add(K k) {
    return this.set.add(k);
  }

  @Override
  public boolean remove(Object o) {
    return this.set.remove(o);
  }

  @Override
  public boolean containsAll(@NonNull Collection<?> collection) {
    return this.set.containsAll(collection);
  }

  @Override
  public boolean addAll(@NonNull Collection<? extends K> collection) {
    return this.set.addAll(collection);
  }

  @Override
  public boolean removeAll(@NonNull Collection<?> collection) {
    return this.set.removeAll(collection);
  }

  @Override
  public boolean retainAll(@NonNull Collection<?> collection) {
    return this.set.retainAll(collection);
  }

  @Override
  public void clear() {
    this.set.clear();
  }

  private static final class IteratorIsObjectIterator<K> implements ObjectIterator<K> {

    private final Iterator<K> iterator;

    private IteratorIsObjectIterator(Iterator<K> iterator) {
      this.iterator = iterator;
    }

    @Override
    public boolean hasNext() {
      return this.iterator.hasNext();
    }

    @Override
    public K next() {
      return this.iterator.next();
    }
  }
}
