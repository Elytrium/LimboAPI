package net.elytrium.limboapi.protocol.codec;

import com.velocitypowered.api.network.ProtocolVersion;
import io.netty.buffer.ByteBuf;

@FunctionalInterface
public interface StreamEncoder<T> {

  void encode(ByteBuf buf, ProtocolVersion version, T value);
}
