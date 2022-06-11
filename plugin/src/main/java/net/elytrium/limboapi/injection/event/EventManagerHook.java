/*
 * Copyright (C) 2021 - 2022 Elytrium
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
import net.elytrium.java.commons.reflection.ReflectionException;
import net.elytrium.limboapi.LimboAPI;
import net.elytrium.limboapi.Settings;

@SuppressWarnings("unchecked")
public class EventManagerHook extends VelocityEventManager {

  private static final Field HANDLERS_BY_TYPE_FIELD;
  private static final Field HANDLERS_CACHE_FIELD;
  private static final Field HANDLER_ADAPTERS_FIELD;
  private static final Field EVENT_TYPE_TRACKER_FIELD;
  private static final Field VELOCITY_SERVER_EVENT_MANAGER_FIELD;
  private static final Class<?> HANDLER_REGISTRATION_CLASS;
  private static final Field UNTARGETED_METHOD_HANDLERS_FIELD;
  private static final Field PLUGIN_FIELD;
  private static final Field VELOCITY_COMMAND_MANAGER_EVENT_MANAGER_FIELD;
  private static final Method FIRE_METHOD;

  private final Set<GameProfile> proceededProfiles = new HashSet<>();
  private final LimboAPI plugin;

  private Object handlerRegistrations;
  private boolean hasHandlerRegistration;

  private EventManagerHook(PluginManager pluginManager, LimboAPI plugin) {
    super(pluginManager);

    this.plugin = plugin;
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
    } else {
      return toReply;
    }
  }

  private <E> CompletableFuture<E> proxyHook(E event) {
    if (event instanceof GameProfileRequestEvent) {
      GameProfile originalProfile = ((GameProfileRequestEvent) event).getGameProfile();
      if (this.proceededProfiles.contains(originalProfile)) {
        this.proceededProfiles.remove(originalProfile);
        return null;
      } else {
        CompletableFuture<E> fireFuture = new CompletableFuture<>();
        CompletableFuture<E> hookFuture = new CompletableFuture<>();
        fireFuture.thenAccept(modifiedEvent -> {
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
            FIRE_METHOD.invoke(this, fireFuture, event, 0, false, this.handlerRegistrations);
          } else {
            fireFuture.complete(event);
          }
        } catch (IllegalAccessException | InvocationTargetException e) {
          fireFuture.complete(event);
        }

        return hookFuture;
      }
    } else {
      return null;
    }
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

  @SuppressWarnings("rawtypes")
  public void reloadHandlers() throws IllegalAccessException {
    ListMultimap<Class<?>, ?> handlersMap = (ListMultimap<Class<?>, ?>) HANDLERS_BY_TYPE_FIELD.get(this);
    List disabledHandlers = handlersMap.get(GameProfileRequestEvent.class);
    List preEvents = new ArrayList<>();
    List newHandlers = new ArrayList<>(disabledHandlers);

    if (this.handlerRegistrations != null) {
      for (int i = 0; i < Array.getLength(this.handlerRegistrations); ++i) {
        preEvents.add(Array.get(this.handlerRegistrations, i));
      }
    }

    for (Object handler : disabledHandlers) {
      PluginContainer pluginContainer = (PluginContainer) PLUGIN_FIELD.get(handler);
      String id = pluginContainer.getDescription().getId();
      if (Settings.IMP.MAIN.PRE_LIMBO_PROFILE_REQUEST_PLUGINS.contains(id)) {
        LimboAPI.getLogger().info("Hooking all GameProfileRequestEvent events from {} ", id);
        preEvents.add(handler);
        newHandlers.remove(handler);
      }
    }

    handlersMap.replaceValues(GameProfileRequestEvent.class, newHandlers);
    this.handlerRegistrations = Array.newInstance(HANDLER_REGISTRATION_CLASS, preEvents.size());

    for (int i = 0; i < preEvents.size(); ++i) {
      Array.set(this.handlerRegistrations, i, preEvents.get(i));
    }

    this.hasHandlerRegistration = preEvents.size() != 0;
  }

  static {
    try {
      HANDLERS_BY_TYPE_FIELD = VelocityEventManager.class.getDeclaredField("handlersByType");
      HANDLERS_BY_TYPE_FIELD.setAccessible(true);

      HANDLERS_CACHE_FIELD = VelocityEventManager.class.getDeclaredField("handlersCache");
      HANDLERS_CACHE_FIELD.setAccessible(true);

      HANDLER_ADAPTERS_FIELD = VelocityEventManager.class.getDeclaredField("handlerAdapters");
      HANDLER_ADAPTERS_FIELD.setAccessible(true);

      VELOCITY_SERVER_EVENT_MANAGER_FIELD = VelocityServer.class.getDeclaredField("eventManager");
      VELOCITY_SERVER_EVENT_MANAGER_FIELD.setAccessible(true);

      UNTARGETED_METHOD_HANDLERS_FIELD = VelocityEventManager.class.getDeclaredField("untargetedMethodHandlers");
      UNTARGETED_METHOD_HANDLERS_FIELD.setAccessible(true);

      EVENT_TYPE_TRACKER_FIELD = VelocityEventManager.class.getDeclaredField("eventTypeTracker");
      EVENT_TYPE_TRACKER_FIELD.setAccessible(true);

      HANDLER_REGISTRATION_CLASS = Class.forName("com.velocitypowered.proxy.event.VelocityEventManager$HandlerRegistration");
      PLUGIN_FIELD = HANDLER_REGISTRATION_CLASS.getDeclaredField("plugin");
      PLUGIN_FIELD.setAccessible(true);

      VELOCITY_COMMAND_MANAGER_EVENT_MANAGER_FIELD = VelocityCommandManager.class.getDeclaredField("eventManager");
      VELOCITY_COMMAND_MANAGER_EVENT_MANAGER_FIELD.setAccessible(true);

      // The desired 5-argument fire method is private, and its 5th argument is the array of the private class,
      // so we can't pass it into the Class#getDeclaredMethod(Class...) method.
      FIRE_METHOD = Arrays.stream(VelocityEventManager.class.getDeclaredMethods())
          .filter(method -> method.getName().equals("fire") && method.getParameterCount() == 5)
          .findFirst()
          .orElseThrow();
      FIRE_METHOD.setAccessible(true);
    } catch (NoSuchFieldException | ClassNotFoundException e) {
      throw new ReflectionException(e);
    }
  }

  public static void init(LimboAPI plugin) throws ReflectiveOperationException {
    VelocityServer server = plugin.getServer();
    EventManager newEventManager = new EventManagerHook(server.getPluginManager(), plugin);
    EventManager oldEventManager = server.getEventManager();
    HANDLERS_BY_TYPE_FIELD.set(newEventManager, HANDLERS_BY_TYPE_FIELD.get(oldEventManager));
    HANDLERS_CACHE_FIELD.set(newEventManager, HANDLERS_CACHE_FIELD.get(oldEventManager));
    UNTARGETED_METHOD_HANDLERS_FIELD.set(newEventManager, UNTARGETED_METHOD_HANDLERS_FIELD.get(oldEventManager));
    HANDLER_ADAPTERS_FIELD.set(newEventManager, HANDLER_ADAPTERS_FIELD.get(oldEventManager));
    EVENT_TYPE_TRACKER_FIELD.set(newEventManager, EVENT_TYPE_TRACKER_FIELD.get(oldEventManager));

    VELOCITY_SERVER_EVENT_MANAGER_FIELD.set(server, newEventManager);
    VELOCITY_COMMAND_MANAGER_EVENT_MANAGER_FIELD.set(server.getCommandManager(), newEventManager);
  }
}
