package net.elytrium.limboapi.server.item.codec.data;

import com.velocitypowered.api.network.ProtocolVersion;
import io.netty.buffer.ByteBuf;
import net.elytrium.limboapi.api.protocol.data.BlockPos;
import net.elytrium.limboapi.protocol.codec.ByteBufCodecs;
import net.elytrium.limboapi.protocol.codec.StreamCodec;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface BlockPosCodec {

  StreamCodec<BlockPos> CODEC = new StreamCodec<>() {

    @Override
    public BlockPos decode(ByteBuf buf, ProtocolVersion version) {
      if (version.noGreaterThan(ProtocolVersion.MINECRAFT_1_8)) {
        return new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
      } else {
        long packed = buf.readLong();
        return version.noGreaterThan(ProtocolVersion.MINECRAFT_1_13_2)
            ? new BlockPos((int) (packed >> 38), (int) (packed << 26 >> 52), (int) (packed << 38 >> 38))
            : new BlockPos((int) (packed >> 38), (int) (packed << 52 >> 52), (int) (packed << 26 >> 38));
      }
    }

    @Override
    public void encode(ByteBuf buf, ProtocolVersion version, BlockPos value) {
      BlockPosCodec.encode(buf, version, value.posX(), value.posY(), value.posZ());
    }
  };
  StreamCodec<@Nullable BlockPos> OPTIONAL_CODEC = ByteBufCodecs.optional(BlockPosCodec.CODEC);

  static void encode(ByteBuf buf, ProtocolVersion version, int posX, int posY, int posZ) {
    if (version.noGreaterThan(ProtocolVersion.MINECRAFT_1_7_6)) {
      buf.writeInt(posX);
      buf.writeInt(posY);
      buf.writeInt(posZ);
    } else {
      buf.writeLong(version.noGreaterThan(ProtocolVersion.MINECRAFT_1_13_2)
          ? ((posX & 0x3FFFFFFL) << 38) | ((posY & 0xFFFL) << 26) | (posZ & 0x3FFFFFFL)
          : ((posX & 0x3FFFFFFL) << 38) | (posY & 0xFFFL) | ((posZ & 0x3FFFFFFL) << 12)
      );
    }
  }
}
