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

package net.elytrium.limboapi.protocol.util;

import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.EncoderException;
import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;
import org.checkerframework.checker.nullness.qual.Nullable;

public class LimboProtocolUtils {

  public static <T> void writeCollection(ByteBuf buf, Collection<T> collection, Consumer<T> encoder) {
    LimboProtocolUtils.writeCollection(buf, collection, Integer.MAX_VALUE, encoder);
  }

  public static <T> void writeCollection(ByteBuf buf, Collection<T> collection, int limit, Consumer<T> encoder) {
    if (collection == null) {
      ProtocolUtils.writeVarInt(buf, 0);
      return;
    }

    int amount = collection.size();
    if (amount == 0) {
      ProtocolUtils.writeVarInt(buf, 0);
    } else {
      if (amount > limit) {
        throw new EncoderException(amount + " elements exceeded max size of: " + limit);
      }

      ProtocolUtils.writeVarInt(buf, amount);
      collection.forEach(encoder);
    }
  }

  public static <T> void writeArray(ByteBuf buf, T @Nullable [] array, Consumer<T> encoder) {
    LimboProtocolUtils.writeArray(buf, array, Integer.MAX_VALUE, encoder);
  }

  public static <T> void writeArray(ByteBuf buf, T @Nullable [] array, int limit, Consumer<T> encoder) {
    if (array == null) {
      ProtocolUtils.writeVarInt(buf, 0);
      return;
    }

    int amount = array.length;
    if (amount == 0) {
      ProtocolUtils.writeVarInt(buf, 0);
    } else {
      if (amount > limit) {
        throw new EncoderException(amount + " elements exceeded max size of: " + limit);
      }

      ProtocolUtils.writeVarInt(buf, amount);
      for (T value : array) {
        encoder.accept(value);
      }
    }
  }

  public static void writeLongArray(ByteBuf buf, long @Nullable [] array) {
    LimboProtocolUtils.writeLongArray(buf, array, Integer.MAX_VALUE);
  }

  public static void writeLongArray(ByteBuf buf, long @Nullable [] array, int limit) {
    if (array == null) {
      ProtocolUtils.writeVarInt(buf, 0);
      return;
    }

    int amount = array.length;
    if (amount == 0) {
      ProtocolUtils.writeVarInt(buf, 0);
    } else {
      if (amount > limit) {
        throw new EncoderException(amount + " elements exceeded max size of: " + limit);
      }

      ProtocolUtils.writeVarInt(buf, amount);
      for (long value : array) {
        buf.writeLong(value);
      }
    }
  }

  public static void writeVarIntArray(ByteBuf buf, int @Nullable [] array) {
    LimboProtocolUtils.writeVarIntArray(buf, array, Integer.MAX_VALUE);
  }

  public static void writeVarIntArray(ByteBuf buf, int @Nullable [] array, int limit) {
    if (array == null) {
      ProtocolUtils.writeVarInt(buf, 0);
      return;
    }

    int amount = array.length;
    if (amount == 0) {
      ProtocolUtils.writeVarInt(buf, 0);
    } else {
      if (amount > limit) {
        throw new EncoderException(amount + " elements exceeded max size of: " + limit);
      }

      ProtocolUtils.writeVarInt(buf, amount);
      for (int value : array) {
        ProtocolUtils.writeVarInt(buf, value);
      }
    }
  }

  public static <K, V> void writeMap(ByteBuf buf, Map<K, V> map, EncoderByteBufValue<K> keyEncoder, EncoderByteBufValue<V> valueEncoder) {
    LimboProtocolUtils.writeMap(buf, map, Integer.MAX_VALUE, keyEncoder, valueEncoder);
  }

  public static <K, V> void writeMap(ByteBuf buf, Map<K, V> map, int limit, EncoderByteBufValue<K> keyEncoder, EncoderByteBufValue<V> valueEncoder) {
    if (map == null) {
      ProtocolUtils.writeVarInt(buf, 0);
      return;
    }

    int amount = map.size();
    if (amount == 0) {
      ProtocolUtils.writeVarInt(buf, 0);
    } else {
      if (amount > limit) {
        throw new EncoderException(amount + " elements exceeded max size of: " + limit);
      }

      ProtocolUtils.writeVarInt(buf, amount);
      map.forEach((key, value) -> {
        keyEncoder.write(buf, key);
        valueEncoder.write(buf, value);
      });
    }
  }

  @FunctionalInterface
  public interface EncoderByteBufValue<T> {

    void write(ByteBuf buf, T value);
  }
}
