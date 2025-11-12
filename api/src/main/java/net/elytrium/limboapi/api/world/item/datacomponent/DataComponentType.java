/*
 * Copyright (C) 2021 - 2024 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.world.item.datacomponent;

import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;

/**
 * @sinceMinecraft 1.20.5
 */
@NullMarked
@ApiStatus.Experimental
@ApiStatus.NonExtendable
public interface DataComponentType {

  @SuppressWarnings("unused")
  @ApiStatus.NonExtendable
  interface Valued<T> extends DataComponentType {

  }

  @ApiStatus.NonExtendable
  interface NonValued extends DataComponentType {

  }
}
