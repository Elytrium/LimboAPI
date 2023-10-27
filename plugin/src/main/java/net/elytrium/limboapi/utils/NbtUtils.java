package net.elytrium.limboapi.utils;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.handler.codec.EncoderException;
import java.io.IOException;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.CompoundBinaryTag;

// TODO: contribute to the Velocity?
public class NbtUtils {
  public static void writeCompoundTag(ByteBuf buf, CompoundBinaryTag tag, ProtocolVersion version) {
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_20_2) >= 0) {
      try {
        buf.writeByte(BinaryTagTypes.COMPOUND.id());
        BinaryTagTypes.COMPOUND.write(tag, new ByteBufOutputStream(buf));
      } catch (IOException exception) {
        throw new EncoderException("Unable to encode NBT CompoundTag", exception);
      }
    } else {
      ProtocolUtils.writeCompoundTag(buf, tag);
    }
  }
}
