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
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.ProtocolUtils.Direction;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.elytrium.limboapi.LimboAPI;
import net.elytrium.limboapi.protocol.LimboProtocol;

public class PreparedPacket {

  private final Map<ProtocolVersion, List<ByteBuf>> packets = new ConcurrentHashMap<>();

  public <T extends MinecraftPacket> PreparedPacket prepare(T packet) {
    return prepare((Function<ProtocolVersion, T>) (version) ->
        packet, ProtocolVersion.MINIMUM_VERSION, ProtocolVersion.MAXIMUM_VERSION);
  }

  public <T extends MinecraftPacket> PreparedPacket prepare(List<T> packets) {
    for (T packet : packets) {
      prepare((Function<ProtocolVersion, T>) (version) ->
          packet, ProtocolVersion.MINIMUM_VERSION, ProtocolVersion.MAXIMUM_VERSION);
    }

    return this;
  }

  public <T extends MinecraftPacket> PreparedPacket prepare(T packet, ProtocolVersion from) {
    return prepare((Function<ProtocolVersion, T>) (version) -> packet, from, ProtocolVersion.MAXIMUM_VERSION);
  }

  public <T extends MinecraftPacket> PreparedPacket prepare(T packet, ProtocolVersion from, ProtocolVersion to) {
    return prepare((Function<ProtocolVersion, T>) (version) -> packet, from, to);
  }

  public <T extends MinecraftPacket> PreparedPacket prepare(Function<ProtocolVersion, T> packet) {
    return prepare(packet, ProtocolVersion.MINIMUM_VERSION, ProtocolVersion.MAXIMUM_VERSION);
  }

  public <T extends MinecraftPacket> PreparedPacket prepare(Function<ProtocolVersion, T> packet, ProtocolVersion from) {
    return prepare(packet, from, ProtocolVersion.MAXIMUM_VERSION);
  }

  public <T extends MinecraftPacket> PreparedPacket prepare(Function<ProtocolVersion, T> packet,
      ProtocolVersion from, ProtocolVersion to) {
    for (ProtocolVersion protocolVersion : EnumSet.range(from, to)) {
      ByteBuf buf = encodePacket(packet.apply(protocolVersion), protocolVersion);
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
    int id = getPacketId(packet, version);
    if (id == Integer.MIN_VALUE) {
      LimboAPI.getInstance().getLogger().error("Bad packet id. {}", packet.getClass().getSimpleName());
    }
    ByteBuf byteBuf = Unpooled.buffer();
    ProtocolUtils.writeVarInt(byteBuf, id);
    packet.encode(byteBuf, Direction.CLIENTBOUND, version);
    return byteBuf;
  }

  @SuppressWarnings("unchecked")
  private <T extends MinecraftPacket> int getPacketId(T packet, ProtocolVersion version) {
    try {
      return LimboProtocol.getPacketId(LimboProtocol.getLimboRegistry().clientbound, packet.getClass(), version);
    } catch (Exception e) {
      return LimboProtocol.getPacketId(LimboProtocol.getLimboRegistry().clientbound,
          (Class<? extends MinecraftPacket>) packet.getClass().getSuperclass(), version);
    }
  }
}
