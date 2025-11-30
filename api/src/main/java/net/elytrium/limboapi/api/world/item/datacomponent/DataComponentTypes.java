package net.elytrium.limboapi.api.world.item.datacomponent;

import java.util.Collection;
import java.util.Map;
import net.elytrium.limboapi.api.protocol.data.ComponentHolder;
import net.elytrium.limboapi.api.protocol.data.ItemStack;
import net.elytrium.limboapi.api.world.item.datacomponent.type.AdventureModePredicate;
import net.elytrium.limboapi.api.world.item.datacomponent.type.ArmorTrim;
import net.elytrium.limboapi.api.world.item.datacomponent.type.AttributeModifiers;
import net.elytrium.limboapi.api.world.item.datacomponent.type.BannerPatternLayer;
import net.elytrium.limboapi.api.world.item.datacomponent.type.BeehiveOccupant;
import net.elytrium.limboapi.api.world.item.datacomponent.type.BlocksAttacks;
import net.elytrium.limboapi.api.world.item.datacomponent.type.Consumable;
import net.elytrium.limboapi.api.world.item.datacomponent.type.CustomModelData;
import net.elytrium.limboapi.api.world.item.datacomponent.type.DyedColor;
import net.elytrium.limboapi.api.world.item.datacomponent.type.Enchantments;
import net.elytrium.limboapi.api.world.item.datacomponent.type.Equippable;
import net.elytrium.limboapi.api.world.item.datacomponent.type.FireworkExplosion;
import net.elytrium.limboapi.api.world.item.datacomponent.type.Fireworks;
import net.elytrium.limboapi.api.world.item.datacomponent.type.FoodProperties;
import net.elytrium.limboapi.api.world.item.datacomponent.type.Instrument;
import net.elytrium.limboapi.api.world.item.datacomponent.type.JukeboxPlayable;
import net.elytrium.limboapi.api.world.item.datacomponent.type.LodestoneTracker;
import net.elytrium.limboapi.api.world.item.datacomponent.type.PaintingVariant;
import net.elytrium.limboapi.api.world.item.datacomponent.type.PotionContents;
import net.elytrium.limboapi.api.world.item.datacomponent.type.ResolvableProfile;
import net.elytrium.limboapi.api.world.item.datacomponent.type.SuspiciousStewEffect;
import net.elytrium.limboapi.api.world.item.datacomponent.type.Tool;
import net.elytrium.limboapi.api.world.item.datacomponent.type.TooltipDisplay;
import net.elytrium.limboapi.api.world.item.datacomponent.type.Unbreakable;
import net.elytrium.limboapi.api.world.item.datacomponent.type.UseCooldown;
import net.elytrium.limboapi.api.world.item.datacomponent.type.Weapon;
import net.elytrium.limboapi.api.world.item.datacomponent.type.WrittenBookContent;
import net.elytrium.limboapi.api.world.item.datacomponent.type.data.ConsumeEffect;
import net.elytrium.limboapi.api.world.item.datacomponent.type.data.Either;
import net.elytrium.limboapi.api.world.item.datacomponent.type.data.Filterable;
import net.elytrium.limboapi.api.world.item.datacomponent.type.data.Holder;
import net.elytrium.limboapi.api.world.item.datacomponent.type.data.HolderSet;
import net.elytrium.limboapi.api.world.item.datacomponent.type.data.SoundEvent;
import net.elytrium.limboapi.api.world.item.datacomponent.type.data.TrimMaterial;
import net.elytrium.limboapi.api.world.item.datacomponent.type.data.TypedEntityData;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.kyori.adventure.nbt.StringBinaryTag;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.common.value.qual.IntRange;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;

/**
 * @sinceMinecraft 1.20.5
 */
@NullMarked
public final class DataComponentTypes {

  public static final DataComponentType.Valued<CompoundBinaryTag> CUSTOM_DATA = DataComponentTypes.uninitialized();
  public static final DataComponentType.Valued<@IntRange(from = 1, to = 99) Integer> MAX_STACK_SIZE = DataComponentTypes.uninitialized();
  public static final DataComponentType.Valued<@Positive Integer> MAX_DAMAGE = DataComponentTypes.uninitialized();
  public static final DataComponentType.Valued<@NonNegative Integer> DAMAGE = DataComponentTypes.uninitialized();
  public static final DataComponentType.Valued<Unbreakable> UNBREAKABLE = DataComponentTypes.uninitialized();
  public static final DataComponentType.Valued<ComponentHolder> CUSTOM_NAME = DataComponentTypes.uninitialized();
  public static final DataComponentType.Valued<ComponentHolder> ITEM_NAME = DataComponentTypes.uninitialized();
  /**
   * @sinceMinecraft 1.21.2
   */
  public static final DataComponentType.Valued<String> ITEM_MODEL = DataComponentTypes.uninitialized();
  public static final DataComponentType.Valued<Collection<ComponentHolder>> LORE = DataComponentTypes.uninitialized();
  public static final DataComponentType.Valued<Integer> RARITY = DataComponentTypes.uninitialized();
  public static final DataComponentType.Valued<Enchantments> ENCHANTMENTS = DataComponentTypes.uninitialized();
  public static final DataComponentType.Valued<AdventureModePredicate> CAN_PLACE_ON = DataComponentTypes.uninitialized();
  public static final DataComponentType.Valued<AdventureModePredicate> CAN_BREAK = DataComponentTypes.uninitialized();
  public static final DataComponentType.Valued<AttributeModifiers> ATTRIBUTE_MODIFIERS = DataComponentTypes.uninitialized();
  public static final DataComponentType.Valued<CustomModelData> CUSTOM_MODEL_DATA = DataComponentTypes.uninitialized();
  /**
   * Removed in version 1.21.5
   */
  @ApiStatus.Obsolete
  public static final DataComponentType.NonValued HIDE_ADDITIONAL_TOOLTIP = DataComponentTypes.uninitialized();
  /**
   * Removed in version 1.21.5
   */
  @ApiStatus.Obsolete
  public static final DataComponentType.NonValued HIDE_TOOLTIP = DataComponentTypes.uninitialized();
  /**
   * @sinceMinecraft 1.21.5
   */
  public static final DataComponentType.Valued<TooltipDisplay> TOOLTIP_DISPLAY = DataComponentTypes.uninitialized();
  public static final DataComponentType.Valued<@NonNegative Integer> REPAIR_COST = DataComponentTypes.uninitialized();
  public static final DataComponentType.NonValued CREATIVE_SLOT_LOCK = DataComponentTypes.uninitialized();
  public static final DataComponentType.Valued<Boolean> ENCHANTMENT_GLINT_OVERRIDE = DataComponentTypes.uninitialized();
  /**
   * @apiNote Does not actually hold any data and should be {@code NonValued},
   * but the client expects an {@link CompoundBinaryTag#empty() empty compound} instead
   */
  public static final DataComponentType.Valued<CompoundBinaryTag> INTANGIBLE_PROJECTILE = DataComponentTypes.uninitialized();
  public static final DataComponentType.Valued<FoodProperties> FOOD = DataComponentTypes.uninitialized();
  /**
   * @sinceMinecraft 1.21.2
   */
  public static final DataComponentType.Valued<Consumable> CONSUMABLE = DataComponentTypes.uninitialized();
  /**
   * @sinceMinecraft 1.21.2
   */
  public static final DataComponentType.Valued<ItemStack> USE_REMAINDER = DataComponentTypes.uninitialized();
  /**
   * @sinceMinecraft 1.21.2
   */
  public static final DataComponentType.Valued<UseCooldown> USE_COOLDOWN = DataComponentTypes.uninitialized();
  /**
   * Removed in version 1.21.2
   */
  @ApiStatus.Obsolete
  public static final DataComponentType.NonValued FIRE_RESISTANT = DataComponentTypes.uninitialized();
  /**
   * @sinceMinecraft 1.21.2
   */
  public static final DataComponentType.Valued<String> DAMAGE_RESISTANT = DataComponentTypes.uninitialized();
  public static final DataComponentType.Valued<Tool> TOOL = DataComponentTypes.uninitialized();
  /**
   * @sinceMinecraft 1.21.5
   */
  public static final DataComponentType.Valued<Weapon> WEAPON = DataComponentTypes.uninitialized();
  /**
   * @sinceMinecraft 1.21.2
   */
  public static final DataComponentType.Valued<@Positive Integer> ENCHANTABLE = DataComponentTypes.uninitialized();
  /**
   * @sinceMinecraft 1.21.2
   */
  public static final DataComponentType.Valued<Equippable> EQUIPPABLE = DataComponentTypes.uninitialized();
  /**
   * @sinceMinecraft 1.21.2
   */
  public static final DataComponentType.Valued<HolderSet> REPAIRABLE = DataComponentTypes.uninitialized();
  /**
   * @sinceMinecraft 1.21.2
   */
  public static final DataComponentType.NonValued GLIDER = DataComponentTypes.uninitialized();
  /**
   * @sinceMinecraft 1.21.2
   */
  public static final DataComponentType.Valued<String> TOOLTIP_STYLE = DataComponentTypes.uninitialized();
  /**
   * @sinceMinecraft 1.21.2
   */
  public static final DataComponentType.Valued<Collection<ConsumeEffect>> DEATH_PROTECTION = DataComponentTypes.uninitialized();
  /**
   * @sinceMinecraft 1.21.5
   */
  public static final DataComponentType.Valued<BlocksAttacks> BLOCKS_ATTACKS = DataComponentTypes.uninitialized();
  public static final DataComponentType.Valued<Enchantments> STORED_ENCHANTMENTS = DataComponentTypes.uninitialized();
  public static final DataComponentType.Valued<DyedColor> DYED_COLOR = DataComponentTypes.uninitialized();
  public static final DataComponentType.Valued<Integer> MAP_COLOR = DataComponentTypes.uninitialized();
  public static final DataComponentType.Valued<Integer> MAP_ID = DataComponentTypes.uninitialized();
  public static final DataComponentType.Valued<CompoundBinaryTag> MAP_DECORATIONS = DataComponentTypes.uninitialized();
  public static final DataComponentType.Valued<Integer/*enum*/> MAP_POST_PROCESSING = DataComponentTypes.uninitialized();
  public static final DataComponentType.Valued<Collection<ItemStack>> CHARGED_PROJECTILES = DataComponentTypes.uninitialized();
  public static final DataComponentType.Valued<Collection<ItemStack>> BUNDLE_CONTENTS = DataComponentTypes.uninitialized();
  public static final DataComponentType.Valued<PotionContents> POTION_CONTENTS = DataComponentTypes.uninitialized();
  /**
   * @sinceMinecraft 1.21.5
   */
  public static final DataComponentType.Valued<Float> POTION_DURATION_SCALE = DataComponentTypes.uninitialized();
  public static final DataComponentType.Valued<Collection<SuspiciousStewEffect>> SUSPICIOUS_STEW_EFFECTS = DataComponentTypes.uninitialized();
  public static final DataComponentType.Valued<Collection<Filterable<String>>> WRITABLE_BOOK_CONTENT = DataComponentTypes.uninitialized();
  public static final DataComponentType.Valued<WrittenBookContent> WRITTEN_BOOK_CONTENT = DataComponentTypes.uninitialized();
  public static final DataComponentType.Valued<ArmorTrim> TRIM = DataComponentTypes.uninitialized();
  public static final DataComponentType.Valued<CompoundBinaryTag> DEBUG_STICK_STATE = DataComponentTypes.uninitialized();
  public static final DataComponentType.Valued<TypedEntityData> ENTITY_DATA = DataComponentTypes.uninitialized();
  public static final DataComponentType.Valued<CompoundBinaryTag> BUCKET_ENTITY_DATA = DataComponentTypes.uninitialized();
  public static final DataComponentType.Valued<TypedEntityData> BLOCK_ENTITY_DATA = DataComponentTypes.uninitialized();
  /**
   * @apiNote For versions prior to 1.21.5, only {@code Holder<Instrument>} is expected.
   * The plugin converts known string values to {@code Holder.Reference<Instrument>},
   * while unknown strings are replaced with an ID of {@code 0}.
   * <br>
   * Since version 1.21.5, either {@code Holder<Instrument>} or {@code String} can be provided.
   */
  public static final DataComponentType.Valued<Either<Holder<Instrument>, String>> INSTRUMENT = DataComponentTypes.uninitialized();
  /**
   * @sinceMinecraft 1.21.5
   */
  public static final DataComponentType.Valued<Either<Holder<TrimMaterial>, String>> PROVIDES_TRIM_MATERIAL = DataComponentTypes.uninitialized();
  public static final DataComponentType.Valued<@IntRange(from = 0, to = 4) Integer> OMINOUS_BOTTLE_AMPLIFIER = DataComponentTypes.uninitialized();
  /**
   * @sinceMinecraft 1.21
   */
  public static final DataComponentType.Valued<JukeboxPlayable> JUKEBOX_PLAYABLE = DataComponentTypes.uninitialized();
  /**
   * @sinceMinecraft 1.21.5
   */
  public static final DataComponentType.Valued<String> PROVIDES_BANNER_PATTERNS = DataComponentTypes.uninitialized();
  public static final DataComponentType.Valued<ListBinaryTag/*<StringBinaryTag>*/> RECIPES = DataComponentTypes.uninitialized();
  public static final DataComponentType.Valued<LodestoneTracker> LODESTONE_TRACKER = DataComponentTypes.uninitialized();
  public static final DataComponentType.Valued<FireworkExplosion> FIREWORK_EXPLOSION = DataComponentTypes.uninitialized();
  public static final DataComponentType.Valued<Fireworks> FIREWORKS = DataComponentTypes.uninitialized();
  public static final DataComponentType.Valued<ResolvableProfile> PROFILE = DataComponentTypes.uninitialized();
  public static final DataComponentType.Valued<String> NOTE_BLOCK_SOUND = DataComponentTypes.uninitialized();
  public static final DataComponentType.Valued<Collection<BannerPatternLayer>> BANNER_PATTERNS = DataComponentTypes.uninitialized();
  public static final DataComponentType.Valued<Integer/*enum DyeColor*/> BASE_COLOR = DataComponentTypes.uninitialized();
  public static final DataComponentType.Valued<Collection<Integer/*Item*/>> POT_DECORATIONS = DataComponentTypes.uninitialized();
  public static final DataComponentType.Valued<Collection<ItemStack>> CONTAINER = DataComponentTypes.uninitialized();
  public static final DataComponentType.Valued<Map<String, String>> BLOCK_STATE = DataComponentTypes.uninitialized();
  public static final DataComponentType.Valued<Collection<BeehiveOccupant>> BEES = DataComponentTypes.uninitialized();
  /**
   * @apiNote For versions prior to 1.21.2, {@link StringBinaryTag} is expected.
   *          Since version 1.21.2, {@link CompoundBinaryTag} is used instead
   */
  public static final DataComponentType.Valued<? extends BinaryTag> LOCK = DataComponentTypes.uninitialized();
  public static final DataComponentType.Valued<CompoundBinaryTag> CONTAINER_LOOT = DataComponentTypes.uninitialized();
  /**
   * @sinceMinecraft 1.21.5
   */
  public static final DataComponentType.Valued<Holder<SoundEvent>> BREAK_SOUND = DataComponentTypes.uninitialized();
  /**
   * @sinceMinecraft 1.21.5
   */
  public static final DataComponentType.Valued<Integer/*Holder*/> VILLAGER_VARIANT = DataComponentTypes.uninitialized();
  /**
   * @sinceMinecraft 1.21.5
   */
  public static final DataComponentType.Valued<Integer/*Holder*/> WOLF_VARIANT = DataComponentTypes.uninitialized();
  /**
   * @sinceMinecraft 1.21.5
   */
  public static final DataComponentType.Valued<Integer/*Holder*/> WOLF_SOUND_VARIANT = DataComponentTypes.uninitialized();
  /**
   * @sinceMinecraft 1.21.5
   */
  public static final DataComponentType.Valued<Integer/*enum DyeColor*/> WOLF_COLLAR = DataComponentTypes.uninitialized();
  /**
   * @sinceMinecraft 1.21.5
   */
  public static final DataComponentType.Valued<Integer/*enum*/> FOX_VARIANT = DataComponentTypes.uninitialized();
  /**
   * @sinceMinecraft 1.21.5
   */
  public static final DataComponentType.Valued<Integer/*enum*/> SALMON_SIZE = DataComponentTypes.uninitialized();
  /**
   * @sinceMinecraft 1.21.5
   */
  public static final DataComponentType.Valued<Integer/*enum*/> PARROT_VARIANT = DataComponentTypes.uninitialized();
  /**
   * @sinceMinecraft 1.21.5
   */
  public static final DataComponentType.Valued<Integer/*packed(sizeId | (sizeSpecificId << 8))*/> TROPICAL_FISH_PATTERN = DataComponentTypes.uninitialized();
  /**
   * @sinceMinecraft 1.21.5
   */
  public static final DataComponentType.Valued<Integer/*enum DyeColor*/> TROPICAL_FISH_BASE_COLOR = DataComponentTypes.uninitialized();
  /**
   * @sinceMinecraft 1.21.5
   */
  public static final DataComponentType.Valued<Integer/*enum DyeColor*/> TROPICAL_FISH_PATTERN_COLOR = DataComponentTypes.uninitialized();
  /**
   * @sinceMinecraft 1.21.5
   */
  public static final DataComponentType.Valued<Integer/*enum*/> MOOSHROOM_VARIANT = DataComponentTypes.uninitialized();
  /**
   * @sinceMinecraft 1.21.5
   */
  public static final DataComponentType.Valued<Integer/*enum*/> RABBIT_VARIANT = DataComponentTypes.uninitialized();
  /**
   * @sinceMinecraft 1.21.5
   */
  public static final DataComponentType.Valued<Integer/*Holder*/> PIG_VARIANT = DataComponentTypes.uninitialized();
  /**
   * @sinceMinecraft 1.21.5
   */
  public static final DataComponentType.Valued<Integer/*Holder*/> COW_VARIANT = DataComponentTypes.uninitialized();
  /**
   * @sinceMinecraft 1.21.5
   */
  // Either a registry id or a registry key (for no reason)
  public static final DataComponentType.Valued<Either<Integer/*Holder*/, String>> CHICKEN_VARIANT = DataComponentTypes.uninitialized();
  /**
   * @sinceMinecraft 1.21.5
   */
  public static final DataComponentType.Valued<Integer/*Holder*/> FROG_VARIANT = DataComponentTypes.uninitialized();
  /**
   * @sinceMinecraft 1.21.5
   */
  public static final DataComponentType.Valued<Integer/*enum*/> HORSE_VARIANT = DataComponentTypes.uninitialized();
  /**
   * @sinceMinecraft 1.21.5
   */
  public static final DataComponentType.Valued<Holder<PaintingVariant>> PAINTING_VARIANT = DataComponentTypes.uninitialized();
  /**
   * @sinceMinecraft 1.21.5
   */
  public static final DataComponentType.Valued<Integer/*enum*/> LLAMA_VARIANT = DataComponentTypes.uninitialized();
  /**
   * @sinceMinecraft 1.21.5
   */
  public static final DataComponentType.Valued<Integer/*enum*/> AXOLOTL_VARIANT = DataComponentTypes.uninitialized();
  /**
   * @sinceMinecraft 1.21.5
   */
  public static final DataComponentType.Valued<Integer/*Holder*/> CAT_VARIANT = DataComponentTypes.uninitialized();
  /**
   * @sinceMinecraft 1.21.5
   */
  public static final DataComponentType.Valued<Integer/*enum DyeColor*/> CAT_COLLAR = DataComponentTypes.uninitialized();
  /**
   * @sinceMinecraft 1.21.5
   */
  public static final DataComponentType.Valued<Integer/*enum DyeColor*/> SHEEP_COLOR = DataComponentTypes.uninitialized();
  /**
   * @sinceMinecraft 1.21.5
   */
  public static final DataComponentType.Valued<Integer/*enum DyeColor*/> SHULKER_COLOR = DataComponentTypes.uninitialized();

  @NonNull
  @SuppressWarnings("DataFlowIssue")
  private static <T> T uninitialized() {
    return null; // The value will be initialized later by LimboAPI plugin using reflection
  }
}
