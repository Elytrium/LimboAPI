/*
 * Copyright (C) 2021 Elytrium
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

package net.elytrium.limboapi.material;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Arrays;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.elytrium.limboapi.api.chunk.VirtualBiome;
import net.elytrium.limboapi.material.Biome.Effects.MoodSound;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag.Builder;
import net.kyori.adventure.nbt.ListBinaryTag;

public enum Biome implements VirtualBiome {

  PLAINS("minecraft:plains", 1,
      new Element("rain", 0.125f, 0.8f, 0.05f, 0.4f, "plains",
          Effects.builder(7907327, 329011, 12638463, 415920)
              .moodSound(MoodSound.of(6000, 2.0d, 8, "minecraft:ambient.cave"))
              .build()
      )
  ),
  SWAMP("minecraft:swamp", 6,
      new Element("rain", -0.2F, 0.8f, 0.1F, 0.9F, "swamp",
          Effects.builder(7907327, 329011, 12638463, 415920)
              .grassColorModifier("swamp")
              .foliageColor(6975545)
              .moodSound(MoodSound.of(6000, 2.0d, 8, "minecraft:ambient.cave"))
              .build()
      )
  ),
  SWAMP_HILLS("minecraft:swamp_hills", 134,
      new Element("rain", -0.1F, 0.8f, 0.3F, 0.9F, "swamp",
          Effects.builder(7907327, 329011, 12638463, 415920)
              .grassColorModifier("swamp")
              .foliageColor(6975545)
              .moodSound(MoodSound.of(6000, 2.0d, 8, "minecraft:ambient.cave"))
              .build()
      )
  );

  public final String name;
  public final int id;
  public final Element element;

  Biome(String name, int id, Element element) {
    this.name = name;
    this.id = id;
    this.element = element;
  }

  public CompoundBinaryTag encodeBiome() {
    return CompoundBinaryTag.builder()
        .putString("name", this.name)
        .putInt("id", this.id)
        .put("element", this.element.encode())
        .build();
  }

  public static CompoundBinaryTag getRegistry() {
    return CompoundBinaryTag.builder()
        .putString("type", "minecraft:worldgen/biome")
        .put("value", ListBinaryTag.from(Arrays.stream(Biome.values()).map(Biome::encodeBiome).collect(Collectors.toList())))
        .build();
  }

  public String getName() {
    return this.name;
  }

  public int getId() {
    return this.id;
  }

  public Element getElement() {
    return this.element;
  }

  public static class Element {

    public final String precipitation;
    public final float depth;
    public final float temperature;
    public final float scale;
    public final float downfall;
    public final String category;
    public final Effects effects;

    public Element(String precipitation, float depth, float temperature, float scale, float downfall, String category, Effects effects) {
      this.precipitation = precipitation;
      this.depth = depth;
      this.temperature = temperature;
      this.scale = scale;
      this.downfall = downfall;
      this.category = category;
      this.effects = effects;
    }

    public CompoundBinaryTag encode() {
      return CompoundBinaryTag.builder()
          .putString("precipitation", this.precipitation)
          .putFloat("depth", this.depth)
          .putFloat("temperature", this.temperature)
          .putFloat("scale", this.scale)
          .putFloat("downfall", this.downfall)
          .putString("category", this.category)
          .put("effects", this.effects.encode())
          .build();
    }

    public String getPrecipitation() {
      return this.precipitation;
    }

    public float getDepth() {
      return this.depth;
    }

    public float getTemperature() {
      return this.temperature;
    }

    public float getScale() {
      return this.scale;
    }

    public float getDownfall() {
      return this.downfall;
    }

    public String getCategory() {
      return this.category;
    }

    public Effects getEffects() {
      return this.effects;
    }
  }

  public static class Effects {

    private final int skyColor;
    private final int waterFogColor;
    private final int fogColor;
    private final int waterColor;

    @Nullable
    private final Integer foliageColor;
    @Nullable
    private final String grassColorModifier;
    @Nullable
    private final Music music;
    @Nullable
    private final String ambientSound;
    @Nullable
    private final AdditionsSound additionsSound;
    @Nullable
    private final MoodSound moodSound;
    @Nullable
    private final Particle particle;

    public Effects(int skyColor,
                   int waterFogColor, int fogColor, int waterColor,
                   @Nullable Integer foliageColor, @Nullable String grassColorModifier, @Nullable Music music,
                   @Nullable String ambientSound, @Nullable AdditionsSound additionsSound,
                   @Nullable MoodSound moodSound, @Nullable Particle particle) {
      this.skyColor = skyColor;
      this.waterFogColor = waterFogColor;
      this.fogColor = fogColor;
      this.waterColor = waterColor;
      this.foliageColor = foliageColor;
      this.grassColorModifier = grassColorModifier;
      this.music = music;
      this.ambientSound = ambientSound;
      this.additionsSound = additionsSound;
      this.moodSound = moodSound;
      this.particle = particle;
    }

    public CompoundBinaryTag encode() {
      Builder result = CompoundBinaryTag.builder();
      result.putInt("sky_color", this.skyColor)
          .putInt("water_fog_color", this.waterColor)
          .putInt("fog_color", this.fogColor)
          .putInt("water_color", this.waterColor);

      if (this.foliageColor != null) {
        result.putInt("foliage_color", this.foliageColor);
      }

      if (this.grassColorModifier != null) {
        result.putString("grass_color_modifier", this.grassColorModifier);
      }

      if (this.music != null) {
        result.put("music", this.music.encode());
      }

      if (this.ambientSound != null) {
        result.putString("ambient_sound", this.ambientSound);
      }

      if (this.additionsSound != null) {
        result.put("additions_sound", this.additionsSound.encode());
      }

      if (this.moodSound != null) {
        result.put("mood_sound", this.moodSound.encode());
      }

      if (this.particle != null) {
        result.put("particle", this.particle.encode());
      }

      return result.build();
    }

    public static EffectsBuilder builder(int skyColor, int waterFogColor, int fogColor, int waterColor) {
      return new EffectsBuilder()
          .skyColor(skyColor)
          .waterFogColor(waterFogColor)
          .fogColor(fogColor)
          .waterColor(waterColor);
    }

    public int getSkyColor() {
      return this.skyColor;
    }

    public int getWaterFogColor() {
      return this.waterFogColor;
    }

    public int getFogColor() {
      return this.fogColor;
    }

    public int getWaterColor() {
      return this.waterColor;
    }

    @Nullable
    public Integer getFoliageColor() {
      return this.foliageColor;
    }

    @Nullable
    public String getGrassColorModifier() {
      return this.grassColorModifier;
    }

    @Nullable
    public Music getMusic() {
      return this.music;
    }

    @Nullable
    public String getAmbientSound() {
      return this.ambientSound;
    }

    @Nullable
    public AdditionsSound getAdditionsSound() {
      return this.additionsSound;
    }

    @Nullable
    public MoodSound getMoodSound() {
      return this.moodSound;
    }

    @Nullable
    public Particle getParticle() {
      return this.particle;
    }

    @Override
    public String toString() {
      return "Biome.Effects{"
          + "skyColor=" + this.getSkyColor()
          + ", waterFogColor=" + this.getWaterFogColor()
          + ", fogColor=" + this.getFogColor()
          + ", waterColor=" + this.getWaterColor()
          + ", foliageColor=" + this.getFoliageColor()
          + ", grassColorModifier=" + this.getGrassColorModifier()
          + ", music=" + this.getMusic()
          + ", ambientSound=" + this.getAmbientSound()
          + ", additionsSound=" + this.getAdditionsSound()
          + ", moodSound=" + this.getMoodSound()
          + ", particle=" + this.getParticle()
          + "}";
    }

    public static class MoodSound {

      private final int tickDelay;
      private final double offset;
      private final int blockSearchExtent;
      @NonNull
      private final String sound;

      private MoodSound(int tickDelay, double offset, int blockSearchExtent, @NonNull String sound) {
        this.tickDelay = tickDelay;
        this.offset = offset;
        this.blockSearchExtent = blockSearchExtent;
        this.sound = sound;
      }

      public static MoodSound of(int tickDelay, double offset, int blockSearchExtent, @NonNull String sound) {
        return new MoodSound(tickDelay, offset, blockSearchExtent, sound);
      }

      public CompoundBinaryTag encode() {
        return CompoundBinaryTag.builder()
            .putInt("tick_delay", this.tickDelay)
            .putDouble("offset", this.offset)
            .putInt("block_search_extent", this.blockSearchExtent)
            .putString("sound", this.sound)
            .build();
      }

      public int getTickDelay() {
        return this.tickDelay;
      }

      public double getOffset() {
        return this.offset;
      }

      public int getBlockSearchExtent() {
        return this.blockSearchExtent;
      }

      @NonNull
      public String getSound() {
        return this.sound;
      }

      @Override
      public String toString() {
        return "Biome.Effects.MoodSound{"
            + "tickDelay=" + this.getTickDelay()
            + ", offset=" + this.getOffset()
            + ", blockSearchExtent=" + this.getBlockSearchExtent()
            + ", sound=" + this.getSound()
            + "}";
      }
    }

    public static class Music {

      private final boolean replaceCurrentMusic;
      @NonNull
      private final String sound;
      private final int maxDelay;
      private final int minDelay;

      private Music(boolean replaceCurrentMusic, @NonNull String sound, int maxDelay, int minDelay) {
        this.replaceCurrentMusic = replaceCurrentMusic;
        this.sound = sound;
        this.maxDelay = maxDelay;
        this.minDelay = minDelay;
      }

      public static Music of(boolean replaceCurrentMusic, @NonNull String sound, int maxDelay, int minDelay) {
        return new Music(replaceCurrentMusic, sound, maxDelay, minDelay);
      }

      public CompoundBinaryTag encode() {
        return CompoundBinaryTag.builder()
            .putBoolean("replace_current_music", this.replaceCurrentMusic)
            .putString("sound", this.sound)
            .putInt("max_delay", this.maxDelay)
            .putInt("min_delay", this.minDelay)
            .build();
      }

      public boolean isReplaceCurrentMusic() {
        return this.replaceCurrentMusic;
      }

      @NonNull
      public String getSound() {
        return this.sound;
      }

      public int getMaxDelay() {
        return this.maxDelay;
      }

      public int getMinDelay() {
        return this.minDelay;
      }

      @Override
      public String toString() {
        return "Biome.Effects.Music{"
            + "replaceCurrentMusic=" + this.isReplaceCurrentMusic()
            + ", sound=" + this.getSound()
            + ", maxDelay=" + this.getMaxDelay()
            + ", minDelay=" + this.getMinDelay()
            + "}";
      }
    }

    public static class AdditionsSound {

      @NonNull
      private final String sound;
      private final double tickChance;

      private AdditionsSound(@NonNull String sound, double tickChance) {
        this.sound = sound;
        this.tickChance = tickChance;
      }

      public static AdditionsSound of(@NonNull String sound, double tickChance) {
        return new AdditionsSound(sound, tickChance);
      }

      public CompoundBinaryTag encode() {
        return CompoundBinaryTag.builder()
            .putString("sound", this.sound)
            .putDouble("tick_chance", this.tickChance)
            .build();
      }

      @NonNull
      public String getSound() {
        return this.sound;
      }

      public double getTickChance() {
        return this.tickChance;
      }

      @Override
      public String toString() {
        return "Biome.Effects.AdditionsSound{"
            + "sound=" + this.getSound()
            + ", tickChance=" + this.getTickChance()
            + "}";
      }
    }

    public static class Particle {

      private final float probability;
      @NonNull
      private final ParticleOptions options;

      private Particle(float probability, @NonNull ParticleOptions options) {
        this.probability = probability;
        this.options = options;
      }

      public static Particle of(float probability, @NonNull ParticleOptions options) {
        return new Particle(probability, options);
      }

      public CompoundBinaryTag encode() {
        return CompoundBinaryTag.builder()
            .putFloat("probability", this.probability)
            .put("options", this.options.encode())
            .build();
      }

      public float getProbability() {
        return this.probability;
      }

      @NonNull
      public ParticleOptions getOptions() {
        return this.options;
      }

      @Override
      public String toString() {
        return "Biome.Effects.Particle{"
            + "probability=" + this.getProbability()
            + ", options=" + this.getOptions()
            + "}";
      }

      public static class ParticleOptions {

        @NonNull
        private final String type;

        public ParticleOptions(@NonNull String type) {
          this.type = type;
        }

        public CompoundBinaryTag encode() {
          return CompoundBinaryTag.builder()
              .putString("type", this.type)
              .build();
        }

        @NonNull
        public String getType() {
          return this.type;
        }

        @Override
        public String toString() {
          return "Biome.Effects.Particle.ParticleOptions{"
              + "type=" + this.getType()
              + "}";
        }
      }
    }

    public static class EffectsBuilder {

      private int skyColor;
      private int waterFogColor;
      private int fogColor;
      private int waterColor;
      private Integer foliageColor;
      private String grassColorModifier;
      private Music music;
      private String ambientSound;
      private AdditionsSound additionsSound;
      private MoodSound moodSound;
      private Particle particle;

      EffectsBuilder() {

      }

      public EffectsBuilder skyColor(int skyColor) {
        this.skyColor = skyColor;
        return this;
      }

      public EffectsBuilder waterFogColor(int waterFogColor) {
        this.waterFogColor = waterFogColor;
        return this;
      }

      public EffectsBuilder fogColor(int fogColor) {
        this.fogColor = fogColor;
        return this;
      }

      public EffectsBuilder waterColor(int waterColor) {
        this.waterColor = waterColor;
        return this;
      }

      public EffectsBuilder foliageColor(Integer foliageColor) {
        this.foliageColor = foliageColor;
        return this;
      }

      public EffectsBuilder grassColorModifier(String grassColorModifier) {
        this.grassColorModifier = grassColorModifier;
        return this;
      }

      public EffectsBuilder music(Music music) {
        this.music = music;
        return this;
      }

      public EffectsBuilder ambientSound(String ambientSound) {
        this.ambientSound = ambientSound;
        return this;
      }

      public EffectsBuilder additionsSound(AdditionsSound additionsSound) {
        this.additionsSound = additionsSound;
        return this;
      }

      public EffectsBuilder moodSound(MoodSound moodSound) {
        this.moodSound = moodSound;
        return this;
      }

      public EffectsBuilder particle(Particle particle) {
        this.particle = particle;
        return this;
      }

      public Effects build() {
        return new Effects(
            this.skyColor,
            this.waterFogColor,
            this.fogColor,
            this.waterColor,
            this.foliageColor,
            this.grassColorModifier,
            this.music,
            this.ambientSound,
            this.additionsSound,
            this.moodSound,
            this.particle
        );
      }

      @Override
      public String toString() {
        return "Biome.Effects.EffectsBuilder{"
            + "skyColor=" + this.skyColor
            + ", waterFogColor=" + this.waterFogColor
            + ", fogColor=" + this.fogColor
            + ", waterColor=" + this.waterColor
            + ", foliageColor=" + this.foliageColor
            + ", grassColorModifier=" + this.grassColorModifier
            + ", music=" + this.music
            + ", ambientSound=" + this.ambientSound
            + ", additionsSound=" + this.additionsSound
            + ", moodSound=" + this.moodSound
            + ", particle=" + this.particle
            + "}";
      }
    }
  }
}
