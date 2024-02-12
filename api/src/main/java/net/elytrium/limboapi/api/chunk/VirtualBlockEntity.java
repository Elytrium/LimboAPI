/*
 * Copyright (C) 2021 - 2024 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.chunk;

import com.velocitypowered.api.network.ProtocolVersion;
import net.kyori.adventure.nbt.CompoundBinaryTag;

public interface VirtualBlockEntity {

  int getID(ProtocolVersion version);

  int getID(BlockEntityVersion version);

  boolean isSupportedOn(ProtocolVersion version);

  boolean isSupportedOn(BlockEntityVersion version);

  String getModernID();

  Entry getEntry(int posX, int posY, int posZ, CompoundBinaryTag nbt);

  interface Entry {

    VirtualBlockEntity getBlockEntity();

    int getPosX();

    int getPosY();

    int getPosZ();

    CompoundBinaryTag getNbt();

    int getID(ProtocolVersion version);

    int getID(BlockEntityVersion version);

    boolean isSupportedOn(ProtocolVersion version);

    boolean isSupportedOn(BlockEntityVersion version);
  }
}
