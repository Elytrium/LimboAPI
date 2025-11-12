package net.elytrium.limboapi.server.item.codec.data;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import net.elytrium.limboapi.api.world.item.datacomponent.type.data.HolderSet;
import net.elytrium.limboapi.protocol.codec.ByteBufCodecs;
import net.elytrium.limboapi.protocol.codec.StreamCodec;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface HolderSetCodec {

  StreamCodec<HolderSet> CODEC = new StreamCodec<>() {

    @Override
    public HolderSet decode(ByteBuf buf, ProtocolVersion version) {
      int amount = ProtocolUtils.readVarInt(buf) - 1;
      if (amount == -1) {
        return new HolderSet.Named(ProtocolUtils.readString(buf));
      }

      int[] blocks = new int[amount];
      for (int i = 0; i < amount; ++i) {
        blocks[i] = ProtocolUtils.readVarInt(buf);
      }

      return new HolderSet.Direct(blocks);
    }

    @Override
    public void encode(ByteBuf buf, ProtocolVersion version, HolderSet value) {
      if (value instanceof HolderSet.Named named) {
        ProtocolUtils.writeVarInt(buf, 0);
        ProtocolUtils.writeString(buf, named.key());
      } else if (value instanceof HolderSet.Direct direct) {
        int[] contents = direct.contents();
        ProtocolUtils.writeVarInt(buf, contents.length + 1);
        for (int block : contents) {
          ProtocolUtils.writeVarInt(buf, block);
        }
      } else {
        throw new IllegalArgumentException(value.getClass().getName());
      }
    }
  };
  StreamCodec<@Nullable HolderSet> OPTIONAL_CODEC = ByteBufCodecs.optional(HolderSetCodec.CODEC);
}
