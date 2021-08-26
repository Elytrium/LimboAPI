/*
 * Copyright (C) 2021 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api;

import com.velocitypowered.proxy.protocol.MinecraftPacket;
import net.elytrium.limboapi.api.player.LimboPlayer;

public interface LimboSessionHandler {
  default void onSpawn(Limbo server, LimboPlayer player) {

  }

  //void onMove(double x, double y, double z, float yaw, float pitch);

  default void onMove(double x, double y, double z) {

  }

  default void onRotate(float yaw, float pitch) {

  }

  default void onGround(boolean onGround) {

  }

  default void onTeleport(int teleportId) {

  }

  default void onChat(String chat) {

  }

  default void onGeneric(MinecraftPacket packet) {

  }

  default void onDisconnect() {

  }
}
