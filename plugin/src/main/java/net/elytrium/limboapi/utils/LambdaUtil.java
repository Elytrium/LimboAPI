/*
 * Copyright (C) 2021 - 2024 Elytrium
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

package net.elytrium.limboapi.utils;

import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.util.function.BiConsumer;
import java.util.function.Function;

public final class LambdaUtil {
  private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

  public static <T, R> Function<T, R> getterOf(Field field) throws Throwable {
    MethodHandle handle = LOOKUP.unreflectGetter(field);
    MethodType type = handle.type();
    //noinspection unchecked
    return (Function<T, R>) LambdaMetafactory.metafactory(
        LOOKUP,
        "apply",
        MethodType.methodType(Function.class, MethodHandle.class),
        type.generic(),
        MethodHandles.exactInvoker(type),
        type
    ).getTarget().invokeExact(handle);
  }

  public static <T, R> BiConsumer<T, R> setterOf(Field f) throws Throwable {
    MethodHandle handle = LOOKUP.unreflectSetter(f);
    MethodType type = handle.type();
    //noinspection unchecked
    return (BiConsumer<T, R>) LambdaMetafactory.metafactory(
        LOOKUP,
        "accept",
        MethodType.methodType(BiConsumer.class, MethodHandle.class),
        type.generic().changeReturnType(void.class),
        MethodHandles.exactInvoker(type),
        type
    ).getTarget().invokeExact(handle);
  }
}
