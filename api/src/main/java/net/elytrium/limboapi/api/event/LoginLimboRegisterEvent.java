/*
 * Copyright (C) 2021 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.event;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.proxy.Player;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * This event is fired during login process before the player has been authenticated, e.g. to enable or disable
 * custom authentication.
 */
public class LoginLimboRegisterEvent {

  private final Player player;
  private final Queue<Runnable> callbacks;

  public LoginLimboRegisterEvent(Player player) {
    this.player = Preconditions.checkNotNull(player, "player");
    this.callbacks = new LinkedBlockingQueue<>();
  }

  public Player getPlayer() {
    return this.player;
  }

  @Override
  public String toString() {
    return "LoginLimboRegisterEvent{"
        + "player=" + this.player
        + "}";
  }

  public Queue<Runnable> getCallbacks() {
    return this.callbacks;
  }

  public void addCallback(Runnable callback) {
    this.callbacks.add(callback);
  }
}
