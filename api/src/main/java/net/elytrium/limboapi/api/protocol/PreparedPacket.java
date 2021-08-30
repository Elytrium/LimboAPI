/*
 * Copyright (C) 2021 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.protocol;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import java.util.List;
import java.util.function.Function;

public interface PreparedPacket {
  <T extends MinecraftPacket> PreparedPacket prepare(T packet);

  <T extends MinecraftPacket> PreparedPacket prepare(List<T> packets);

  <T extends MinecraftPacket> PreparedPacket prepare(T packet, ProtocolVersion from);

  <T extends MinecraftPacket> PreparedPacket prepare(T packet, ProtocolVersion from, ProtocolVersion to);

  <T extends MinecraftPacket> PreparedPacket prepare(Function<ProtocolVersion, T> packet);

  <T extends MinecraftPacket> PreparedPacket prepare(Function<ProtocolVersion, T> packet, ProtocolVersion from);

  <T extends MinecraftPacket> PreparedPacket prepare(
      Function<ProtocolVersion, T> packet, ProtocolVersion from, ProtocolVersion to);
}
