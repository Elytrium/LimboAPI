/*
 * Copyright (C) 2021 - 2024 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.protocol;

import com.velocitypowered.api.network.ProtocolVersion;
import java.util.List;
import java.util.function.Function;

public interface PreparedPacket {

  <T> PreparedPacket prepare(T packet);

  <T> PreparedPacket prepare(T[] packets);

  <T> PreparedPacket prepare(List<T> packets);

  <T> PreparedPacket prepare(T packet, ProtocolVersion from);

  <T> PreparedPacket prepare(T packet, ProtocolVersion from, ProtocolVersion to);

  <T> PreparedPacket prepare(T[] packets, ProtocolVersion from);

  <T> PreparedPacket prepare(T[] packets, ProtocolVersion from, ProtocolVersion to);

  <T> PreparedPacket prepare(List<T> packets, ProtocolVersion from);

  <T> PreparedPacket prepare(List<T> packets, ProtocolVersion from, ProtocolVersion to);

  <T> PreparedPacket prepare(Function<ProtocolVersion, T> packet);

  <T> PreparedPacket prepare(Function<ProtocolVersion, T> packet, ProtocolVersion from);

  <T> PreparedPacket prepare(Function<ProtocolVersion, T> packet, ProtocolVersion from, ProtocolVersion to);

  PreparedPacket build();

  void release();
}
