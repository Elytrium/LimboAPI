/*
 * Copyright (C) 2021 Elytrium
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.elytrium.limboapi.injection.event;

import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import com.velocitypowered.api.plugin.PluginManager;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.command.VelocityCommandManager;
import com.velocitypowered.proxy.event.VelocityEventManager;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.elytrium.limboapi.LimboAPI;

public class EventManagerHook extends VelocityEventManager {

  private static final Field eventManager;
  private static final Field eventManagerInCommandManager;

  private final LimboAPI plugin;
  private final Set<GameProfile> proceededProfiles = new HashSet<>();

  static {
    try {
      eventManager = VelocityServer.class.getDeclaredField("eventManager");
      eventManager.setAccessible(true);
      eventManagerInCommandManager = VelocityCommandManager.class.getDeclaredField("eventManager");
      eventManagerInCommandManager.setAccessible(true);
    } catch (NoSuchFieldException e) {
      throw new RuntimeException(e);
    }
  }

  public EventManagerHook(PluginManager pluginManager, LimboAPI plugin) {
    super(pluginManager);
    this.plugin = plugin;
  }

  public static void init(LimboAPI plugin) throws IllegalAccessException {
    EventManagerHook hook = new EventManagerHook(plugin.getServer().getPluginManager(), plugin);
    eventManager.set(plugin.getServer(), hook);
    eventManagerInCommandManager.set(plugin.getServer().getCommandManager(), hook);
  }

  @Override
  public void fireAndForget(Object event) {
    Object toReply = this.proxyHook(event);
    if (toReply == null) {
      super.fireAndForget(event);
    }
  }

  @Override
  public <E> CompletableFuture<E> fire(E event) {
    CompletableFuture<E> toReply = this.proxyHook(event);
    if (toReply == null) {
      return super.fire(event);
    }

    return toReply;
  }

  private <E> CompletableFuture<E> proxyHook(E event) {
    if (event instanceof GameProfileRequestEvent) {
      try {
        GameProfileRequestEvent requestEvent = (GameProfileRequestEvent) event;
        GameProfile profile = requestEvent.getGameProfile();

        if (this.proceededProfiles.contains(profile)) {
          this.proceededProfiles.remove(profile);
          return null;
        }

        this.plugin.getLoginListener().hookLoginSession(requestEvent);
        this.proceededProfiles.add(profile);

        return CompletableFuture.completedFuture(event);
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      }
    }

    return null;
  }
}
