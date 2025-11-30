package net.elytrium.limboapi.protocol.codec;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.ByteBufUtil;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.BinaryTagType;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.EndBinaryTag;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jspecify.annotations.NullMarked;

public interface ByteBufCodecs {

  StreamCodec<Boolean> BOOL = new StreamCodec<>() {

    @Override
    public Boolean decode(ByteBuf buf, ProtocolVersion version) {
      return buf.readBoolean();
    }

    @Override
    public void encode(ByteBuf buf, ProtocolVersion version, Boolean value) {
      buf.writeBoolean(value);
    }
  };
  StreamCodec<Byte> BYTE = new StreamCodec<>() {

    @Override
    public Byte decode(ByteBuf buf, ProtocolVersion version) {
      return buf.readByte();
    }

    @Override
    public void encode(ByteBuf buf, ProtocolVersion version, Byte value) {
      buf.writeByte(value);
    }
  };
  StreamCodec<Short> SHORT = new StreamCodec<>() {

    @Override
    public Short decode(ByteBuf buf, ProtocolVersion version) {
      return buf.readShort();
    }

    @Override
    public void encode(ByteBuf buf, ProtocolVersion version, Short value) {
      buf.writeShort(value);
    }
  };
  StreamCodec<Integer> INT = new StreamCodec<>() {

    @Override
    public Integer decode(ByteBuf buf, ProtocolVersion version) {
      return buf.readInt();
    }

    @Override
    public void encode(ByteBuf buf, ProtocolVersion version, Integer value) {
      buf.writeInt(value);
    }
  };
  StreamCodec<Collection<Integer>> INT_COLLECTION = ByteBufCodecs.collection(ByteBufCodecs.INT);
  StreamCodec<Integer> VAR_INT = new StreamCodec<>() {

    @Override
    public Integer decode(ByteBuf buf, ProtocolVersion version) {
      byte readByte = buf.readByte();
      if ((readByte & 0x80) != 0x80) {
        return (int) readByte;
      }

      int result = readByte & 0x7F;
      int bitsRead = 7;
      do {
        readByte = buf.readByte();
        result |= (readByte & 0x7F) << bitsRead;
        bitsRead += 7;
        if (bitsRead > 5 * 7) {
          throw new CorruptedFrameException("VarInt too big");
        }
      } while ((readByte & 0x80) == 0x80);
      return result;
    }

    @Override
    public void encode(ByteBuf buf, ProtocolVersion version, Integer value) {
      ProtocolUtils.writeVarInt(buf, value);
    }
  };
  StreamCodec<Long> VAR_LONG = new StreamCodec<>() {

    @Override
    public Long decode(ByteBuf buf, ProtocolVersion version) {
      byte readByte = buf.readByte();
      if ((readByte & 0x80) != 0x80) {
        return (long) readByte;
      }

      long result = readByte & 0x7F;
      int bitsRead = 7;
      do {
        readByte = buf.readByte();
        result |= (readByte & 0x7FL) << bitsRead;
        bitsRead += 7;
        if (bitsRead > 10 * 7) {
          throw new CorruptedFrameException("VarLong too big");
        }
      } while ((readByte & 0x80) == 0x80);
      return result;
    }

    @Override
    public void encode(ByteBuf buf, ProtocolVersion version, Long valueObj) {
      long value = valueObj;
      while ((value & ~0x7F) != 0L) {
        buf.writeByte((int) (value & 0x7FL) | 0x80);
        value >>>= 7;
      }

      buf.writeByte((int) value);
    }
  };
  StreamCodec<Float> FLOAT = new StreamCodec<>() {

    @Override
    public Float decode(ByteBuf buf, ProtocolVersion version) {
      return buf.readFloat();
    }

    @Override
    public void encode(ByteBuf buf, ProtocolVersion version, Float value) {
      buf.writeFloat(value);
    }
  };
  StreamCodec<Double> DOUBLE = new StreamCodec<>() {

    @Override
    public Double decode(ByteBuf buf, ProtocolVersion version) {
      return buf.readDouble();
    }

    @Override
    public void encode(ByteBuf buf, ProtocolVersion version, Double value) {
      buf.writeDouble(value);
    }
  };

  StreamCodec<UUID> UUID = new StreamCodec<>() {

    @Override
    public UUID decode(ByteBuf buf, ProtocolVersion version) {
      return ProtocolUtils.readUuid(buf);
    }

    @Override
    public void encode(ByteBuf buf, ProtocolVersion version, UUID value) {
      ProtocolUtils.writeUuid(buf, value);
    }
  };
  StreamCodec<@Nullable UUID> OPTIONAL_UUID = ByteBufCodecs.optional(ByteBufCodecs.UUID);

  StreamCodec<Integer> CONTAINER_ID = new StreamCodec<>() {

    @Override
    public Integer decode(ByteBuf buf, ProtocolVersion version) {
      return version.noLessThan(ProtocolVersion.MINECRAFT_1_21_2) ? ProtocolUtils.readVarInt(buf) : buf.readUnsignedByte();
    }

    @Override
    public void encode(ByteBuf buf, ProtocolVersion version, Integer value) {
      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_21_2)) {
        ProtocolUtils.writeVarInt(buf, value);
      } else {
        buf.writeByte(value & 0xFF);
      }
    }
  };

  StreamCodec<String> STRING_UTF8 = ByteBufCodecs.stringUtf8(Short.MAX_VALUE);
  StreamCodec<@Nullable String> OPTIONAL_STRING_UTF8 = ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8);

  StreamCodec<String> STRING_UTF8_1024 = ByteBufCodecs.stringUtf8(1024);
  StreamCodec<@Nullable String> OPTIONAL_STRING_UTF8_1024 = ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8_1024);

  StreamCodec<String> PLAYER_NAME = ByteBufCodecs.stringUtf8(16);
  StreamCodec<@Nullable String> OPTIONAL_PLAYER_NAME = ByteBufCodecs.optional(ByteBufCodecs.PLAYER_NAME);

  StreamCodec<@Nullable BinaryTag> OPTIONAL_TAG = new StreamCodec<>() {

    //<editor-fold defaultstate="collapsed" desc="static">
    private static final BinaryTagType<? extends BinaryTag>[] BINARY_TAG_TYPES;

    static {
      try {
        Field field = ProtocolUtils.class.getDeclaredField("BINARY_TAG_TYPES");
        field.setAccessible(true);
        BINARY_TAG_TYPES = (BinaryTagType<? extends BinaryTag>[]) field.get(null);
      } catch (NoSuchFieldException | IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="decode">
    @Nullable
    @Override
    public BinaryTag decode(ByteBuf buf, ProtocolVersion version) {
      if (version.noGreaterThan(ProtocolVersion.MINECRAFT_1_7_6)) {
        short length = buf.readShort();
        if (length < 0) {
          return null;
        } else {
          try (DataInputStream inputStream = new DataInputStream(new BufferedInputStream(new GZIPInputStream(new ByteBufInputStream(buf, length))))) {
            BinaryTagType<?> type = BINARY_TAG_TYPES[inputStream.readByte()];
            inputStream.skipBytes(inputStream.readUnsignedShort());
            return type.read(inputStream);
          } catch (IOException e) {
            throw new DecoderException(e);
          }
        }
      } else {
        byte type = buf.readByte();
        if (type == 0) {
          return null;
        } else {
          if (version.lessThan(ProtocolVersion.MINECRAFT_1_20_2)) {
            buf.skipBytes(buf.readUnsignedShort());
          }

          try {
            return BINARY_TAG_TYPES[type].read(new ByteBufInputStream(buf));
          } catch (IOException e) {
            throw new DecoderException("Unable to parse BinaryTag", e);
          }
        }
      }
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="encode">
    @Override
    @SuppressWarnings("unchecked")
    public void encode(ByteBuf buf, ProtocolVersion version, BinaryTag value) {
      if (version.noGreaterThan(ProtocolVersion.MINECRAFT_1_7_6)) {
        if (value == null || value instanceof EndBinaryTag) {
          buf.writeShort(-1);
        } else {
          buf.writeShort(0);
          int preIndex = buf.writerIndex();
          try (DataOutputStream output = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(new ByteBufOutputStream(buf))))) {
            var type = (BinaryTagType<BinaryTag>) value.type();
            output.writeByte(type.id());
            output.writeShort(0); // writeUTF("")
            type.write(value, output);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
          int postIndex = buf.writerIndex();
          buf.writerIndex(preIndex - Short.BYTES);
          buf.writeShort(postIndex - preIndex);
          buf.writerIndex(postIndex);
        }
      } else if (value == null || value instanceof EndBinaryTag) {
        buf.writeByte(0);
      } else {
        ProtocolUtils.writeBinaryTag(buf, version, value);
      }
    }
    //</editor-fold>
  };
  StreamCodec<BinaryTag> TAG = ByteBufCodecs.OPTIONAL_TAG.map(tag -> {
    if (tag == null) {
      throw new DecoderException("Expected non-null binary tag");
    } else {
      return tag;
    }
  }, tag -> {
    if (tag == null || tag instanceof EndBinaryTag) {
      throw new EncoderException("Expected non-null binary tag");
    } else {
      return tag;
    }
  });
  StreamCodec<@Nullable CompoundBinaryTag> OPTIONAL_COMPOUND_TAG = ByteBufCodecs.OPTIONAL_TAG.map(tag -> {
    if (tag == null || tag instanceof CompoundBinaryTag) {
      return (CompoundBinaryTag) tag;
    } else {
      throw new DecoderException("Not a compound tag: " + tag);
    }
  }, Function.identity());
  StreamCodec<CompoundBinaryTag> COMPOUND_TAG = ByteBufCodecs.TAG.map(tag -> {
    if (tag instanceof CompoundBinaryTag compound) {
      return compound;
    } else {
      throw new EncoderException("Not a compound tag: " + tag);
    }
  }, Function.identity());

  StreamCodec<Map<String, String>> STRING_2_STRING_MAP = ByteBufCodecs.map(Object2ObjectOpenHashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.STRING_UTF8);

  static StreamCodec<String> stringUtf8(int capacity) {
    return new StreamCodec<>() {

      @Override
      public String decode(ByteBuf buf, ProtocolVersion version) {
        int length = ProtocolUtils.readVarInt(buf);
        int utf8MaxBytes = ByteBufUtil.utf8MaxBytes(capacity);
        if (length > utf8MaxBytes) {
          throw new DecoderException("The received encoded string buffer length is longer than maximum allowed (" + length + " > " + utf8MaxBytes + ")");
        } else if (length < 0) {
          throw new DecoderException("The received encoded string buffer length is less than zero! Weird string!");
        } else {
          int readableBytes = buf.readableBytes();
          if (length > readableBytes) {
            throw new DecoderException("Not enough bytes in buffer, expected " + length + ", but got " + readableBytes);
          } else {
            String result = buf.readString(length, StandardCharsets.UTF_8);
            if (result.length() > capacity) {
              throw new DecoderException("The received string length is longer than maximum allowed (" + result.length() + " > " + capacity + ")");
            } else {
              return result;
            }
          }
        }
      }

      @Override
      public void encode(ByteBuf buf, ProtocolVersion version, String value) {
        // https://github.com/astei/krypton/blob/v0.2.8/src/main/java/me/steinborn/krypton/mixin/shared/network/microopt/StringEncodingMixin.java#L20
        if (value.length() > capacity) {
          throw new EncoderException("String too big (was " + value.length() + " characters, max " + capacity + ")");
        } else {
          int utf8Bytes = ByteBufUtil.utf8Bytes(value);
          int utf8MaxBytes = ByteBufUtil.utf8MaxBytes(capacity);
          if (utf8Bytes > utf8MaxBytes) {
            throw new EncoderException("String too big (was " + utf8Bytes + " bytes encoded, max " + utf8MaxBytes + ")");
          } else {
            ProtocolUtils.writeVarInt(buf, utf8Bytes);
            buf.writeCharSequence(value, StandardCharsets.UTF_8);
          }
        }
      }
    };
  }

  static <V> StreamCodec<@Nullable V> optional(StreamCodec<@Nullable V> codec) {
    return new StreamCodec<>() {

      @Nullable
      @Override
      public V decode(ByteBuf buf, ProtocolVersion version) {
        return buf.readBoolean() ? codec.decode(buf, version) : null;
      }

      @Override
      public void encode(ByteBuf buf, ProtocolVersion version, @Nullable V value) {
        if (value == null) {
          buf.writeBoolean(false);
        } else {
          buf.writeBoolean(true);
          codec.encode(buf, version, value);
        }
      }
    };
  }

  static <V> StreamCodec<List<V>> list(StreamCodec<V> codec) {
    return ByteBufCodecs.collection(ArrayList::new, codec, Integer.MAX_VALUE);
  }

  static <V> StreamCodec<List<V>> list(StreamCodec<V> codec, int max) {
    return ByteBufCodecs.collection(ArrayList::new, codec, max);
  }

  static <V> StreamCodec<Collection<V>> collection(StreamCodec<V> codec) {
    return ByteBufCodecs.collection(ArrayList::new, codec, Integer.MAX_VALUE);
  }

  static <V> StreamCodec<Collection<V>> collection(StreamCodec<V> codec, int max) {
    return ByteBufCodecs.collection(ArrayList::new, codec, max);
  }

  static <V, C extends Collection<V>> StreamCodec<C> collection(IntFunction<? extends C> constructor, StreamCodec<V> codec) {
    return ByteBufCodecs.collection(constructor, codec, Integer.MAX_VALUE);
  }

  static <V, C extends Collection<V>> StreamCodec<C> collection(IntFunction<? extends C> constructor, StreamCodec<V> codec, int max) {
    return new StreamCodec<>() {

      @Override
      public C decode(ByteBuf buf, ProtocolVersion version) {
        int count = ByteBufCodecs.readCount(buf, max);
        C result = constructor.apply(count);
        for (int i = 0; i < count; ++i) {
          result.add(codec.decode(buf, version));
        }

        return result;
      }

      @Override
      public void encode(ByteBuf buf, ProtocolVersion version, C value) {
        int size = value.size();
        ByteBufCodecs.writeCount(buf, size, max);
        if (size > 0) {
          value.forEach(object -> codec.encode(buf, version, object));
        }
      }
    };
  }

  static <K, V, M extends Map<K, V>> StreamCodec<M> map(IntFunction<? extends M> constructor, StreamCodec<K> keyCodec, StreamCodec<V> valueCodec) {
    return ByteBufCodecs.map(constructor, keyCodec, valueCodec, Integer.MAX_VALUE);
  }

  static <K, V, M extends Map<K, V>> StreamCodec<M> map(IntFunction<? extends M> constructor, StreamCodec<K> keyCodec, StreamCodec<V> valueCodec, int max) {
    return new StreamCodec<>() {

      @Override
      public M decode(ByteBuf buf, ProtocolVersion version) {
        int count = ByteBufCodecs.readCount(buf, max);
        M map = constructor.apply(count);
        for (int i = 0; i < count; ++i) {
          map.put(keyCodec.decode(buf, version), valueCodec.decode(buf, version));
        }

        return map;
      }

      @Override
      public void encode(ByteBuf buf, ProtocolVersion version, M map) {
        int size = map.size();
        ByteBufCodecs.writeCount(buf, size, max);
        if (size > 0) {
          map.forEach((key, value) -> {
            keyCodec.encode(buf, version, key);
            valueCodec.encode(buf, version, value);
          });
        }
      }
    };
  }

  static int readCount(ByteBuf buf, int max) {
    int count = ProtocolUtils.readVarInt(buf);
    if (count > max) {
      throw new DecoderException(count + " elements exceeded max size of: " + max);
    } else {
      return count;
    }
  }

  static void writeCount(ByteBuf buf, int count, int max) {
    if (count > max) {
      throw new EncoderException(count + " elements exceeded max size of: " + max);
    } else {
      ProtocolUtils.writeVarInt(buf, count);
    }
  }
}
