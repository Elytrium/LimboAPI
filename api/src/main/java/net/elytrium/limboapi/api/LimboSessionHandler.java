/*
 * Copyright (C) 2021 - 2025 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api;

import net.elytrium.limboapi.api.player.LimboPlayer;
import net.kyori.adventure.key.Key;

public interface LimboSessionHandler {

  default void onSpawn(Limbo server, LimboPlayer player) {

  }

  default void onConfig(Limbo server, LimboPlayer player) {

  }

  default void onMove(double posX, double posY, double posZ) {

  }

  default void onMove(double posX, double posY, double posZ, float yaw, float pitch) {

  }

  default void onRotate(float yaw, float pitch) {

  }

  default void onGround(boolean onGround) {

  }

  default void onTeleport(int teleportID) {

  }

  default void onChat(String chat) {

  }

  /**
   * @param packet Any velocity built-in packet or any packet registered via {@link Limbo#registerPacket}.
   */
  default void onGeneric(Object packet) {

  }

  /**
   * Called when the client sends a cookie response (reply to {@code Player#requestCookie}) while
   * the player is still inside the Limbo. The response is also buffered and replayed as a
   * {@code CookieReceiveEvent} when the player is handed back to Velocity, so this hook is meant
   * for handlers that need the cookie value during the Limbo session itself.
   *
   * @param key  the cookie key
   * @param data the cookie payload
   */
  default void onCookieResponse(Key key, byte[] data) {

  }

  default void onDisconnect() {

  }
}
