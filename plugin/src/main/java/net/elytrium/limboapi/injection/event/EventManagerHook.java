/*
 * Copyright (C) 2021 - 2025 Elytrium
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
import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.proxy.event.VelocityEventManager;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import net.elytrium.commons.utils.reflection.ReflectionException;
import net.elytrium.limboapi.LimboAPI;
import net.elytrium.limboapi.Settings;

@SuppressWarnings("unchecked")
public class EventManagerHook {

  private static final Field HANDLERS_BY_TYPE_FIELD;
  private static final Class<?> HANDLER_REGISTRATION_CLASS;
  private static final MethodHandle PLUGIN_FIELD;
  private static final MethodHandle FIRE_METHOD;
  private static final MethodHandle FUTURE_FIELD;

  private final Set<GameProfile> proceededProfiles = new HashSet<>();
  private final LimboAPI plugin;
  private final VelocityEventManager eventManager;

  private Object handlerRegistrations;
  private boolean hasHandlerRegistration;

  public EventManagerHook(LimboAPI plugin, VelocityEventManager eventManager) {
    this.plugin = plugin;
    this.eventManager = eventManager;
  }

  @Subscribe(order = PostOrder.FIRST)
  public EventTask onGameProfileRequest(GameProfileRequestEvent event) {
    GameProfile originalProfile = event.getGameProfile();
    if (this.proceededProfiles.remove(originalProfile)) {
      return null;
    } else {
      CompletableFuture<GameProfileRequestEvent> fireFuture = new CompletableFuture<>();
      CompletableFuture<GameProfileRequestEvent> hookFuture = new CompletableFuture<>();
      fireFuture.thenAccept(modifiedEvent -> {
        try {
          this.plugin.getLoginListener().hookLoginSession(modifiedEvent);
          hookFuture.complete(modifiedEvent);
        } catch (Throwable e) {
          throw new ReflectionException(e);
        }
      });

      if (this.hasHandlerRegistration) {
        try {
          FIRE_METHOD.invoke(this.eventManager, fireFuture, event, 0, false, this.handlerRegistrations);
        } catch (Throwable e) {
          fireFuture.complete(event);
          throw new ReflectionException(e);
        }
      } else {
        fireFuture.complete(event);
      }

      // ignoring other subscribers by directly completing the future
      return EventTask.withContinuation(continuation -> hookFuture.whenComplete((result, cause) -> {
        try {
          CompletableFuture<GameProfileRequestEvent> future = (CompletableFuture<GameProfileRequestEvent>) FUTURE_FIELD.invokeExact(continuation);
          if (future != null) {
            future.complete(result);
          }
        } catch (Throwable e) {
          throw new ReflectionException(e);
        }
      }));
    }
  }

  @Subscribe(order = PostOrder.LAST)
  public EventTask onKickedFromServer(KickedFromServerEvent event) {
    CompletableFuture<KickedFromServerEvent> hookFuture = new CompletableFuture<>();
    try {
      Function<KickedFromServerEvent, Boolean> callback = this.plugin.getKickCallback(event.getPlayer());
      if (callback == null || !callback.apply(event)) {
        hookFuture.complete(event);
      }
    } catch (Throwable throwable) {
      LimboAPI.getLogger().error("Failed to handle KickCallback, ignoring its result", throwable);
      hookFuture.complete(event);
    }

    // if kick callback is null and no exception occurred, hookFuture won't be ever finished, and
    // the event chain would be broken, that is what we need.
    return EventTask.resumeWhenComplete(hookFuture);
  }

  public void proceedProfile(GameProfile profile) {
    this.proceededProfiles.add(profile);
  }

  @SuppressWarnings("rawtypes")
  public void reloadHandlers() throws IllegalAccessException {
    ListMultimap<Class<?>, ?> handlersMap = (ListMultimap<Class<?>, ?>) HANDLERS_BY_TYPE_FIELD.get(this.eventManager);
    List disabledHandlers = handlersMap.get(GameProfileRequestEvent.class);
    List preEvents = new ArrayList<>();
    List newHandlers = new ArrayList<>(disabledHandlers);

    if (this.handlerRegistrations != null) {
      for (int i = 0; i < Array.getLength(this.handlerRegistrations); ++i) {
        preEvents.add(Array.get(this.handlerRegistrations, i));
      }
    }

    try {
      for (Object handler : disabledHandlers) {
        PluginContainer pluginContainer = (PluginContainer) PLUGIN_FIELD.invoke(handler);
        String id = pluginContainer.getDescription().getId();
        if (Settings.IMP.MAIN.PRE_LIMBO_PROFILE_REQUEST_PLUGINS.contains(id)) {
          LimboAPI.getLogger().info("Hooking all GameProfileRequestEvent events from {} ", id);
          preEvents.add(handler);
          newHandlers.remove(handler);
        }
      }
    } catch (Throwable e) {
      throw new ReflectionException(e);
    }

    handlersMap.replaceValues(GameProfileRequestEvent.class, newHandlers);
    this.handlerRegistrations = Array.newInstance(HANDLER_REGISTRATION_CLASS, preEvents.size());

    for (int i = 0; i < preEvents.size(); ++i) {
      Array.set(this.handlerRegistrations, i, preEvents.get(i));
    }

    this.hasHandlerRegistration = !preEvents.isEmpty();
  }

  static {
    try {
      HANDLERS_BY_TYPE_FIELD = VelocityEventManager.class.getDeclaredField("handlersByType");
      HANDLERS_BY_TYPE_FIELD.setAccessible(true);

      HANDLER_REGISTRATION_CLASS = Class.forName("com.velocitypowered.proxy.event.VelocityEventManager$HandlerRegistration");
      PLUGIN_FIELD = MethodHandles.privateLookupIn(HANDLER_REGISTRATION_CLASS, MethodHandles.lookup())
          .findGetter(HANDLER_REGISTRATION_CLASS, "plugin", PluginContainer.class);

      Class<?> continuationTaskClass = Class.forName("com.velocitypowered.proxy.event.VelocityEventManager$ContinuationTask");
      FUTURE_FIELD = MethodHandles.privateLookupIn(continuationTaskClass, MethodHandles.lookup())
          .findGetter(continuationTaskClass, "future", CompletableFuture.class);

      // The desired 5-argument fire method is private, and its 5th argument is the array of the private class,
      // so we can't pass it into the Class#getDeclaredMethod(Class...) method.
      Method fireMethod = Arrays.stream(VelocityEventManager.class.getDeclaredMethods())
          .filter(method -> method.getName().equals("fire") && method.getParameterCount() == 5)
          .findFirst()
          .orElseThrow();
      fireMethod.setAccessible(true);
      FIRE_METHOD = MethodHandles.privateLookupIn(VelocityEventManager.class, MethodHandles.lookup())
          .findVirtual(VelocityEventManager.class, "fire", MethodType.methodType(
              void.class,
              CompletableFuture.class,
              Object.class,
              int.class,
              boolean.class,
              Array.newInstance(HANDLER_REGISTRATION_CLASS, 0).getClass()
          ));
    } catch (NoSuchFieldException | ClassNotFoundException | IllegalAccessException | NoSuchMethodException e) {
      throw new ReflectionException(e);
    }
  }
}
