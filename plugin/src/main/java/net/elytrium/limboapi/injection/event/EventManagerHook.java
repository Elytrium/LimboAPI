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

import com.google.common.collect.ListMultimap;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginManager;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.command.VelocityCommandManager;
import com.velocitypowered.proxy.event.VelocityEventManager;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import net.elytrium.limboapi.LimboAPI;
import net.elytrium.limboapi.Settings;

@SuppressWarnings("unchecked")
public class EventManagerHook extends VelocityEventManager {

  private static final Class<?> handlerRegistration;
  private static final Field handlersCache;
  private static final Field untargetedMethodHandlers;
  private static final Field handlerAdapters;
  private static final Field eventTypeTracker;
  private static final Field eventManager;
  private static final Field handlersMapField;
  private static final Field pluginField;
  private static final Field eventManagerInCommandManager;
  private static final Method fire;
  private static EventManagerHook instance;

  private final LimboAPI plugin;
  private final Set<GameProfile> proceededProfiles = new HashSet<>();

  private Object handlerRegistrations;
  private boolean hasHandlerRegistration;

  static {
    try {
      eventManager = VelocityServer.class.getDeclaredField("eventManager");
      eventManager.setAccessible(true);

      handlersMapField = VelocityEventManager.class.getDeclaredField("handlersByType");
      handlersMapField.setAccessible(true);

      handlersCache = VelocityEventManager.class.getDeclaredField("handlersCache");
      handlersCache.setAccessible(true);

      untargetedMethodHandlers = VelocityEventManager.class.getDeclaredField("untargetedMethodHandlers");
      untargetedMethodHandlers.setAccessible(true);

      handlerAdapters = VelocityEventManager.class.getDeclaredField("handlerAdapters");
      handlerAdapters.setAccessible(true);

      eventTypeTracker = VelocityEventManager.class.getDeclaredField("eventTypeTracker");
      eventTypeTracker.setAccessible(true);

      handlerRegistration = Class.forName("com.velocitypowered.proxy.event.VelocityEventManager$HandlerRegistration");

      pluginField = handlerRegistration.getDeclaredField("plugin");
      pluginField.setAccessible(true);

      eventManagerInCommandManager = VelocityCommandManager.class.getDeclaredField("eventManager");
      eventManagerInCommandManager.setAccessible(true);

      // The desired 5-argument fire method is private, and its 5th argument is the array of the private class,
      // so we can't pass it into the Class#getDeclaredMethod(Class...) method.
      fire = Arrays.stream(VelocityEventManager.class.getDeclaredMethods())
          .filter(method -> method.getName().equals("fire") && method.getParameterCount() == 5)
          .findFirst()
          .orElseThrow();
      fire.setAccessible(true);
    } catch (NoSuchFieldException | ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  public EventManagerHook(PluginManager pluginManager, LimboAPI plugin) {
    super(pluginManager);
    this.plugin = plugin;
  }

  public static void init(LimboAPI plugin) throws IllegalAccessException, ExecutionException {
    instance = new EventManagerHook(plugin.getServer().getPluginManager(), plugin);

    EventManager oldEventManager = plugin.getServer().getEventManager();
    handlersMapField.set(instance, handlersMapField.get(oldEventManager));
    handlersCache.set(instance, handlersCache.get(oldEventManager));
    untargetedMethodHandlers.set(instance, untargetedMethodHandlers.get(oldEventManager));
    handlerAdapters.set(instance, handlerAdapters.get(oldEventManager));
    eventTypeTracker.set(instance, eventTypeTracker.get(oldEventManager));

    eventManager.set(plugin.getServer(), instance);
    eventManagerInCommandManager.set(plugin.getServer().getCommandManager(), instance);
  }

  public static void postInit() {
    try {
      instance.reloadHandlers();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    }
  }

  @SuppressWarnings("rawtypes")
  public void reloadHandlers() throws IllegalAccessException {
    ListMultimap<Class<?>, ?> handlersMap = (ListMultimap<Class<?>, ?>) handlersMapField.get(this);
    List disabledHandlers = handlersMap.get(GameProfileRequestEvent.class);
    List preEvents = new ArrayList<>();
    List newHandlers = new ArrayList<>(disabledHandlers);

    if (this.handlerRegistrations != null) {
      for (int i = 0; i < Array.getLength(this.handlerRegistrations); ++i) {
        preEvents.add(Array.get(this.handlerRegistrations, i));
      }
    }

    for (Object handler : disabledHandlers) {
      PluginContainer pluginContainer = (PluginContainer) pluginField.get(handler);
      String id = pluginContainer.getDescription().getId();
      if (Settings.IMP.MAIN.PRE_LIMBO_PROFILE_REQUEST_PLUGINS.contains(id)) {
        this.plugin.getLogger().info("Hooking all GameProfileRequestEvent events from {} ", id);
        preEvents.add(handler);
        newHandlers.remove(handler);
      }
    }

    handlersMap.replaceValues(GameProfileRequestEvent.class, newHandlers);
    this.handlerRegistrations = Array.newInstance(handlerRegistration, preEvents.size());

    for (int i = 0; i < preEvents.size(); ++i) {
      Array.set(this.handlerRegistrations, i, preEvents.get(i));
    }

    this.hasHandlerRegistration = preEvents.size() != 0;
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

  @Override
  public void register(Object plugin, Object listener) {
    super.register(plugin, listener);

    if (Settings.IMP.MAIN != null && Settings.IMP.MAIN.AUTO_REGENERATE_LISTENERS) {
      try {
        this.reloadHandlers();
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      }
    }
  }

  private <E> CompletableFuture<E> proxyHook(E event) {
    if (event instanceof GameProfileRequestEvent) {
      GameProfile originalProfile = ((GameProfileRequestEvent) event).getGameProfile();
      if (this.proceededProfiles.contains(originalProfile)) {
        this.proceededProfiles.remove(originalProfile);
        return null;
      }

      CompletableFuture<E> fireFuture = new CompletableFuture<>();
      CompletableFuture<E> hookFuture = new CompletableFuture<>();

      fireFuture.thenAccept((modifiedEvent) -> {
        try {
          GameProfileRequestEvent requestEvent = (GameProfileRequestEvent) modifiedEvent;
          GameProfile profile = requestEvent.getGameProfile();

          this.plugin.getLoginListener().hookLoginSession(requestEvent);
          this.proceededProfiles.add(profile);

          hookFuture.complete(modifiedEvent);
        } catch (IllegalAccessException e) {
          e.printStackTrace();
        }
      });

      try {
        if (this.hasHandlerRegistration) {
          fire.invoke(this, fireFuture, event, 0, false, this.handlerRegistrations);
        } else {
          fireFuture.complete(event);
        }
      } catch (IllegalAccessException | InvocationTargetException e) {
        fireFuture.complete(event);
      }

      return hookFuture;
    }

    return null;
  }
}
