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

package net.elytrium.limboapi.server.item;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.velocitypowered.api.network.ProtocolVersion;
import it.unimi.dsi.fastutil.Function;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import net.elytrium.limboapi.LimboAPI;
import net.elytrium.limboapi.server.item.type.BooleanItemComponent;
import net.elytrium.limboapi.server.item.type.ComponentItemComponent;
import net.elytrium.limboapi.server.item.type.ComponentsItemComponent;
import net.elytrium.limboapi.server.item.type.DyedColorItemComponent;
import net.elytrium.limboapi.server.item.type.EmptyItemComponent;
import net.elytrium.limboapi.server.item.type.EnchantmentsItemComponent;
import net.elytrium.limboapi.server.item.type.GameProfileItemComponent;
import net.elytrium.limboapi.server.item.type.IntItemComponent;
import net.elytrium.limboapi.server.item.type.StringItemComponent;
import net.elytrium.limboapi.server.item.type.StringsItemComponent;
import net.elytrium.limboapi.server.item.type.TagItemComponent;
import net.elytrium.limboapi.server.item.type.VarIntItemComponent;
import net.elytrium.limboapi.server.item.type.WriteableItemComponent;

public class SimpleItemComponentManager {

  private static final Gson GSON = new Gson();

  private static final Map<ProtocolVersion, Object2IntMap<String>> ID = new HashMap<>();

  static {
    LinkedTreeMap<String, LinkedTreeMap<String, String>> mapping = GSON.fromJson(
        new InputStreamReader(
            Objects.requireNonNull(LimboAPI.class.getResourceAsStream("/mapping/data_component_types_mapping.json")),
            StandardCharsets.UTF_8
        ),
        LinkedTreeMap.class
    );

    LinkedTreeMap<String, String> components = GSON.fromJson(
        new InputStreamReader(
            Objects.requireNonNull(LimboAPI.class.getResourceAsStream("/mapping/data_component_types.json")),
            StandardCharsets.UTF_8
        ),
        LinkedTreeMap.class
    );

    Map<String, ProtocolVersion> cache = new HashMap<>();
    for (ProtocolVersion version : ProtocolVersion.values()) {
      if (version.compareTo(ProtocolVersion.MINECRAFT_1_20_5) < 0) {
        continue;
      }

      cache.put(version.name().substring("MINECRAFT_".length()).replace('_', '.'), version);
    }

    components.forEach((name, id) -> {
      mapping.get(id).forEach((version, protocolId) -> {
        ID.computeIfAbsent(cache.get(version), key -> new Object2IntOpenHashMap<>()).put(name, Integer.parseInt(protocolId));
      });
    });
  }

  private final Map<String, Function<ProtocolVersion, WriteableItemComponent>> factory = new HashMap<>();

  public SimpleItemComponentManager() {
    // TODO: implement missing components:
    //  trim, intangible_projectile, food, suspicious_stew_effects, lock, tool,
    //  can_break, writable_book_content, potion_contents, bees, banner_patterns,
    //  pot_decorations, map_decorations, debug_stick_state, can_place_on, lodestone_tracker,
    //  written_book_content, container_loot, container, block_state, attribute_modifiers,
    //  bundle_contents, firework_explosion, charged_projectiles, fireworks
    this.register("minecraft:lore", version -> new ComponentsItemComponent("minecraft:lore"));
    this.register("minecraft:dyed_color", version -> new DyedColorItemComponent("minecraft:dyed_color"));
    this.register("minecraft:profile", version -> new GameProfileItemComponent("minecraft:profile"));

    for (String type : new String[] { "minecraft:max_stack_size", "minecraft:max_damage",
                                      "minecraft:damage", "minecraft:rarity", "minecraft:custom_model_data",
                                      "minecraft:repair_cost", "minecraft:map_id", "minecraft:map_post_processing",
                                      "minecraft:ominous_bottle_amplifier", "minecraft:base_color" }) {
      this.register(type, version -> new VarIntItemComponent(type));
    }

    for (String type : new String[] { "minecraft:unbreakable", "minecraft:enchantment_glint_override" }) {
      this.register(type, version -> new BooleanItemComponent(type));
    }

    for (String type : new String[] { "minecraft:custom_name", "minecraft:item_name" }) {
      this.register(type, version -> new ComponentItemComponent(type));
    }

    for (String type : new String[] { "minecraft:hide_additional_tooltip", "minecraft:hide_tooltip",
                                      "minecraft:creative_slot_lock", "minecraft:fire_resistant" }) {
      this.register(type, version -> new EmptyItemComponent(type));
    }

    for (String type : new String[] { "minecraft:enchantments", "minecraft:stored_enchantments" }) {
      this.register(type, version -> new EnchantmentsItemComponent(type));
    }

    for (String type : new String[] { "minecraft:map_color" }) {
      this.register(type, version -> new IntItemComponent(type));
    }

    for (String type : new String[] { "minecraft:custom_data", "minecraft:entity_data",
                                      "minecraft:bucket_entity_data", "minecraft:block_entity_data" }) {
      this.register(type, version -> new TagItemComponent(type));
    }

    for (String type : new String[] { "minecraft:instrument", "minecraft:note_block_sound" }) {
      this.register(type, version -> new StringItemComponent(type));
    }

    for (String type : new String[] { "minecraft:recipes" }) {
      this.register(type, version -> new StringsItemComponent(type));
    }
  }

  public <T> void register(String name, Function<ProtocolVersion, WriteableItemComponent> factory) {
    this.factory.put(name, factory);
  }

  public <T> WriteableItemComponent<T> createComponent(ProtocolVersion version, String name) {
    return (WriteableItemComponent<T>) this.factory.get(name).apply(version);
  }

  public int getId(String name, ProtocolVersion version) {
    Object2IntMap<String> ids = ID.get(version);
    if (ids == null) {
      throw new IllegalArgumentException("LimboAPI item components do not support this protocol version: " + version);
    }

    if (!ids.containsKey(name)) {
      throw new IllegalStateException("component not found: " + name);
    }

    return ids.getInt(name);
  }
}
