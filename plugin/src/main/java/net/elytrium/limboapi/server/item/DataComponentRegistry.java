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

import com.google.gson.internal.LinkedTreeMap;
import com.velocitypowered.api.network.ProtocolVersion;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.elytrium.commons.utils.reflection.ReflectionException;
import net.elytrium.limboapi.LimboAPI;
import net.elytrium.limboapi.api.world.item.datacomponent.DataComponentType;
import net.elytrium.limboapi.api.world.item.datacomponent.DataComponentTypes;
import net.elytrium.limboapi.api.world.item.datacomponent.type.Unbreakable;
import net.elytrium.limboapi.protocol.codec.ByteBufCodecs;
import net.elytrium.limboapi.protocol.codec.StreamCodec;
import net.elytrium.limboapi.server.item.codec.AdventureModePredicateCodec;
import net.elytrium.limboapi.server.item.codec.ArmorTrimCodec;
import net.elytrium.limboapi.server.item.codec.AttributeModifiersCodec;
import net.elytrium.limboapi.server.item.codec.BannerPatternLayerCodec;
import net.elytrium.limboapi.server.item.codec.BeehiveOccupantCodec;
import net.elytrium.limboapi.server.item.codec.BlocksAttacksCodec;
import net.elytrium.limboapi.server.item.codec.ConsumableCodec;
import net.elytrium.limboapi.server.item.codec.CustomModelDataCodec;
import net.elytrium.limboapi.server.item.codec.DyedColorCodec;
import net.elytrium.limboapi.server.item.codec.EnchantmentsCodec;
import net.elytrium.limboapi.server.item.codec.EquippableCodec;
import net.elytrium.limboapi.server.item.codec.FireworkExplosionCodec;
import net.elytrium.limboapi.server.item.codec.FireworksCodec;
import net.elytrium.limboapi.server.item.codec.FoodPropertiesCodec;
import net.elytrium.limboapi.server.item.codec.InstrumentCodec;
import net.elytrium.limboapi.server.item.codec.JukeboxPlayableCodec;
import net.elytrium.limboapi.server.item.codec.LodestoneTrackerCodec;
import net.elytrium.limboapi.server.item.codec.PaintingVariantCodec;
import net.elytrium.limboapi.server.item.codec.PotionContentsCodec;
import net.elytrium.limboapi.server.item.codec.ResolvableProfileCodec;
import net.elytrium.limboapi.server.item.codec.SuspiciousStewEffectCodec;
import net.elytrium.limboapi.server.item.codec.ToolCodec;
import net.elytrium.limboapi.server.item.codec.TooltipDisplayCodec;
import net.elytrium.limboapi.server.item.codec.UseCooldownCodec;
import net.elytrium.limboapi.server.item.codec.WeaponCodec;
import net.elytrium.limboapi.server.item.codec.WrittenBookContentCodec;
import net.elytrium.limboapi.server.item.codec.data.ComponentHolderCodec;
import net.elytrium.limboapi.server.item.codec.data.ConsumeEffectCodec;
import net.elytrium.limboapi.server.item.codec.data.EitherCodec;
import net.elytrium.limboapi.server.item.codec.data.FilterableCodec;
import net.elytrium.limboapi.server.item.codec.data.HolderSetCodec;
import net.elytrium.limboapi.server.item.codec.data.ItemStackCodec;
import net.elytrium.limboapi.server.item.codec.data.SoundEventCodec;
import net.elytrium.limboapi.server.item.codec.data.TrimMaterialCodec;
import net.elytrium.limboapi.server.item.codec.data.TypedEntityDataCodec;
import net.elytrium.limboapi.utils.JsonUtil;
import net.elytrium.limboapi.utils.Reflection;
import net.elytrium.limboapi.utils.Unit;
import org.checkerframework.checker.nullness.qual.NonNull;

public class DataComponentRegistry {

  private static final EnumMap<ProtocolVersion, Map.Entry<Int2ObjectOpenHashMap<DataComponentTypeImpl<?>>, Object2IntOpenHashMap<DataComponentType>>> REGISTRY = new EnumMap<>(ProtocolVersion.class);

  static {
    var mappings = JsonUtil.<LinkedTreeMap<String, Number>>parse(LimboAPI.class.getResourceAsStream("/mappings/data_component_types_mappings.json"));

    Field[] fields = DataComponentTypes.class.getDeclaredFields();
    Map<String, MethodHandle> types = new HashMap<>(fields.length);
    for (Field field : fields) {
      String name = field.getName();
      try {
        types.put(name, Reflection.LOOKUP.findStaticSetter(DataComponentTypes.class, name, field.getType()));
      } catch (NoSuchFieldException | IllegalAccessException e) {
        throw new ReflectionException(e);
      }
    }

    mappings.forEach((name, versions) -> {
      DataComponentTypeImpl<?> type;
      try {
        types.get(name.replace('/', '_').substring(10/*"minecraft:".length()*/).toUpperCase(Locale.US)).invoke(type = new DataComponentTypeImpl<>(name));
      } catch (Throwable t) {
        throw new ReflectionException(name, t);
      }

      versions.forEach((version, _id) -> {
        var entry = DataComponentRegistry.REGISTRY.computeIfAbsent(ProtocolVersion.getProtocolVersion(Integer.parseInt(version)), key -> {
          Object2IntOpenHashMap<DataComponentType> t2i = new Object2IntOpenHashMap<>(mappings.size());
          t2i.defaultReturnValue(Integer.MIN_VALUE);
          return Map.entry(new Int2ObjectOpenHashMap<>(mappings.size()), t2i);
        });
        int id = _id.intValue();
        entry.getKey().put(id, type);
        entry.getValue().put(type, id);
      });
    });
    DataComponentRegistry.REGISTRY.values().forEach(entry -> {
      entry.getKey().trim();
      entry.getValue().trim();
    });

    DataComponentRegistry.register(ByteBufCodecs.COMPOUND_TAG,
        DataComponentTypes.CUSTOM_DATA, DataComponentTypes.INTANGIBLE_PROJECTILE, DataComponentTypes.MAP_DECORATIONS,
        DataComponentTypes.DEBUG_STICK_STATE, DataComponentTypes.BUCKET_ENTITY_DATA, DataComponentTypes.CONTAINER_LOOT
    );
    DataComponentRegistry.register(ByteBufCodecs.VAR_INT,
        DataComponentTypes.MAX_STACK_SIZE, DataComponentTypes.MAX_DAMAGE, DataComponentTypes.DAMAGE, DataComponentTypes.RARITY, DataComponentTypes.REPAIR_COST,
        DataComponentTypes.ENCHANTABLE, DataComponentTypes.MAP_ID, DataComponentTypes.MAP_POST_PROCESSING, DataComponentTypes.OMINOUS_BOTTLE_AMPLIFIER, DataComponentTypes.BASE_COLOR,
        DataComponentTypes.VILLAGER_VARIANT, DataComponentTypes.WOLF_VARIANT, DataComponentTypes.WOLF_SOUND_VARIANT, DataComponentTypes.WOLF_COLLAR, DataComponentTypes.FOX_VARIANT,
        DataComponentTypes.SALMON_SIZE, DataComponentTypes.PARROT_VARIANT, DataComponentTypes.TROPICAL_FISH_PATTERN, DataComponentTypes.TROPICAL_FISH_BASE_COLOR,
        DataComponentTypes.TROPICAL_FISH_PATTERN_COLOR, DataComponentTypes.MOOSHROOM_VARIANT, DataComponentTypes.RABBIT_VARIANT, DataComponentTypes.PIG_VARIANT,
        DataComponentTypes.COW_VARIANT, DataComponentTypes.FROG_VARIANT, DataComponentTypes.HORSE_VARIANT, DataComponentTypes.LLAMA_VARIANT, DataComponentTypes.AXOLOTL_VARIANT,
        DataComponentTypes.CAT_VARIANT, DataComponentTypes.CAT_COLLAR, DataComponentTypes.SHEEP_COLOR, DataComponentTypes.SHULKER_COLOR
    );
    DataComponentRegistry.register(StreamCodec.lt(ProtocolVersion.MINECRAFT_1_21_5, ByteBufCodecs.BOOL.map(value -> value ? Unbreakable.TRUE : Unbreakable.FALSE, Unbreakable::showInTooltip), Unbreakable.TRUE),
        DataComponentTypes.UNBREAKABLE
    );
    DataComponentRegistry.register(ComponentHolderCodec.CODEC, DataComponentTypes.CUSTOM_NAME, DataComponentTypes.ITEM_NAME);
    DataComponentRegistry.register(ByteBufCodecs.STRING_UTF8,
        DataComponentTypes.ITEM_MODEL, DataComponentTypes.DAMAGE_RESISTANT, DataComponentTypes.TOOLTIP_STYLE,
        DataComponentTypes.PROVIDES_BANNER_PATTERNS, DataComponentTypes.NOTE_BLOCK_SOUND
    );
    DataComponentRegistry.register(ByteBufCodecs.collection(ComponentHolderCodec.CODEC, 256), DataComponentTypes.LORE);
    DataComponentRegistry.register(EnchantmentsCodec.CODEC, DataComponentTypes.ENCHANTMENTS, DataComponentTypes.STORED_ENCHANTMENTS);
    DataComponentRegistry.register(AdventureModePredicateCodec.CODEC, DataComponentTypes.CAN_PLACE_ON, DataComponentTypes.CAN_BREAK);
    DataComponentRegistry.register(AttributeModifiersCodec.CODEC, DataComponentTypes.ATTRIBUTE_MODIFIERS);
    DataComponentRegistry.register(CustomModelDataCodec.CODEC, DataComponentTypes.CUSTOM_MODEL_DATA);
    DataComponentRegistry.register(Unit.CODEC,
        DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP, DataComponentTypes.HIDE_TOOLTIP,
        DataComponentTypes.CREATIVE_SLOT_LOCK, DataComponentTypes.FIRE_RESISTANT, DataComponentTypes.GLIDER
    );
    DataComponentRegistry.register(ByteBufCodecs.BOOL, DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE);
    DataComponentRegistry.register(TooltipDisplayCodec.CODEC, DataComponentTypes.TOOLTIP_DISPLAY);
    DataComponentRegistry.register(FoodPropertiesCodec.CODEC, DataComponentTypes.FOOD);
    DataComponentRegistry.register(ConsumableCodec.CODEC, DataComponentTypes.CONSUMABLE);
    DataComponentRegistry.register(ItemStackCodec.CODEC, DataComponentTypes.USE_REMAINDER);
    DataComponentRegistry.register(UseCooldownCodec.CODEC, DataComponentTypes.USE_COOLDOWN);
    DataComponentRegistry.register(ToolCodec.CODEC, DataComponentTypes.TOOL);
    DataComponentRegistry.register(WeaponCodec.CODEC, DataComponentTypes.WEAPON);
    DataComponentRegistry.register(EquippableCodec.CODEC, DataComponentTypes.EQUIPPABLE);
    DataComponentRegistry.register(HolderSetCodec.CODEC, DataComponentTypes.REPAIRABLE);
    DataComponentRegistry.register(ConsumeEffectCodec.COLLECTION_CODEC, DataComponentTypes.DEATH_PROTECTION);
    DataComponentRegistry.register(BlocksAttacksCodec.CODEC, DataComponentTypes.BLOCKS_ATTACKS);
    DataComponentRegistry.register(DyedColorCodec.CODEC, DataComponentTypes.DYED_COLOR);
    DataComponentRegistry.register(ByteBufCodecs.INT, DataComponentTypes.MAP_COLOR);
    DataComponentRegistry.register(ByteBufCodecs.collection(ItemStackCodec.CODEC), DataComponentTypes.CHARGED_PROJECTILES, DataComponentTypes.BUNDLE_CONTENTS);
    DataComponentRegistry.register(PotionContentsCodec.CODEC, DataComponentTypes.POTION_CONTENTS);
    DataComponentRegistry.register(ByteBufCodecs.FLOAT, DataComponentTypes.POTION_DURATION_SCALE);
    DataComponentRegistry.register(ByteBufCodecs.collection(SuspiciousStewEffectCodec.CODEC), DataComponentTypes.SUSPICIOUS_STEW_EFFECTS);
    DataComponentRegistry.register(ByteBufCodecs.collection(FilterableCodec.codec(ByteBufCodecs.STRING_UTF8_1024, ByteBufCodecs.OPTIONAL_STRING_UTF8_1024), 100), DataComponentTypes.WRITABLE_BOOK_CONTENT);
    DataComponentRegistry.register(WrittenBookContentCodec.CODEC, DataComponentTypes.WRITTEN_BOOK_CONTENT);
    DataComponentRegistry.register(ArmorTrimCodec.CODEC, DataComponentTypes.TRIM);
    DataComponentRegistry.register(TypedEntityDataCodec.TYPED_ENTITY_DATA_CODEC, DataComponentTypes.ENTITY_DATA, DataComponentTypes.BLOCK_ENTITY_DATA);
    DataComponentRegistry.register(InstrumentCodec.EITHER_CODEC, DataComponentTypes.INSTRUMENT);
    DataComponentRegistry.register(EitherCodec.codec(TrimMaterialCodec.HOLDER_CODEC, ByteBufCodecs.STRING_UTF8), DataComponentTypes.PROVIDES_TRIM_MATERIAL);
    DataComponentRegistry.register(JukeboxPlayableCodec.CODEC, DataComponentTypes.JUKEBOX_PLAYABLE);
    DataComponentRegistry.register(ByteBufCodecs.TAG, DataComponentTypes.RECIPES);
    DataComponentRegistry.register(LodestoneTrackerCodec.CODEC, DataComponentTypes.LODESTONE_TRACKER);
    DataComponentRegistry.register(FireworkExplosionCodec.CODEC, DataComponentTypes.FIREWORK_EXPLOSION);
    DataComponentRegistry.register(FireworksCodec.CODEC, DataComponentTypes.FIREWORKS);
    DataComponentRegistry.register(ResolvableProfileCodec.CODEC, DataComponentTypes.PROFILE);
    DataComponentRegistry.register(ByteBufCodecs.collection(BannerPatternLayerCodec.CODEC), DataComponentTypes.BANNER_PATTERNS);
    DataComponentRegistry.register(ByteBufCodecs.collection(ByteBufCodecs.VAR_INT, 4), DataComponentTypes.POT_DECORATIONS);
    DataComponentRegistry.register(ByteBufCodecs.collection(ItemStackCodec.OPTIONAL_CODEC, 256), DataComponentTypes.CONTAINER);
    DataComponentRegistry.register(ByteBufCodecs.STRING_2_STRING_MAP, DataComponentTypes.BLOCK_STATE);
    DataComponentRegistry.register(ByteBufCodecs.collection(BeehiveOccupantCodec.CODEC), DataComponentTypes.BEES);
    DataComponentRegistry.register(StreamCodec.lt(ProtocolVersion.MINECRAFT_1_21_2, ByteBufCodecs.TAG, ByteBufCodecs.COMPOUND_TAG), DataComponentTypes.LOCK);
    DataComponentRegistry.register(SoundEventCodec.HOLDER_CODEC, DataComponentTypes.BREAK_SOUND);
    DataComponentRegistry.register(EitherCodec.codec(ByteBufCodecs.VAR_INT, ByteBufCodecs.STRING_UTF8), DataComponentTypes.CHICKEN_VARIANT);
    DataComponentRegistry.register(PaintingVariantCodec.HOLDER_CODEC, DataComponentTypes.PAINTING_VARIANT);
  }

  @SafeVarargs
  @SuppressWarnings("unchecked")
  private static <T> void register(StreamCodec<T> codec, DataComponentType.Valued<@NonNull ? extends T>... types) {
    for (DataComponentType type : types) {
      var impl = (DataComponentTypeImpl<T>) type;
      if (impl.codec != null) {
        throw new IllegalStateException("Codec already registered");
      }

      impl.codec = codec;
    }
  }

  @SuppressWarnings("rawtypes")
  private static void register(StreamCodec<?> codec, DataComponentType.NonValued... types) {
    for (DataComponentType type : types) {
      var impl = (DataComponentTypeImpl) type;
      if (impl.codec != null) {
        throw new IllegalStateException("Codec already registered");
      }

      impl.codec = codec;
    }
  }

  public static DataComponentType getType(int id, ProtocolVersion version) {
    var type = DataComponentRegistry.REGISTRY.get(version).getKey().get(id);
    if (type == null) {
      throw new IllegalStateException("Component not found: " + id);
    }

    return type;
  }

  public static int getId(DataComponentType type, ProtocolVersion version) {
    return DataComponentRegistry.REGISTRY.get(version).getValue().getInt(type);
  }

  public static final class DataComponentTypeImpl<T> implements DataComponentType.Valued<@NonNull T>, DataComponentType.NonValued {

    private final String name;

    private StreamCodec<T> codec;

    private DataComponentTypeImpl(String name) {
      this.name = name;
    }

    public StreamCodec<T> codec() {
      return this.codec;
    }

    @Override
    public String toString() {
      return this.name;
    }
  }
}
