package net.elytrium.limboapi.server.item.codec.data;

import com.velocitypowered.api.network.ProtocolVersion;
import io.netty.buffer.ByteBuf;
import net.elytrium.limboapi.api.world.item.datacomponent.type.data.Either;
import net.elytrium.limboapi.protocol.codec.StreamCodec;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface EitherCodec {

  static <L, R> StreamCodec<Either<L, R>> codec(StreamCodec<L> leftCodec, StreamCodec<R> rightCodec) {
    return new StreamCodec<>() {

      @Override
      public Either<L, R> decode(ByteBuf buf, ProtocolVersion version) {
        return buf.readBoolean() ? Either.left(leftCodec.decode(buf, version)) : Either.right(rightCodec.decode(buf, version));
      }

      @Override
      public void encode(ByteBuf buf, ProtocolVersion version, Either<L, R> value) {
        value.ifLeft(left -> {
          buf.writeBoolean(true);
          leftCodec.encode(buf, version, left);
        }).ifRight(right -> {
          buf.writeBoolean(false);
          rightCodec.encode(buf, version, right);
        });
      }
    };
  }
}
