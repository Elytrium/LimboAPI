package net.elytrium.limboapi.protocol.codec;

import com.velocitypowered.api.network.ProtocolVersion;
import io.netty.buffer.ByteBuf;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import net.elytrium.limboapi.utils.functions.Function10;
import net.elytrium.limboapi.utils.functions.Function11;
import net.elytrium.limboapi.utils.functions.Function3;
import net.elytrium.limboapi.utils.functions.Function4;
import net.elytrium.limboapi.utils.functions.Function5;
import net.elytrium.limboapi.utils.functions.Function6;
import net.elytrium.limboapi.utils.functions.Function7;
import net.elytrium.limboapi.utils.functions.Function8;
import net.elytrium.limboapi.utils.functions.Function9;
import org.checkerframework.checker.nullness.qual.Nullable;

@SuppressWarnings("DuplicatedCode")
public interface StreamCodec<V> extends StreamDecoder<V>, StreamEncoder<V> {

  default <O> StreamCodec<O> map(Function<? super V, ? extends O> decodeMap, Function<? super O, ? extends V> encodeMap) {
    return new StreamCodec<>() {

      @Override
      public O decode(ByteBuf buf, ProtocolVersion version) {
        return decodeMap.apply(StreamCodec.this.decode(buf, version));
      }

      @Override
      public void encode(ByteBuf buf, ProtocolVersion version, O value) {
        StreamCodec.this.encode(buf, version, encodeMap.apply(value));
      }
    };
  }

  default <O> StreamCodec<O> map(BiFunction<? super V, ProtocolVersion, ? extends O> decodeMap, BiFunction<? super O, ProtocolVersion, ? extends V> encodeMap) {
    return new StreamCodec<>() {

      @Override
      public O decode(ByteBuf buf, ProtocolVersion version) {
        return decodeMap.apply(StreamCodec.this.decode(buf, version), version);
      }

      @Override
      public void encode(ByteBuf buf, ProtocolVersion version, O value) {
        StreamCodec.this.encode(buf, version, encodeMap.apply(value, version));
      }
    };
  }

  static <V> StreamCodec<V> unit(V unit) {
    return new StreamCodec<>() {

      @Override
      public V decode(ByteBuf buf, ProtocolVersion version) {
        return unit;
      }

      @Override
      public void encode(ByteBuf buf, ProtocolVersion version, V value) {
        if (!Objects.equals(value, unit)) {
          throw new IllegalStateException("Can't encode '" + value + "', expected '" + unit + "'");
        }
      }
    };
  }

  static <V> StreamCodec<V> eq(ProtocolVersion eq, StreamCodec<V> codec) {
    return StreamCodec.eq(eq, codec, null);
  }

  static <V> StreamCodec<V> eq(ProtocolVersion eq, StreamCodec<V> codec, @Nullable V fallback) {
    return new StreamCodec<>() {

      @Nullable
      @Override
      public V decode(ByteBuf buf, ProtocolVersion version) {
        return version == eq ? codec.decode(buf, version) : fallback;
      }

      @Override
      public void encode(ByteBuf buf, ProtocolVersion version, V value) {
        if (version == eq) {
          codec.encode(buf, version, value);
        }
      }
    };
  }

  static <V> StreamCodec<V> ne(ProtocolVersion ne, StreamCodec<V> codec) {
    return StreamCodec.ne(ne, codec, null);
  }

  static <V> StreamCodec<V> ne(ProtocolVersion ne, StreamCodec<V> codec, @Nullable V fallback) {
    return new StreamCodec<>() {

      @Nullable
      @Override
      public V decode(ByteBuf buf, ProtocolVersion version) {
        return version != ne ? codec.decode(buf, version) : fallback;
      }

      @Override
      public void encode(ByteBuf buf, ProtocolVersion version, V value) {
        if (version != ne) {
          codec.encode(buf, version, value);
        }
      }
    };
  }

  static <V> StreamCodec<V> gt(ProtocolVersion gt, StreamCodec<V> codec) {
    return StreamCodec.gt(gt, codec, null);
  }

  static <V> StreamCodec<V> gt(ProtocolVersion gt, StreamCodec<V> codec, @Nullable V fallback) {
    return new StreamCodec<>() {

      @Nullable
      @Override
      public V decode(ByteBuf buf, ProtocolVersion version) {
        return version.greaterThan(gt) ? codec.decode(buf, version) : fallback;
      }

      @Override
      public void encode(ByteBuf buf, ProtocolVersion version, V value) {
        if (version.greaterThan(gt)) {
          codec.encode(buf, version, value);
        }
      }
    };
  }

  static <V> StreamCodec<V> ge(ProtocolVersion ge, StreamCodec<V> codec) {
    return StreamCodec.ge(ge, codec, null);
  }

  static <V> StreamCodec<V> ge(ProtocolVersion ge, StreamCodec<V> codec, @Nullable V fallback) {
    return new StreamCodec<>() {

      @Nullable
      @Override
      public V decode(ByteBuf buf, ProtocolVersion version) {
        return version.noLessThan(ge) ? codec.decode(buf, version) : fallback;
      }

      @Override
      public void encode(ByteBuf buf, ProtocolVersion version, V value) {
        if (version.noLessThan(ge)) {
          codec.encode(buf, version, value);
        }
      }
    };
  }

  static <V> StreamCodec<V> lt(ProtocolVersion lt, StreamCodec<V> codec) {
    return StreamCodec.lt(lt, codec, (V) null);
  }

  static <V> StreamCodec<V> lt(ProtocolVersion lt, StreamCodec<V> codec, @Nullable V fallback) {
    return new StreamCodec<>() {

      @Nullable
      @Override
      public V decode(ByteBuf buf, ProtocolVersion version) {
        return version.lessThan(lt) ? codec.decode(buf, version) : fallback;
      }

      @Override
      public void encode(ByteBuf buf, ProtocolVersion version, V value) {
        if (version.lessThan(lt)) {
          codec.encode(buf, version, value);
        }
      }
    };
  }

  static <V> StreamCodec<? extends V> lt(ProtocolVersion lt, StreamCodec<? extends V> codec, StreamCodec<? extends V> otherwise) {
    return new StreamCodec<>() {

      @Override
      public V decode(ByteBuf buf, ProtocolVersion version) {
        return (version.lessThan(lt) ? codec : otherwise).decode(buf, version);
      }

      @Override
      @SuppressWarnings("unchecked")
      public void encode(ByteBuf buf, ProtocolVersion version, V value) {
        ((StreamCodec<V>) (version.lessThan(lt) ? codec : otherwise)).encode(buf, version, value);
      }
    };
  }

  static <V> StreamCodec<V> le(ProtocolVersion le, StreamCodec<V> codec) {
    return StreamCodec.le(le, codec, null);
  }

  static <V> StreamCodec<V> le(ProtocolVersion le, StreamCodec<V> codec, @Nullable V fallback) {
    return new StreamCodec<>() {

      @Nullable
      @Override
      public V decode(ByteBuf buf, ProtocolVersion version) {
        return version.noGreaterThan(le) ? codec.decode(buf, version) : fallback;
      }

      @Override
      public void encode(ByteBuf buf, ProtocolVersion version, V value) {
        if (version.noGreaterThan(le)) {
          codec.encode(buf, version, value);
        }
      }
    };
  }

  static <V, T1> StreamCodec<V> composite(
      StreamCodec<T1> codec1, Function<V, T1> getter1,
      Function<T1, V> constructor
  ) {
    return new StreamCodec<>() {

      @Override
      public V decode(ByteBuf buf, ProtocolVersion version) {
        return constructor.apply(codec1.decode(buf, version));
      }

      @Override
      public void encode(ByteBuf buf, ProtocolVersion version, V value) {
        codec1.encode(buf, version, getter1.apply(value));
      }
    };
  }

  static <V, T1, T2> StreamCodec<V> composite(
      StreamCodec<T1> codec1, Function<V, T1> getter1,
      StreamCodec<T2> codec2, Function<V, T2> getter2,
      BiFunction<T1, T2, V> constructor
  ) {
    return new StreamCodec<>() {

      @Override
      public V decode(ByteBuf buf, ProtocolVersion version) {
        return constructor.apply(codec1.decode(buf, version), codec2.decode(buf, version));
      }

      @Override
      public void encode(ByteBuf buf, ProtocolVersion version, V value) {
        codec1.encode(buf, version, getter1.apply(value));
        codec2.encode(buf, version, getter2.apply(value));
      }
    };
  }

  static <V, T1, T2, T3> StreamCodec<V> composite(
      StreamCodec<T1> codec1, Function<V, T1> getter1,
      StreamCodec<T2> codec2, Function<V, T2> getter2,
      StreamCodec<T3> codec3, Function<V, T3> getter3,
      Function3<T1, T2, T3, V> constructor
  ) {
    return new StreamCodec<>() {

      @Override
      public V decode(ByteBuf buf, ProtocolVersion version) {
        return constructor.apply(codec1.decode(buf, version), codec2.decode(buf, version), codec3.decode(buf, version));
      }

      @Override
      public void encode(ByteBuf buf, ProtocolVersion version, V value) {
        codec1.encode(buf, version, getter1.apply(value));
        codec2.encode(buf, version, getter2.apply(value));
        codec3.encode(buf, version, getter3.apply(value));
      }
    };
  }

  static <V, T1, T2, T3, T4> StreamCodec<V> composite(
      StreamCodec<T1> codec1, Function<V, T1> getter1,
      StreamCodec<T2> codec2, Function<V, T2> getter2,
      StreamCodec<T3> codec3, Function<V, T3> getter3,
      StreamCodec<T4> codec4, Function<V, T4> getter4,
      Function4<T1, T2, T3, T4, V> constructor
  ) {
    return new StreamCodec<>() {

      @Override
      public V decode(ByteBuf buf, ProtocolVersion version) {
        return constructor.apply(codec1.decode(buf, version), codec2.decode(buf, version), codec3.decode(buf, version), codec4.decode(buf, version));
      }

      @Override
      public void encode(ByteBuf buf, ProtocolVersion version, V value) {
        codec1.encode(buf, version, getter1.apply(value));
        codec2.encode(buf, version, getter2.apply(value));
        codec3.encode(buf, version, getter3.apply(value));
        codec4.encode(buf, version, getter4.apply(value));
      }
    };
  }

  static <V, T1, T2, T3, T4, T5> StreamCodec<V> composite(
      StreamCodec<T1> codec1, Function<V, T1> getter1,
      StreamCodec<T2> codec2, Function<V, T2> getter2,
      StreamCodec<T3> codec3, Function<V, T3> getter3,
      StreamCodec<T4> codec4, Function<V, T4> getter4,
      StreamCodec<T5> codec5, Function<V, T5> getter5,
      Function5<T1, T2, T3, T4, T5, V> constructor
  ) {
    return new StreamCodec<>() {

      @Override
      public V decode(ByteBuf buf, ProtocolVersion version) {
        return constructor.apply(codec1.decode(buf, version), codec2.decode(buf, version), codec3.decode(buf, version), codec4.decode(buf, version), codec5.decode(buf, version));
      }

      @Override
      public void encode(ByteBuf buf, ProtocolVersion version, V value) {
        codec1.encode(buf, version, getter1.apply(value));
        codec2.encode(buf, version, getter2.apply(value));
        codec3.encode(buf, version, getter3.apply(value));
        codec4.encode(buf, version, getter4.apply(value));
        codec5.encode(buf, version, getter5.apply(value));
      }
    };
  }

  static <V, T1, T2, T3, T4, T5, T6> StreamCodec<V> composite(
      StreamCodec<T1> codec1, Function<V, T1> getter1,
      StreamCodec<T2> codec2, Function<V, T2> getter2,
      StreamCodec<T3> codec3, Function<V, T3> getter3,
      StreamCodec<T4> codec4, Function<V, T4> getter4,
      StreamCodec<T5> codec5, Function<V, T5> getter5,
      StreamCodec<T6> codec6, Function<V, T6> getter6,
      Function6<T1, T2, T3, T4, T5, T6, V> constructor
  ) {
    return new StreamCodec<>() {

      @Override
      public V decode(ByteBuf buf, ProtocolVersion version) {
        return constructor.apply(
            codec1.decode(buf, version), codec2.decode(buf, version), codec3.decode(buf, version),
            codec4.decode(buf, version), codec5.decode(buf, version), codec6.decode(buf, version)
        );
      }

      @Override
      public void encode(ByteBuf buf, ProtocolVersion version, V value) {
        codec1.encode(buf, version, getter1.apply(value));
        codec2.encode(buf, version, getter2.apply(value));
        codec3.encode(buf, version, getter3.apply(value));
        codec4.encode(buf, version, getter4.apply(value));
        codec5.encode(buf, version, getter5.apply(value));
        codec6.encode(buf, version, getter6.apply(value));
      }
    };
  }

  static <V, T1, T2, T3, T4, T5, T6, T7> StreamCodec<V> composite(
      StreamCodec<T1> codec1, Function<V, T1> getter1,
      StreamCodec<T2> codec2, Function<V, T2> getter2,
      StreamCodec<T3> codec3, Function<V, T3> getter3,
      StreamCodec<T4> codec4, Function<V, T4> getter4,
      StreamCodec<T5> codec5, Function<V, T5> getter5,
      StreamCodec<T6> codec6, Function<V, T6> getter6,
      StreamCodec<T7> codec7, Function<V, T7> getter7,
      Function7<T1, T2, T3, T4, T5, T6, T7, V> constructor
  ) {
    return new StreamCodec<>() {

      @Override
      public V decode(ByteBuf buf, ProtocolVersion version) {
        return constructor.apply(
            codec1.decode(buf, version), codec2.decode(buf, version), codec3.decode(buf, version),
            codec4.decode(buf, version), codec5.decode(buf, version), codec6.decode(buf, version), codec7.decode(buf, version)
        );
      }

      @Override
      public void encode(ByteBuf buf, ProtocolVersion version, V value) {
        codec1.encode(buf, version, getter1.apply(value));
        codec2.encode(buf, version, getter2.apply(value));
        codec3.encode(buf, version, getter3.apply(value));
        codec4.encode(buf, version, getter4.apply(value));
        codec5.encode(buf, version, getter5.apply(value));
        codec6.encode(buf, version, getter6.apply(value));
        codec7.encode(buf, version, getter7.apply(value));
      }
    };
  }

  static <V, T1, T2, T3, T4, T5, T6, T7, T8> StreamCodec<V> composite(
      StreamCodec<T1> codec1, Function<V, T1> getter1,
      StreamCodec<T2> codec2, Function<V, T2> getter2,
      StreamCodec<T3> codec3, Function<V, T3> getter3,
      StreamCodec<T4> codec4, Function<V, T4> getter4,
      StreamCodec<T5> codec5, Function<V, T5> getter5,
      StreamCodec<T6> codec6, Function<V, T6> getter6,
      StreamCodec<T7> codec7, Function<V, T7> getter7,
      StreamCodec<T8> codec8, Function<V, T8> getter8,
      Function8<T1, T2, T3, T4, T5, T6, T7, T8, V> constructor
  ) {
    return new StreamCodec<>() {

      @Override
      public V decode(ByteBuf buf, ProtocolVersion version) {
        return constructor.apply(
            codec1.decode(buf, version), codec2.decode(buf, version), codec3.decode(buf, version), codec4.decode(buf, version),
            codec5.decode(buf, version), codec6.decode(buf, version), codec7.decode(buf, version), codec8.decode(buf, version)
        );
      }

      @Override
      public void encode(ByteBuf buf, ProtocolVersion version, V value) {
        codec1.encode(buf, version, getter1.apply(value));
        codec2.encode(buf, version, getter2.apply(value));
        codec3.encode(buf, version, getter3.apply(value));
        codec4.encode(buf, version, getter4.apply(value));
        codec5.encode(buf, version, getter5.apply(value));
        codec6.encode(buf, version, getter6.apply(value));
        codec7.encode(buf, version, getter7.apply(value));
        codec8.encode(buf, version, getter8.apply(value));
      }
    };
  }

  static <V, T1, T2, T3, T4, T5, T6, T7, T8, T9> StreamCodec<V> composite(
      StreamCodec<T1> codec1, Function<V, T1> getter1,
      StreamCodec<T2> codec2, Function<V, T2> getter2,
      StreamCodec<T3> codec3, Function<V, T3> getter3,
      StreamCodec<T4> codec4, Function<V, T4> getter4,
      StreamCodec<T5> codec5, Function<V, T5> getter5,
      StreamCodec<T6> codec6, Function<V, T6> getter6,
      StreamCodec<T7> codec7, Function<V, T7> getter7,
      StreamCodec<T8> codec8, Function<V, T8> getter8,
      StreamCodec<T9> codec9, Function<V, T9> getter9,
      Function9<T1, T2, T3, T4, T5, T6, T7, T8, T9, V> constructor
  ) {
    return new StreamCodec<>() {

      @Override
      public V decode(ByteBuf buf, ProtocolVersion version) {
        return constructor.apply(
            codec1.decode(buf, version), codec2.decode(buf, version), codec3.decode(buf, version), codec4.decode(buf, version),
            codec5.decode(buf, version), codec6.decode(buf, version), codec7.decode(buf, version), codec8.decode(buf, version), codec9.decode(buf, version)
        );
      }

      @Override
      public void encode(ByteBuf buf, ProtocolVersion version, V value) {
        codec1.encode(buf, version, getter1.apply(value));
        codec2.encode(buf, version, getter2.apply(value));
        codec3.encode(buf, version, getter3.apply(value));
        codec4.encode(buf, version, getter4.apply(value));
        codec5.encode(buf, version, getter5.apply(value));
        codec6.encode(buf, version, getter6.apply(value));
        codec7.encode(buf, version, getter7.apply(value));
        codec8.encode(buf, version, getter8.apply(value));
        codec9.encode(buf, version, getter9.apply(value));
      }
    };
  }

  static <V, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> StreamCodec<V> composite(
      StreamCodec<T1> codec1, Function<V, T1> getter1,
      StreamCodec<T2> codec2, Function<V, T2> getter2,
      StreamCodec<T3> codec3, Function<V, T3> getter3,
      StreamCodec<T4> codec4, Function<V, T4> getter4,
      StreamCodec<T5> codec5, Function<V, T5> getter5,
      StreamCodec<T6> codec6, Function<V, T6> getter6,
      StreamCodec<T7> codec7, Function<V, T7> getter7,
      StreamCodec<T8> codec8, Function<V, T8> getter8,
      StreamCodec<T9> codec9, Function<V, T9> getter9,
      StreamCodec<T10> codec10, Function<V, T10> getter10,
      Function10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, V> constructor
  ) {
    return new StreamCodec<>() {

      @Override
      public V decode(ByteBuf buf, ProtocolVersion version) {
        return constructor.apply(
            codec1.decode(buf, version), codec2.decode(buf, version), codec3.decode(buf, version), codec4.decode(buf, version), codec5.decode(buf, version),
            codec6.decode(buf, version), codec7.decode(buf, version), codec8.decode(buf, version), codec9.decode(buf, version), codec10.decode(buf, version)
        );
      }

      @Override
      public void encode(ByteBuf buf, ProtocolVersion version, V value) {
        codec1.encode(buf, version, getter1.apply(value));
        codec2.encode(buf, version, getter2.apply(value));
        codec3.encode(buf, version, getter3.apply(value));
        codec4.encode(buf, version, getter4.apply(value));
        codec5.encode(buf, version, getter5.apply(value));
        codec6.encode(buf, version, getter6.apply(value));
        codec7.encode(buf, version, getter7.apply(value));
        codec8.encode(buf, version, getter8.apply(value));
        codec9.encode(buf, version, getter9.apply(value));
        codec10.encode(buf, version, getter10.apply(value));
      }
    };
  }

  static <V, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> StreamCodec<V> composite(
      StreamCodec<T1> codec1, Function<V, T1> getter1,
      StreamCodec<T2> codec2, Function<V, T2> getter2,
      StreamCodec<T3> codec3, Function<V, T3> getter3,
      StreamCodec<T4> codec4, Function<V, T4> getter4,
      StreamCodec<T5> codec5, Function<V, T5> getter5,
      StreamCodec<T6> codec6, Function<V, T6> getter6,
      StreamCodec<T7> codec7, Function<V, T7> getter7,
      StreamCodec<T8> codec8, Function<V, T8> getter8,
      StreamCodec<T9> codec9, Function<V, T9> getter9,
      StreamCodec<T10> codec10, Function<V, T10> getter10,
      StreamCodec<T11> codec11, Function<V, T11> getter11,
      Function11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, V> constructor
  ) {
    return new StreamCodec<>() {

      @Override
      public V decode(ByteBuf buf, ProtocolVersion version) {
        return constructor.apply(
            codec1.decode(buf, version), codec2.decode(buf, version), codec3.decode(buf, version), codec4.decode(buf, version), codec5.decode(buf, version),
            codec6.decode(buf, version), codec7.decode(buf, version), codec8.decode(buf, version), codec9.decode(buf, version), codec10.decode(buf, version), codec11.decode(buf, version)
        );
      }

      @Override
      public void encode(ByteBuf buf, ProtocolVersion version, V value) {
        codec1.encode(buf, version, getter1.apply(value));
        codec2.encode(buf, version, getter2.apply(value));
        codec3.encode(buf, version, getter3.apply(value));
        codec4.encode(buf, version, getter4.apply(value));
        codec5.encode(buf, version, getter5.apply(value));
        codec6.encode(buf, version, getter6.apply(value));
        codec7.encode(buf, version, getter7.apply(value));
        codec8.encode(buf, version, getter8.apply(value));
        codec9.encode(buf, version, getter9.apply(value));
        codec10.encode(buf, version, getter10.apply(value));
        codec11.encode(buf, version, getter11.apply(value));
      }
    };
  }

  static <T> StreamCodec<T> recursive(UnaryOperator<StreamCodec<T>> initializer) {
    return new StreamCodec<>() {

      private final StreamCodec<T> inner = initializer.apply(this);

      @Override
      public T decode(ByteBuf buf, ProtocolVersion version) {
        return this.inner.decode(buf, version);
      }

      @Override
      public void encode(ByteBuf buf, ProtocolVersion version, T value) {
        this.inner.encode(buf, version, value);
      }
    };
  }
}
