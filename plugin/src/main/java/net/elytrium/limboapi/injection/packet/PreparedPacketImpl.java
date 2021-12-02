/*
 * Copyright (C) 2021 Elytrium
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

package net.elytrium.limboapi.injection.packet;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.ProtocolUtils.Direction;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.elytrium.limboapi.LimboAPI;
import net.elytrium.limboapi.api.protocol.PreparedPacket;
import net.elytrium.limboapi.protocol.LimboProtocol;

public class PreparedPacketImpl implements PreparedPacket {

  private final Map<ProtocolVersion, List<ByteBuf>> packets = new ConcurrentHashMap<>();

  @Override
  public <T extends MinecraftPacket> PreparedPacketImpl prepare(T packet) {
    return this.prepare((Function<ProtocolVersion, T>) (version) -> packet, ProtocolVersion.MINIMUM_VERSION, ProtocolVersion.MAXIMUM_VERSION);
  }

  @Override
  public <T extends MinecraftPacket> PreparedPacket prepare(T[] packets) {
    return this.prepare(Arrays.asList(packets));
  }

  @Override
  public <T extends MinecraftPacket> PreparedPacketImpl prepare(List<T> packets) {
    for (T packet : packets) {
      this.prepare((Function<ProtocolVersion, T>) (version) -> packet, ProtocolVersion.MINIMUM_VERSION, ProtocolVersion.MAXIMUM_VERSION);
    }

    return this;
  }

  @Override
  public <T extends MinecraftPacket> PreparedPacketImpl prepare(T packet, ProtocolVersion from) {
    return this.prepare((Function<ProtocolVersion, T>) (version) -> packet, from, ProtocolVersion.MAXIMUM_VERSION);
  }

  @Override
  public <T extends MinecraftPacket> PreparedPacketImpl prepare(T packet, ProtocolVersion from, ProtocolVersion to) {
    return this.prepare((Function<ProtocolVersion, T>) (version) -> packet, from, to);
  }

  @Override
  public <T extends MinecraftPacket> PreparedPacket prepare(T[] packets, ProtocolVersion from) {
    return this.prepare(Arrays.asList(packets), from);
  }

  @Override
  public <T extends MinecraftPacket> PreparedPacket prepare(T[] packets, ProtocolVersion from, ProtocolVersion to) {
    return this.prepare(Arrays.asList(packets), from, to);
  }

  @Override
  public <T extends MinecraftPacket> PreparedPacket prepare(List<T> packets, ProtocolVersion from) {
    for (T packet : packets) {
      this.prepare(packet, from, ProtocolVersion.MAXIMUM_VERSION);
    }

    return this;
  }

  @Override
  public <T extends MinecraftPacket> PreparedPacket prepare(List<T> packets, ProtocolVersion from, ProtocolVersion to) {
    for (T packet : packets) {
      this.prepare(packet, from, to);
    }

    return this;
  }

  @Override
  public <T extends MinecraftPacket> PreparedPacketImpl prepare(Function<ProtocolVersion, T> packet) {
    return this.prepare(packet, ProtocolVersion.MINIMUM_VERSION, ProtocolVersion.MAXIMUM_VERSION);
  }

  @Override
  public <T extends MinecraftPacket> PreparedPacketImpl prepare(Function<ProtocolVersion, T> packet, ProtocolVersion from) {
    return this.prepare(packet, from, ProtocolVersion.MAXIMUM_VERSION);
  }

  @Override
  public <T extends MinecraftPacket> PreparedPacketImpl prepare(Function<ProtocolVersion, T> packet, ProtocolVersion from, ProtocolVersion to) {
    for (ProtocolVersion protocolVersion : EnumSet.range(from, to)) {
      ByteBuf buf = this.encodePacket(packet.apply(protocolVersion), protocolVersion);
      if (this.packets.containsKey(protocolVersion)) {
        this.packets.get(protocolVersion).add(buf);
      } else {
        List<ByteBuf> list = new ArrayList<>();
        list.add(buf);
        this.packets.put(protocolVersion, list);
      }
    }

    return this;
  }

  public List<ByteBuf> getPackets(ProtocolVersion version) {
    return this.packets.get(version).stream().map(ByteBuf::copy).collect(Collectors.toList());
  }

  public boolean hasPacketsFor(ProtocolVersion version) {
    return this.packets.containsKey(version);
  }

  private <T extends MinecraftPacket> ByteBuf encodePacket(T packet, ProtocolVersion version) {
    int id = this.getPacketId(packet, version);
    if (id == Integer.MIN_VALUE) {
      LimboAPI.getInstance().getLogger().error("Bad packet id {}:{}", id, packet.getClass().getSimpleName());
    }
    ByteBuf byteBuf = Unpooled.buffer();
    ProtocolUtils.writeVarInt(byteBuf, id);
    packet.encode(byteBuf, Direction.CLIENTBOUND, version);
    return byteBuf.capacity(byteBuf.readableBytes());
  }

  @SuppressWarnings("unchecked")
  private <T extends MinecraftPacket> int getPacketId(T packet, ProtocolVersion version) {
    try {
      return LimboProtocol.getPacketId(LimboProtocol.getLimboRegistry().clientbound, packet.getClass(), version);
    } catch (Exception e) {
      return LimboProtocol.getPacketId(
          LimboProtocol.getLimboRegistry().clientbound, (Class<? extends MinecraftPacket>) packet.getClass().getSuperclass(), version
      );
    }
  }

  @Override
  public void decode(ByteBuf buf, Direction direction, ProtocolVersion protocolVersion) {

  }

  @Override
  public void encode(ByteBuf buf, Direction direction, ProtocolVersion protocolVersion) {

  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return false;
  }
}
