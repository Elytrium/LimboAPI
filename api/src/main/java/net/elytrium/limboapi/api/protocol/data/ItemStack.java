/*
 * Copyright (C) 2021 - 2024 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.protocol.data;

import com.velocitypowered.api.network.ProtocolVersion;
import net.elytrium.limboapi.api.world.WorldVersion;
import net.elytrium.limboapi.api.world.item.VirtualItem;
import net.elytrium.limboapi.api.world.item.datacomponent.DataComponentMap;
import net.elytrium.limboapi.api.world.item.datacomponent.DataComponentTypes;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.checkerframework.checker.nullness.qual.Nullable;

public record ItemStack(VirtualItem material, int amount, int data, @Nullable CompoundBinaryTag nbt, @Nullable DataComponentMap map) implements EntityData.Particle.ParticleOptions {

  public static final ItemStack EMPTY = new ItemStack(null, 0, 0, null, null);

  public ItemStack(VirtualItem material, int amount, @Nullable CompoundBinaryTag nbt) {
    this(material, amount, nbt == null ? 0 : nbt.getInt("Damage", 0), nbt, null);
  }

  public ItemStack(VirtualItem material, int amount, @Nullable DataComponentMap map) {
    this(material, amount, map == null ? 0 : map.getDataOrDefault(DataComponentTypes.DAMAGE, 0), map);
  }

  public ItemStack(VirtualItem material, int amount, int data) {
    this(material, amount, data, null, null);
  }

  public ItemStack(VirtualItem material, int amount, int data, @Nullable CompoundBinaryTag nbt) {
    this(material, amount, data, nbt, null);
  }

  public ItemStack(VirtualItem material, int amount, int data, @Nullable DataComponentMap map) {
    this(material, amount, data, null, map);
  }

  public boolean isEmpty(ProtocolVersion version, boolean checkDamage) {
    return this.isEmpty(WorldVersion.from(version), checkDamage);
  }

  public boolean isEmpty(WorldVersion version, boolean checkDamage) {
    return this == ItemStack.EMPTY || this.material.itemId(version) <= 0 || this.amount <= 0 || (checkDamage && (this.data < -32768 || this.data > 65535));
  }
}
