package net.elytrium.limboapi.protocol.codec;

import com.velocitypowered.api.network.ProtocolVersion;
import io.netty.buffer.ByteBuf;

@FunctionalInterface
public interface StreamDecoder<T> {

  T decode(ByteBuf buf, ProtocolVersion version);
}
