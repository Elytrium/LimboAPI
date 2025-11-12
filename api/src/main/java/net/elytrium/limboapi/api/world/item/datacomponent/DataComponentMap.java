/*
 * Copyright (C) 2021 - 2024 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.world.item.datacomponent;

import com.velocitypowered.api.network.ProtocolVersion;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * @sinceMinecraft 1.20.5
 */
@ApiStatus.NonExtendable
public interface DataComponentMap {

  void setData(DataComponentType.NonValued type);

  <T> void setData(DataComponentType.Valued<T> type, @NonNull T value);

  /**
   * Marks this data component as removed for this map
   */
  void unsetData(DataComponentType type);

  /**
   * Resets the value of this component to be the default value for the item type
   */
  void resetData(DataComponentType type);

  /**
   * @return the value for the data component type, or {@code null} if not set or marked as removed
   */
  @Nullable
  <T> T getData(DataComponentType.Valued<T> type);

  default <T> T getDataOrDefault(DataComponentType.Valued<T> type, T fallback) {
    T value = this.getData(type);
    return value == null ? fallback : value;
  }

  boolean hasData(DataComponentType type);

  DataComponentMap read(Object buf, ProtocolVersion version);

  DataComponentMap write(Object buf, ProtocolVersion version);
}
