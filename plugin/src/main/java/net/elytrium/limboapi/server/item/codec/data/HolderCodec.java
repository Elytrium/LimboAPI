package net.elytrium.limboapi.server.item.codec.data;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import net.elytrium.limboapi.api.world.item.datacomponent.type.data.Holder;
import net.elytrium.limboapi.protocol.codec.StreamCodec;
import org.jspecify.annotations.NullMarked;

@NullMarked
@SuppressWarnings("unchecked")
public interface HolderCodec {

  static <T extends Holder.Direct<T>> StreamCodec<Holder<T>> codec(StreamCodec<T> codec) {
    return new StreamCodec<>() {

      @Override
      public Holder<T> decode(ByteBuf buf, ProtocolVersion version) {
        int i = ProtocolUtils.readVarInt(buf);
        return i == 0 ? codec.decode(buf, version) : Holder.ref(i - 1);
      }

      @Override
      public void encode(ByteBuf buf, ProtocolVersion version, Holder<T> value) {
        if (value instanceof Holder.Reference<?> reference) {
          ProtocolUtils.writeVarInt(buf, reference.id() + 1);
        } else if (value instanceof Holder.Direct<T> direct) {
          ProtocolUtils.writeVarInt(buf, 0);
          codec.encode(buf, version, (T) direct);
        } else {
          throw new IllegalArgumentException(value.getClass().getName());
        }
      }
    };
  }
}
