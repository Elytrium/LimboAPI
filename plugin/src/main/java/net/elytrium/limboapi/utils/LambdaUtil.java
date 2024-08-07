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