/*
 * Copyright (C) 2021 - 2024 Elytrium
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
import java.util.List;
import java.util.function.Function;
import net.elytrium.fastprepare.PreparedPacket;
import net.elytrium.fastprepare.PreparedPacketFactory;

public class PreparedPacketImpl extends PreparedPacket implements net.elytrium.limboapi.api.protocol.PreparedPacket {

  public PreparedPacketImpl(ProtocolVersion minVersion, ProtocolVersion maxVersion, PreparedPacketFactory factory) {
    super(minVersion, maxVersion, factory);
  }

  @Override
  public <T> PreparedPacketImpl prepare(T packet) {
    return (PreparedPacketImpl) super.prepare(packet);
  }

  @Override
  public <T> PreparedPacketImpl prepare(T[] packets) {
    return (PreparedPacketImpl) super.prepare(packets);
  }

  @Override
  public <T> PreparedPacketImpl prepare(List<T> packets) {
    return (PreparedPacketImpl) super.prepare(packets);
  }

  @Override
  public <T> PreparedPacketImpl prepare(T packet, ProtocolVersion from) {
    return (PreparedPacketImpl) super.prepare(packet, from);
  }

  @Override
  public <T> PreparedPacketImpl prepare(T packet, ProtocolVersion from, ProtocolVersion to) {
    return (PreparedPacketImpl) super.prepare(packet, from, to);
  }

  @Override
  public <T> PreparedPacketImpl prepare(T[] packets, ProtocolVersion from) {
    return (PreparedPacketImpl) super.prepare(packets, from);
  }

  @Override
  public <T> PreparedPacketImpl prepare(T[] packets, ProtocolVersion from, ProtocolVersion to) {
    return (PreparedPacketImpl) super.prepare(packets, from, to);
  }

  @Override
  public <T> PreparedPacketImpl prepare(List<T> packets, ProtocolVersion from) {
    return (PreparedPacketImpl) super.prepare(packets, from);
  }

  @Override
  public <T> PreparedPacketImpl prepare(List<T> packets, ProtocolVersion from, ProtocolVersion to) {
    return (PreparedPacketImpl) super.prepare(packets, from, to);
  }

  @Override
  public <T> PreparedPacketImpl prepare(Function<ProtocolVersion, T> packet) {
    return (PreparedPacketImpl) super.prepare(packet);
  }

  @Override
  public <T> PreparedPacketImpl prepare(Function<ProtocolVersion, T> packet, ProtocolVersion from) {
    return (PreparedPacketImpl) super.prepare(packet, from);
  }

  @Override
  public <T> PreparedPacketImpl prepare(Function<ProtocolVersion, T> packet, ProtocolVersion from, ProtocolVersion to) {
    return (PreparedPacketImpl) super.prepare(packet, from, to);
  }

  @Override
  public PreparedPacketImpl build() {
    return (PreparedPacketImpl) super.build();
  }

  @Override
  public void release() {
    super.release();
  }
}
