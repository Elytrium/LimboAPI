package net.elytrium.limboapi.server.world;

import com.velocitypowered.api.network.ProtocolVersion;
import net.elytrium.limboapi.api.protocol.data.EntityData;
import net.elytrium.limboapi.protocol.codec.StreamCodec;
import net.elytrium.limboapi.server.item.codec.data.BlockStateCodec;
import net.elytrium.limboapi.server.item.codec.data.ItemStackCodec;
import net.elytrium.limboapi.server.item.codec.data.ParticleCodec;
import org.jetbrains.annotations.ApiStatus;

public enum Particle {

  /**
   * Present only in [1.13-1.20.3]
   */
  @ApiStatus.Obsolete
  AMBIENT_ENTITY_EFFECT,
  ANGRY_VILLAGER,
  /**
   * @sinceMinecraft 1.16
   */
  ASH,
  /**
   * Present only in [1.13-1.17.1]
   */
  @ApiStatus.Obsolete
  BARRIER,
  BLOCK(BlockStateCodec.CODEC),
  /**
   * @sinceMinecraft 1.21.2
   */
  BLOCK_CRUMBLE(BlockStateCodec.CODEC),
  /**
   * @sinceMinecraft 1.18
   */
  BLOCK_MARKER(BlockStateCodec.CODEC),
  BUBBLE,
  BUBBLE_COLUMN_UP,
  BUBBLE_POP,
  /**
   * @sinceMinecraft 1.14
   */
  CAMPFIRE_COSY_SMOKE,
  /**
   * @sinceMinecraft 1.14
   */
  CAMPFIRE_SIGNAL_SMOKE,
  /**
   * @sinceMinecraft 1.20
   */
  CHERRY_LEAVES,
  CLOUD,
  /**
   * @sinceMinecraft 1.14
   */
  COMPOSTER,
  /**
   * @sinceMinecraft 1.21.9
   */
  COPPER_FIRE_FLAME,
  /**
   * @sinceMinecraft 1.16
   */
  CRIMSON_SPORE,
  CRIT,
  CURRENT_DOWN,
  DAMAGE_INDICATOR,
  DOLPHIN,
  DRAGON_BREATH(ParticleCodec.ParticleDataCodecs.POWER_CODEC),
  /**
   * Present only in [1.19.4-1.19.4]
   *
   * @sinceMinecraft 1.19.4
   */
  @ApiStatus.Obsolete
  DRIPPING_CHERRY_LEAVES,
  /**
   * @sinceMinecraft 1.17
   */
  DRIPPING_DRIPSTONE_LAVA,
  /**
   * @sinceMinecraft 1.17
   */
  DRIPPING_DRIPSTONE_WATER,
  /**
   * @sinceMinecraft 1.15
   */
  DRIPPING_HONEY,
  DRIPPING_LAVA,
  /**
   * @sinceMinecraft 1.16
   */
  DRIPPING_OBSIDIAN_TEAR,
  DRIPPING_WATER,
  DUST(ParticleCodec.ParticleDataCodecs.DUST_PARTICLE_CODEC),
  /**
   * @sinceMinecraft 1.17
   */
  DUST_COLOR_TRANSITION(ParticleCodec.ParticleDataCodecs.DUST_COLOR_TRANSITION_CODEC),
  /**
   * @sinceMinecraft 1.20.5
   */
  DUST_PILLAR(BlockStateCodec.CODEC),
  /**
   * @sinceMinecraft 1.20.3
   */
  DUST_PLUME,
  EFFECT(StreamCodec.ge(ProtocolVersion.MINECRAFT_1_21_9, ParticleCodec.ParticleDataCodecs.COLOR_CODEC, new EntityData.Particle.ColorParticleOption(-1))),
  /**
   * @sinceMinecraft 1.20
   */
  EGG_CRACK,
  ELDER_GUARDIAN,
  /**
   * @sinceMinecraft 1.17
   */
  ELECTRIC_SPARK,
  ENCHANT,
  ENCHANTED_HIT,
  END_ROD,
  ENTITY_EFFECT(StreamCodec.ge(ProtocolVersion.MINECRAFT_1_20_5, ParticleCodec.ParticleDataCodecs.COLOR_CODEC, new EntityData.Particle.ColorParticleOption(0))),
  EXPLOSION,
  EXPLOSION_EMITTER,
  /**
   * Present only in [1.19.4-1.19.4]
   *
   * @sinceMinecraft 1.19.4
   */
  @ApiStatus.Obsolete
  FALLING_CHERRY_LEAVES,
  /**
   * @sinceMinecraft 1.17
   */
  FALLING_DRIPSTONE_LAVA,
  /**
   * @sinceMinecraft 1.17
   */
  FALLING_DRIPSTONE_WATER,
  FALLING_DUST(BlockStateCodec.CODEC),
  /**
   * @sinceMinecraft 1.15
   */
  FALLING_HONEY,
  /**
   * @sinceMinecraft 1.14
   */
  FALLING_LAVA,
  /**
   * @sinceMinecraft 1.15
   */
  FALLING_NECTAR,
  /**
   * @sinceMinecraft 1.16
   */
  FALLING_OBSIDIAN_TEAR,
  /**
   * @sinceMinecraft 1.17
   */
  FALLING_SPORE_BLOSSOM,
  /**
   * @sinceMinecraft 1.14
   */
  FALLING_WATER,
  /**
   * @sinceMinecraft 1.21.5
   */
  FIREFLY,
  FIREWORK,
  FISHING,
  FLAME,
  /**
   * @sinceMinecraft 1.14
   */
  FLASH(Particle.EFFECT.codec()),
  /**
   * @sinceMinecraft 1.17
   */
  GLOW,
  /**
   * @sinceMinecraft 1.17
   */
  GLOW_SQUID_INK,
  /**
   * @sinceMinecraft 1.20.3
   */
  GUST,
  /**
   * Present only in [1.20.3-1.20.3]
   *
   * @sinceMinecraft 1.20.3
   */
  @ApiStatus.Obsolete
  GUST_DUST,
  /**
   * Present only in [1.20.3-1.20.3]
   *
   * @sinceMinecraft 1.20.3
   */
  @ApiStatus.Obsolete
  GUST_EMITTER,
  /**
   * @sinceMinecraft 1.20.5
   */
  GUST_EMITTER_LARGE,
  /**
   * @sinceMinecraft 1.20.5
   */
  GUST_EMITTER_SMALL,
  HAPPY_VILLAGER,
  HEART,
  /**
   * @sinceMinecraft 1.20.5
   */
  INFESTED,
  INSTANT_EFFECT(ParticleCodec.ParticleDataCodecs.SPELL_CODEC),
  ITEM(ItemStackCodec.CODEC),
  /**
   * @sinceMinecraft 1.20.5
   */
  ITEM_COBWEB,
  ITEM_SLIME,
  ITEM_SNOWBALL,
  /**
   * Present only in 1.19.4
   *
   * @sinceMinecraft 1.19.4
   */
  @ApiStatus.Obsolete
  LANDING_CHERRY_LEAVES,
  /**
   * @sinceMinecraft 1.15
   */
  LANDING_HONEY,
  /**
   * @sinceMinecraft 1.14
   */
  LANDING_LAVA,
  /**
   * @sinceMinecraft 1.16
   */
  LANDING_OBSIDIAN_TEAR,
  LARGE_SMOKE,
  LAVA,
  /**
   * Present only in [1.17-1.17.1]
   *
   * @sinceMinecraft 1.17
   */
  @ApiStatus.Obsolete
  LIGHT,
  MYCELIUM,
  NAUTILUS,
  NOTE,
  /**
   * @sinceMinecraft 1.20.5
   */
  OMINOUS_SPAWNING,
  /**
   * @sinceMinecraft 1.21.4
   */
  PALE_OAK_LEAVES,
  POOF,
  PORTAL,
  /**
   * @sinceMinecraft 1.20.5
   */
  RAID_OMEN,
  RAIN,
  /**
   * @sinceMinecraft 1.16
   */
  REVERSE_PORTAL,
  /**
   * @sinceMinecraft 1.17
   */
  SCRAPE,
  /**
   * @sinceMinecraft 1.19
   */
  SCULK_CHARGE(ParticleCodec.ParticleDataCodecs.SCULK_CHARGE),
  /**
   * @sinceMinecraft 1.19
   */
  SCULK_CHARGE_POP,
  /**
   * @sinceMinecraft 1.19
   */
  SCULK_SOUL,
  /**
   * @sinceMinecraft 1.19
   */
  SHRIEK(ParticleCodec.ParticleDataCodecs.SHRIEK_CODEC),
  /**
   * @sinceMinecraft 1.17
   */
  SMALL_FLAME,
  /**
   * @sinceMinecraft 1.20.5
   */
  SMALL_GUST,
  SMOKE,
  /**
   * @sinceMinecraft 1.14
   */
  SNEEZE,
  /**
   * @sinceMinecraft 1.17
   */
  SNOWFLAKE,
  /**
   * @sinceMinecraft 1.19
   */
  SONIC_BOOM,
  /**
   * @sinceMinecraft 1.16
   */
  SOUL,
  /**
   * @sinceMinecraft 1.16
   */
  SOUL_FIRE_FLAME,
  SPIT,
  SPLASH,
  /**
   * @sinceMinecraft 1.17
   */
  SPORE_BLOSSOM_AIR,
  SQUID_INK,
  SWEEP_ATTACK,
  /**
   * @sinceMinecraft 1.21.5
   */
  TINTED_LEAVES(ParticleCodec.ParticleDataCodecs.COLOR_CODEC),
  TOTEM_OF_UNDYING,
  /**
   * @sinceMinecraft 1.21.2
   */
  TRAIL(ParticleCodec.ParticleDataCodecs.TRAIL_CODEC),
  /**
   * @sinceMinecraft 1.20.5
   */
  TRIAL_OMEN,
  /**
   * @sinceMinecraft 1.20.3
   */
  TRIAL_SPAWNER_DETECTION,
  /**
   * @sinceMinecraft 1.20.5
   */
  TRIAL_SPAWNER_DETECTION_OMINOUS,
  UNDERWATER,
  /**
   * @sinceMinecraft 1.20.5
   */
  VAULT_CONNECTION,
  /**
   * @sinceMinecraft 1.17
   */
  VIBRATION(ParticleCodec.ParticleDataCodecs.VIBRATION_CODEC),
  /**
   * @sinceMinecraft 1.16
   */
  WARPED_SPORE,
  /**
   * @sinceMinecraft 1.17
   */
  WAX_OFF,
  /**
   * @sinceMinecraft 1.17
   */
  WAX_ON,
  /**
   * @sinceMinecraft 1.16
   */
  WHITE_ASH,
  /**
   * @sinceMinecraft 1.20.3
   */
  WHITE_SMOKE,
  WITCH;

  private final StreamCodec<EntityData.Particle.ParticleOptions> codec;

  Particle() {
    this.codec = null;
  }

  @SuppressWarnings("unchecked")
  Particle(StreamCodec<? extends EntityData.Particle.ParticleOptions> codec) {
    this.codec = (StreamCodec<EntityData.Particle.ParticleOptions>) codec;
  }

  public StreamCodec<EntityData.Particle.ParticleOptions> codec() {
    return this.codec;
  }
}
