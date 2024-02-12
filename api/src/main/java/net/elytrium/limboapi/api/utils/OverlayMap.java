/*
 * Copyright (C) 2021 - 2024 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.utils;

import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class OverlayMap<K, V> implements Map<K, V> {

  protected boolean override = false;
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
    if (this.override) {
      return this.overlay.containsKey(o);
    }

    return this.parent.containsKey(o) || this.overlay.containsKey(o);
  }

  @Override
  public boolean containsValue(Object o) {
    if (this.override) {
      return this.overlay.containsValue(o);
    }

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

  public boolean isOverride() {
    return this.override;
  }

  public void setOverride(boolean override) {
    this.override = override;
  }
}
