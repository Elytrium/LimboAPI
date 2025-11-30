/*
 * Copyright (C) 2021 - 2024 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.protocol.data;

import com.velocitypowered.api.network.ProtocolVersion;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.OptionalInt;
import java.util.UUID;
import net.elytrium.limboapi.api.world.item.datacomponent.type.PaintingVariant;
import net.elytrium.limboapi.api.world.item.datacomponent.type.ResolvableProfile;
import net.elytrium.limboapi.api.world.item.datacomponent.type.data.Holder;
import net.elytrium.limboapi.api.world.item.datacomponent.type.data.HolderSet;
import net.elytrium.limboapi.api.world.player.LimboPlayer;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.EndBinaryTag;
import org.checkerframework.checker.index.qual.Positive;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;

/**
 * The value can be one of the following data types (note that some are version-specific):
 * <pre>
 * {@link Byte},
 * {@link Short} (2-byte short <1.9, VarInt >=1.9),
 * {@link Integer} (4-byte int <=1.8, VarInt >1.8),
 * {@link Long} (4-byte int <=1.8, VarInt <=1.19.2, VarLong >1.19.2),
 * {@link Float}, {@link String}, {@link ComponentHolder}, {@link OptionalComponentHolder}, {@link ItemStack},
 * {@link Boolean} (serialized as Byte data type <=1.8, Boolean >1.8),
 * {@link Rotations}, {@link BlockPos}, {@link OptionalBlockPos}, {@link Direction},
 * {@link OptionalUUID} (serialized as String data type <=1.8, OptionalUUID >1.8),
 * {@link BlockState}, {@link OptionalBlockState},
 * {@link CompoundBinaryTag} ({@link EndBinaryTag#endBinaryTag()} Should be used instead of null),
 * {@link Particle}, {@link Particles}, {@link VillagerData},
 * {@link OptionalInt} (Also known as OptionalUnsignedInt) (4-byte int <=1.8, VarInt <=1.13.2, OptionalInt >1.13.2),
 * {@link Pose}, {@link CatVariant}, {@link CowVariant}, {@link WolfVariant}, {@link WolfSoundVariant}, {@link FrogVariant}, {@link PigVariant}, {@link ChickenVariant},
 * {@link OptionalGlobalPos}, {@link PaintingVariantHolder}, {@link SnifferState}, {@link ArmadilloState}, {@link CopperGolemState}, {@link WeatheringCopperState},
 * {@link ResolvableProfile}, {@link Vector3}, {@link Quaternion}
 * </pre>
 *
 * @see LimboPlayer#setEntityData(int, EntityData)
 */
@NullMarked
public class EntityData extends Short2ObjectOpenHashMap<Object> {

  public EntityData(int expected, float f) {
    super(expected, f);
  }

  public EntityData(int expected) {
    super(expected);
  }

  public EntityData() {

  }

  public EntityData(Map<? extends Short, ?> m, float f) {
    super(m, f);
  }

  public EntityData(Map<? extends Short, ?> m) {
    super(m);
  }

  public EntityData(Short2ObjectMap<Object> m, float f) {
    super(m, f);
  }

  public EntityData(Short2ObjectMap<Object> m) {
    super(m);
  }

  public EntityData(short[] k, Object[] v, float f) {
    super(k, v, f);
  }

  public EntityData(short[] k, Object[] v) {
    super(k, v);
  }

  public record OptionalComponentHolder(@Nullable ComponentHolder component) {

    public static final OptionalComponentHolder EMPTY = new OptionalComponentHolder(null);

    public ComponentHolder orElseThrow() {
      if (this.component == null) {
        throw new NoSuchElementException("No value present");
      }

      return this.component;
    }

    public static OptionalComponentHolder of(@Nullable ComponentHolder component) {
      return component == null ? OptionalComponentHolder.EMPTY : new OptionalComponentHolder(component);
    }
  }

  public record OptionalBlockPos(@Nullable BlockPos blockPos) {

    public static final OptionalBlockPos EMPTY = new OptionalBlockPos(null);

    public BlockPos orElseThrow() {
      if (this.blockPos == null) {
        throw new NoSuchElementException("No value present");
      }

      return this.blockPos;
    }

    public static OptionalBlockPos of(@Nullable BlockPos blockPos) {
      return blockPos == null ? OptionalBlockPos.EMPTY : new OptionalBlockPos(blockPos);
    }
  }

  /**
   * @sinceMinecraft 1.8
   */
  public record Rotations(float posX, float posY, float posZ) {

    public Rotations(float posX, float posY, float posZ) {
      this.posX = !Float.isInfinite(posX) && !Float.isNaN(posX) ? posX % 360.0F : 0.0F;
      this.posY = !Float.isInfinite(posY) && !Float.isNaN(posY) ? posY % 360.0F : 0.0F;
      this.posZ = !Float.isInfinite(posZ) && !Float.isNaN(posZ) ? posZ % 360.0F : 0.0F;
    }
  }

  public enum Direction {

    DOWN,
    UP,
    NORTH,
    SOUTH,
    WEST,
    EAST;

    public static final Direction[] VALUES = Direction.values();
  }

  /**
   * Also known as OptionalLivingEntityReference as of 1.21.5
   */
  public record OptionalUUID(@Nullable UUID uuid) {

    public static final OptionalUUID EMPTY = new OptionalUUID(null);

    public UUID orElseThrow() {
      if (this.uuid == null) {
        throw new NoSuchElementException("No value present");
      }

      return this.uuid;
    }

    public static OptionalUUID of(@Nullable UUID uuid) {
      return uuid == null ? OptionalUUID.EMPTY : new OptionalUUID(uuid);
    }
  }

  /**
   * @sinceMinecraft 1.19.4
   */
  public record BlockState(int blockState/*TODO use existing api*/) implements Particle.ParticleOptions {

  }

  public record OptionalBlockState(@Nullable BlockState blockState) {

    public static final OptionalBlockState EMPTY = new OptionalBlockState(null);

    public BlockState orElseThrow() {
      if (this.blockState == null) {
        throw new NoSuchElementException("No value present");
      }

      return this.blockState;
    }

    public static OptionalBlockState of(@Nullable BlockState blockState) {
      return blockState == null ? OptionalBlockState.EMPTY : new OptionalBlockState(blockState);
    }
  }

  /**
   * @sinceMinecraft 1.13
   */
  public record Particle(int type/*TODO expose api*/, @Nullable ParticleOptions data) {

    /**
     * @sinceMinecraft 1.21.9
     */
    public record PowerParticleOption(float power) implements ParticleOptions {

    }

    /**
     * @param scale Must be within range [0.01, 4.0]
     *
     * @sinceMinecraft 1.13
     */
    public record DustParticleOptions(int color, float scale) implements ParticleOptions {

      /**
       * @param scale Must be within range [0.01, 4.0]
       */
      public DustParticleOptions(int color, float scale) {
        this.color = color;
        this.scale = scale < 0.01F ? 0.01F : Math.min(scale, 4.0F);
      }
    }

    /**
     * @param scale Must be within range [0.01, 4.0]
     *
     * @sinceMinecraft 1.17
     */
    public record DustColorTransitionOptions(int fromColor, int toColor, float scale) implements ParticleOptions {

      public DustColorTransitionOptions(int fromColor, int toColor, float scale) {
        this.fromColor = fromColor;
        this.toColor = toColor;
        this.scale = scale < 0.01F ? 0.01F : Math.min(scale, 4.0F);
      }
    }

    /**
     * @sinceMinecraft 1.21.9
     */
    public record SpellParticleOption(int color, float power) implements ParticleOptions {

    }

    /**
     * @param color ARGB
     *
     * @sinceMinecraft 1.20.5
     */
    public record ColorParticleOption(int color) implements ParticleOptions {

      public static float redFloat(int color) {
        return ColorParticleOption.from8BitChannel(ColorParticleOption.red(color));
      }

      public static int red(int color) {
        return (color >> 16) & 0xFF;
      }

      public static float greenFloat(int color) {
        return ColorParticleOption.from8BitChannel(ColorParticleOption.green(color));
      }

      public static int green(int color) {
        return (color >> 8) & 0xFF;
      }

      public static float blueFloat(int color) {
        return ColorParticleOption.from8BitChannel(ColorParticleOption.blue(color));
      }

      public static int blue(int i) {
        return i & 0xFF;
      }

      public static float alphaFloat(int color) {
        return ColorParticleOption.from8BitChannel(ColorParticleOption.alpha(color));
      }

      public static int alpha(int color) {
        return color >>> 24;
      }

      public static int colorFromFloat(float red, float green, float blue) {
        return ColorParticleOption.colorFromFloat(1.0F, red, green, blue);
      }

      public static int colorFromFloat(float alpha, float red, float green, float blue) {
        return ColorParticleOption.color(ColorParticleOption.as8BitChannel(alpha), ColorParticleOption.as8BitChannel(red), ColorParticleOption.as8BitChannel(green), ColorParticleOption.as8BitChannel(blue));
      }

      public static int color(int red, int green, int blue) {
        return ColorParticleOption.color(0xFF, red, green, blue);
      }

      public static int color(int alpha, int red, int green, int blue) {
        return (alpha & 0xFF) << 24 | (red & 0xFF) << 16 | (green & 0xFF) << 8 | blue & 0xFF;
      }

      private static float from8BitChannel(int channel) {
        return (float) channel / 255.0F;
      }

      public static int as8BitChannel(float channel) {
        return ColorParticleOption.floor(channel * 255.0F);
      }

      private static int floor(float value) {
        int i = (int) value;
        return value < i ? i - 1 : i;
      }
    }

    /**
     * @sinceMinecraft 1.19
     */
    public record SculkChargeParticleOptions(float roll) implements ParticleOptions {

    }

    /**
     * @param origin Present only in versions [1.17-1.18.2]
     *
     * @sinceMinecraft 1.17
     */
    public record VibrationParticle(@ApiStatus.Obsolete @Nullable BlockPos origin, PositionSource destination, int arrivalInTicks) implements ParticleOptions {

      public record EntityPositionSource(int entityId, float yOffset) implements PositionSource {

      }

      public sealed interface PositionSource permits BlockPos, EntityPositionSource {

      }
    }

    /**
     * @param duration Added in version 1.21.4
     *
     * @sinceMinecraft 1.21.2
     */
    public record TrailParticleOption(Vector3 target, int color, @Positive int duration) implements ParticleOptions {

    }

    /**
     * @sinceMinecraft 1.19
     */
    public record ShriekParticleOption(int delay) implements ParticleOptions {

    }

    public sealed interface ParticleOptions permits BlockState,
        PowerParticleOption, DustParticleOptions, DustColorTransitionOptions, SpellParticleOption, ColorParticleOption,
        SculkChargeParticleOptions, ItemStack, VibrationParticle, TrailParticleOption, ShriekParticleOption {

    }
  }

  /**
   * @sinceMinecraft 1.20.5
   */
  public static class Particles extends ObjectArrayList<Particle> {

    public Particles(int initialCapacity) {
      super(initialCapacity);
    }

    public Particles() {

    }
  }

  /**
   * @param level Must be no less than 1
   *
   * @sinceMinecraft 1.14
   */
  public record VillagerData(int type, int profession, int level) {

    public VillagerData(int type, int profession, int level) {
      this.type = type;
      this.profession = profession;
      this.level = Math.max(1, level);
    }
  }

  /**
   * @sinceMinecraft 1.14
   */
  public enum Pose { // TODO registry map

    STANDING,
    FALL_FLYING,
    SLEEPING,
    SWIMMING,
    SPIN_ATTACK,
    CROUCHING,
    /**
     * @sinceMinecraft 1.17
     */
    LONG_JUMPING,
    DYING,
    /**
     * @sinceMinecraft 1.19
     */
    CROAKING,
    /**
     * @sinceMinecraft 1.19
     */
    USING_TONGUE,
    /**
     * @sinceMinecraft 1.19.3
     */
    SITTING,
    /**
     * @sinceMinecraft 1.19
     */
    ROARING,
    /**
     * @sinceMinecraft 1.19
     */
    SNIFFING,
    /**
     * @sinceMinecraft 1.19
     */
    EMERGING,
    /**
     * @sinceMinecraft 1.19
     */
    DIGGING,
    /**
     * @sinceMinecraft 1.20
     */
    SLIDING,
    /**
     * @sinceMinecraft 1.20
     */
    SHOOTING,
    /**
     * @sinceMinecraft 1.20
     */
    INHALING;

    public static final Pose[] VALUES = Pose.values();

    public int getProtocolId(ProtocolVersion version) {
      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_20)) { // after 1.19.3 all new poses were added to end
        return this.ordinal();
      } else {
        return switch (this) {
          case STANDING -> 0;
          case FALL_FLYING -> 1;
          case SLEEPING -> 2;
          case SWIMMING -> 3;
          case SPIN_ATTACK -> 4;
          case CROUCHING -> 5;
          default -> version.noLessThan(ProtocolVersion.MINECRAFT_1_17) ? switch (this) {
            // >=1.17
            case LONG_JUMPING -> 6;
            case DYING -> 7;
            default -> version.noLessThan(ProtocolVersion.MINECRAFT_1_19) ? switch (this) {
              // >=1.19
              case CROAKING -> 8;
              case USING_TONGUE -> 9;
              default -> version.noLessThan(ProtocolVersion.MINECRAFT_1_19_3) ? switch (this) {
                // >=1.19.3
                case SITTING -> 10;
                case ROARING -> 11;
                case SNIFFING -> 12;
                case EMERGING -> 13;
                case DIGGING -> 14;
                default -> Pose.fail(this);
              } : switch (this) {
                // >=1.19
                case ROARING -> 10;
                case SNIFFING -> 11;
                case EMERGING -> 12;
                case DIGGING -> 13;
                default -> Pose.fail(this);
              };
            } : Pose.fail(this);
          } : this == Pose.DYING ? 6 : Pose.fail(this);
        };
      }
    }

    public static Pose fromProtocolId(int id, ProtocolVersion version) {
      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_20)) {
        return Pose.VALUES[id];
      } else {
        return switch (id) {
          case 0 -> Pose.STANDING;
          case 1 -> Pose.FALL_FLYING;
          case 2 -> Pose.SLEEPING;
          case 3 -> Pose.SWIMMING;
          case 4 -> Pose.SPIN_ATTACK;
          case 5 -> Pose.CROUCHING;
          default -> version.noLessThan(ProtocolVersion.MINECRAFT_1_17) ? switch (id) {
            // >=1.17
            case 6 -> Pose.LONG_JUMPING;
            case 7 -> Pose.DYING;
            default -> version.noLessThan(ProtocolVersion.MINECRAFT_1_19) ? switch (id) {
              // >=1.19
              case 8 -> Pose.CROAKING;
              case 9 -> Pose.USING_TONGUE;
              default -> version.noLessThan(ProtocolVersion.MINECRAFT_1_19_3) ? switch (id) {
                // >=1.19.3
                case 10 -> Pose.SITTING;
                case 11 -> Pose.ROARING;
                case 12 -> Pose.SNIFFING;
                case 13 -> Pose.EMERGING;
                case 14 -> Pose.DIGGING;
                default -> Pose.fail(id);
              } : switch (id) {
                // >=1.19
                case 10 -> Pose.ROARING;
                case 11 -> Pose.SNIFFING;
                case 12 -> Pose.EMERGING;
                case 13 -> Pose.DIGGING;
                default -> Pose.fail(id);
              };
            } : Pose.fail(id);
          } : id == 6 ? Pose.DYING : Pose.fail(id);
        };
      }
    }

    private static <T> T fail(Object value) {
      throw new IllegalStateException("Unexpected value: " + value);
    }
  }

  /**
   * @sinceMinecraft 1.19
   */
  public record CatVariant(int id) {

  }

  /**
   * @sinceMinecraft 1.21.5
   */
  public record CowVariant(int id) {

  }

  /**
   * @sinceMinecraft 1.20.5
   */
  public sealed interface WolfVariant permits WolfVariant.Direct, WolfVariant.Reference {

    static WolfVariant.Reference of(int id) {
      return new WolfVariant.Reference(id);
    }

    record Reference(int id) implements WolfVariant {

    }

    /**
     * Present only in versions [1.21-1.21.4]
     * <p>
     * For versions outside this range, {@code WolfVariant.Direct} values are replaced with ID {@code 0}.
     */
    @ApiStatus.Obsolete
    record Direct(String wildTexture, String tameTexture, String angryTexture, HolderSet biomes) implements WolfVariant {

    }
  }

  /**
   * @sinceMinecraft 1.21.5
   */
  public record WolfSoundVariant(int id) {

  }

  /**
   * @sinceMinecraft 1.19
   */
  public record FrogVariant(int id) {

  }

  /**
   * @sinceMinecraft 1.21.5
   */
  public record PigVariant(int id) {

  }

  /**
   * @sinceMinecraft 1.21.5
   */
  public record ChickenVariant(int id) {

  }

  /**
   * @sinceMinecraft 1.19
   */
  public record OptionalGlobalPos(@Nullable GlobalPos globalPos) {

    public static final OptionalGlobalPos EMPTY = new OptionalGlobalPos(null);

    public GlobalPos orElseThrow() {
      if (this.globalPos == null) {
        throw new NoSuchElementException("No value present");
      }

      return this.globalPos;
    }

    public static OptionalGlobalPos of(@Nullable GlobalPos globalPos) {
      return globalPos == null ? OptionalGlobalPos.EMPTY : new OptionalGlobalPos(globalPos);
    }
  }

  /**
   * @param painting For versions prior to 1.21, only {@code Holder.Reference<PaintingVariant>} is expected.
   *                 Direct {@link PaintingVariant} values are replaced with an ID {@code 0}.
   *                 <br>
   *                 Since version 1.21, either {@code PaintingVariant} or {@code Holder.Reference<PaintingVariant>} can be provided.
   *
   * @sinceMinecraft 1.19
   */
  public record PaintingVariantHolder(Holder<PaintingVariant> painting) {

  }

  /**
   * @sinceMinecraft 1.19.4
   */
  public enum SnifferState {

    IDLING,
    FEELING_HAPPY,
    SCENTING,
    SNIFFING,
    SEARCHING,
    DIGGING,
    RISING;

    public static final SnifferState[] VALUES = SnifferState.values();
  }

  /**
   * @sinceMinecraft 1.20.5
   */
  public enum ArmadilloState { // 🤠

    IDLE,
    ROLLING,
    SCARED,
    UNROLLING;

    public static final ArmadilloState[] VALUES = ArmadilloState.values();
  }

  /**
   * @sinceMinecraft 1.21.5
   */
  public enum CopperGolemState {

    IDLE,
    GETTING_ITEM,
    GETTING_NO_ITEM,
    DROPPING_ITEM,
    DROPPING_NO_ITEM;

    public static final CopperGolemState[] VALUES = CopperGolemState.values();
  }

  /**
   * @sinceMinecraft 1.21.5
   */
  public enum WeatheringCopperState {

    UNAFFECTED,
    EXPOSED,
    WEATHERED,
    OXIDIZED;

    public static final WeatheringCopperState[] VALUES = WeatheringCopperState.values();
  }

  /**
   * @sinceMinecraft 1.19.4
   */
  public record Vector3(float x, float y, float z) {

  }

  /**
   * @sinceMinecraft 1.19.4
   */
  public record Quaternion(float x, float y, float z, float w) {

  }
}
