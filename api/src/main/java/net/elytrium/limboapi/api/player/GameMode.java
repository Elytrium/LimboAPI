/*
 * Copyright (C) 2021 - 2022 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.player;

import org.checkerframework.checker.nullness.qual.Nullable;

@SuppressWarnings("unused")
public enum GameMode {

  SURVIVAL,
  CREATIVE,
  ADVENTURE,
  SPECTATOR;

  /**
   * Cached {@link #values()} array to avoid constant array allocation.
   */
  private static final GameMode[] VALUES = values();

  /**
   * Get the ID of this {@link GameMode}.
   *
   * @return The ID.
   * @see #getById(int)
   */
  public int getId() {
    return this.ordinal();
  }

  /**
   * Get a {@link GameMode} by its' ID.
   *
   * @param id The ID.
   * @return The {@link GameMode}, or {@code null} if it does not exist.
   * @see #getId()
   */
  @Nullable
  public static GameMode getById(int id) {
    return VALUES[id];
  }
}
