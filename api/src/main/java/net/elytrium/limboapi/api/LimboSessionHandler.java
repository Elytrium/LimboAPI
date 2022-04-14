/*
 * Copyright (C) 2021 - 2022 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api;

import net.elytrium.limboapi.api.player.LimboPlayer;

public interface LimboSessionHandler {

  default void onSpawn(Limbo server, LimboPlayer player) {

  }

  default void onMove(double x, double y, double z) {

  }

  default void onMove(double x, double y, double z, float yaw, float pitch) {

  }

  default void onRotate(float yaw, float pitch) {

  }

  default void onGround(boolean onGround) {

  }

  default void onTeleport(int teleportId) {

  }

  default void onChat(String chat) {

  }

  /**
   * @param packet Any velocity built-in packet or any packet registered via {@link LimboFactory#registerPacket}
   */
  default void onGeneric(Object packet) {

  }

  default void onDisconnect() {

  }
}
