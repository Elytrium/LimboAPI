/*
 * Copyright (C) 2021 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.event;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.proxy.Player;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * This event is fired during login process before the player has been authenticated, e.g. to enable or disable
 * custom authentication.
 */
public final class LoginLimboRegisterEvent {

  private final Player player;
  private final List<CompletableFuture<Void>> callbacks;

  public LoginLimboRegisterEvent(Player player) {
    this.player = Preconditions.checkNotNull(player, "player");
    this.callbacks = new ArrayList<>();
  }

  public Player getPlayer() {
    return player;
  }

  @Override
  public String toString() {
    return "LoginLimboRegisterEvent{"
        + "player=" + player
        + '}';
  }

  public List<CompletableFuture<Void>> getCallbacks() {
    return this.callbacks;
  }

  public void addCallback(CompletableFuture<Void> callback) {
    this.callbacks.add(callback);
  }
}
