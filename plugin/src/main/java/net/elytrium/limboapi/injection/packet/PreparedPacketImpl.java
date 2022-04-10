/*
 * Copyright (C) 2021 - 2022 Elytrium
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
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import net.elytrium.limboapi.LimboAPI;
import net.elytrium.limboapi.api.protocol.PreparedPacket;
import net.elytrium.limboapi.protocol.LimboProtocol;
import org.slf4j.Logger;

public class PreparedPacketImpl implements PreparedPacket {

  private final Map<ProtocolVersion, List<ByteBuf>> packets = new ConcurrentHashMap<>();
  private final ProtocolVersion minVersion;
  private final ProtocolVersion maxVersion;

  private final Logger logger;

  public PreparedPacketImpl(ProtocolVersion minVersion, ProtocolVersion maxVersion, LimboAPI plugin) {
    this.minVersion = minVersion;
    this.maxVersion = maxVersion;
    this.logger = plugin.getLogger();
  }

  @Override
  public <T> PreparedPacketImpl prepare(T packet) {
    if (packet == null) {
      return this;
    }

    return this.prepare((version) -> packet, ProtocolVersion.MINIMUM_VERSION, ProtocolVersion.MAXIMUM_VERSION);
  }

  @Override
  public <T> PreparedPacketImpl prepare(T[] packets) {
    return this.prepare(Arrays.asList(packets));
  }

  @Override
  public <T> PreparedPacketImpl prepare(List<T> packets) {
    if (packets == null) {
      return this;
    }

    for (T packet : packets) {
      this.prepare((version) -> packet, ProtocolVersion.MINIMUM_VERSION, ProtocolVersion.MAXIMUM_VERSION);
    }

    return this;
  }

  @Override
  public <T> PreparedPacketImpl prepare(T packet, ProtocolVersion from) {
    if (packet == null) {
      return this;
    }

    return this.prepare((version) -> packet, from, ProtocolVersion.MAXIMUM_VERSION);
  }

  @Override
  public <T> PreparedPacketImpl prepare(T packet, ProtocolVersion from, ProtocolVersion to) {
    if (packet == null) {
      return this;
    }

    return this.prepare((version) -> packet, from, to);
  }

  @Override
  public <T> PreparedPacketImpl prepare(T[] packets, ProtocolVersion from) {
    return this.prepare(Arrays.asList(packets), from);
  }

  @Override
  public <T> PreparedPacketImpl prepare(T[] packets, ProtocolVersion from, ProtocolVersion to) {
    return this.prepare(Arrays.asList(packets), from, to);
  }

  @Override
  public <T> PreparedPacketImpl prepare(List<T> packets, ProtocolVersion from) {
    if (packets == null) {
      return this;
    }

    for (T packet : packets) {
      this.prepare(packet, from);
    }

    return this;
  }

  @Override
  public <T> PreparedPacketImpl prepare(List<T> packets, ProtocolVersion from, ProtocolVersion to) {
    if (packets == null) {
      return this;
    }

    for (T packet : packets) {
      this.prepare(packet, from, to);
    }

    return this;
  }

  @Override
  public <T> PreparedPacketImpl prepare(Function<ProtocolVersion, T> packet) {
    return this.prepare(packet, ProtocolVersion.MINIMUM_VERSION, ProtocolVersion.MAXIMUM_VERSION);
  }

  @Override
  public <T> PreparedPacketImpl prepare(Function<ProtocolVersion, T> packet, ProtocolVersion from) {
    return this.prepare(packet, from, ProtocolVersion.MAXIMUM_VERSION);
  }

  @Override
  public <T> PreparedPacketImpl prepare(Function<ProtocolVersion, T> packet, ProtocolVersion originalFrom, ProtocolVersion originalTo) {
    ProtocolVersion from = originalFrom.compareTo(this.minVersion) > 0 ? originalFrom : this.minVersion;
    ProtocolVersion to = originalTo.compareTo(this.maxVersion) < 0 ? originalTo : this.maxVersion;
    if (from.compareTo(to) > 0) {
      return this;
    }
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
    return this.packets.get(version);
  }

  public boolean hasPacketsFor(ProtocolVersion version) {
    return this.packets.containsKey(version);
  }

  private <T> ByteBuf encodePacket(T packet, ProtocolVersion version) {
    int id = this.getPacketId(packet, version);
    if (id == Integer.MIN_VALUE) {
      this.logger.error("Bad packet id {}:{}", id, packet.getClass().getSimpleName());
    }
    ByteBuf byteBuf = Unpooled.buffer();
    ProtocolUtils.writeVarInt(byteBuf, id);
    ((MinecraftPacket) packet).encode(byteBuf, Direction.CLIENTBOUND, version);
    return byteBuf.capacity(byteBuf.readableBytes());
  }

  @SuppressWarnings("unchecked")
  private <T> int getPacketId(T packet, ProtocolVersion version) {
    try {
      return LimboProtocol.getPacketId(LimboProtocol.getLimboRegistry().clientbound, ((MinecraftPacket) packet).getClass(), version);
    } catch (Exception e) {
      return LimboProtocol.getPacketId(
          LimboProtocol.getLimboRegistry().clientbound, (Class<? extends MinecraftPacket>) packet.getClass().getSuperclass(), version
      );
    }
  }
}

