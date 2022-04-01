/*
 * Copyright (C) 2021 - 2022 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.event;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.util.GameProfile;

/**
 * Safe GameProfileRequestEvent, which executes only after all the login limbo server.
 *
 * @deprecated Use Velocity built-in GameProfileRequestEvent
 */
@Deprecated
public class SafeGameProfileRequestEvent {

  private final String username;
  private final GameProfile originalProfile;
  private final boolean onlineMode;
  private GameProfile gameProfile;

  public SafeGameProfileRequestEvent(GameProfile originalProfile, boolean onlineMode) {
    this.originalProfile = Preconditions.checkNotNull(originalProfile, "originalProfile");
    this.username = originalProfile.getName();
    this.onlineMode = onlineMode;
  }

  public String getUsername() {
    return this.username;
  }

  public GameProfile getOriginalProfile() {
    return this.originalProfile;
  }

  public boolean isOnlineMode() {
    return this.onlineMode;
  }

  public void setGameProfile(GameProfile gameProfile) {
    this.gameProfile = gameProfile;
  }

  public GameProfile getGameProfile() {
    return this.gameProfile == null ? this.originalProfile : this.gameProfile;
  }

  public String toString() {
    return "SafeGameProfileRequestEvent{"
        + "username=" + this.username
        + ", originalProfile=" + this.originalProfile
        + ", gameProfile=" + this.gameProfile
        + "}";
  }
}
