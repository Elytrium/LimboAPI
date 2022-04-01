/*
 * Copyright (C) 2021 - 2022 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.protocol.packets;

import com.velocitypowered.api.network.ProtocolVersion;
import org.checkerframework.checker.nullness.qual.Nullable;

public class PacketMapping {

  private final int id;
  private final ProtocolVersion protocolVersion;
  private final boolean encodeOnly;
  @Nullable
  private final ProtocolVersion lastValidProtocolVersion;

  public PacketMapping(int id, ProtocolVersion protocolVersion, @Nullable ProtocolVersion lastValidProtocolVersion, boolean packetDecoding) {
    this.id = id;
    this.protocolVersion = protocolVersion;
    this.lastValidProtocolVersion = lastValidProtocolVersion;
    this.encodeOnly = packetDecoding;
  }

  public int getId() {
    return this.id;
  }

  public ProtocolVersion getProtocolVersion() {
    return this.protocolVersion;
  }

  public boolean isEncodeOnly() {
    return this.encodeOnly;
  }

  @Nullable
  public ProtocolVersion getLastValidProtocolVersion() {
    return this.lastValidProtocolVersion;
  }
}
