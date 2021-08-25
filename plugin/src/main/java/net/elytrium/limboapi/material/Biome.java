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

import java.util.Arrays;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.elytrium.limboapi.api.chunk.VirtualBiome;
import net.elytrium.limboapi.material.Biome.Effects.MoodSound;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag.Builder;
import net.kyori.adventure.nbt.ListBinaryTag;

@Getter
public enum Biome implements VirtualBiome {

  PLAINS("minecraft:plains", 1,
      new Element("rain", 0.125f, 0.8f, 0.05f, 0.4f, "plains",
          Effects.builder(7907327, 329011, 12638463, 415920)
              .moodSound(MoodSound.of(6000, 2.0d, 8, "minecraft:ambient.cave"))
              .build())
  ),
  SWAMP("minecraft:swamp", 6,
      new Element("rain", -0.2F, 0.8f, 0.1F, 0.9F, "swamp",
          Effects.builder(7907327, 329011, 12638463, 415920).grassColorModifier("swamp")
              .foliageColor(6975545)
              .moodSound(MoodSound.of(6000, 2.0d, 8, "minecraft:ambient.cave"))
              .build())
  ),
  SWAMP_HILLS("minecraft:swamp_hills", 134,
      new Element("rain", -0.1F, 0.8f, 0.3F, 0.9F, "swamp",
          Effects.builder(7907327, 329011, 12638463, 415920).grassColorModifier("swamp")
              .foliageColor(6975545)
              .moodSound(MoodSound.of(6000, 2.0d, 8, "minecraft:ambient.cave"))
              .build())
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
        .putString("name", name)
        .putInt("id", id)
        .put("element", element.encode()).build();
  }

  public static CompoundBinaryTag getRegistry() {
    return CompoundBinaryTag.builder()
        .putString("type", "minecraft:worldgen/biome")
        .put("value", ListBinaryTag
            .from(Arrays.stream(Biome.values()).map(Biome::encodeBiome)
                .collect(Collectors.toList()))).build();
  }

  @Getter
  public static class Element {

    public final String precipitation;
    public final float depth;
    public final float temperature;
    public final float scale;
    public final float downfall;
    public final String category;
    public final Effects effects;

    public Element(String precipitation, float depth, float temperature,
                   float scale, float downfall, String category, Effects effects) {
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
          .putString("precipitation", precipitation)
          .putFloat("depth", depth)
          .putFloat("temperature", temperature)
          .putFloat("scale", scale)
          .putFloat("downfall", downfall)
          .putString("category", category)
          .put("effects", effects.encode()).build();
    }
  }

  @Data
  @RequiredArgsConstructor
  @lombok.Builder
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

    public CompoundBinaryTag encode() {
      Builder result = CompoundBinaryTag.builder();
      result.putInt("sky_color", skyColor)
          .putInt("water_fog_color", waterColor)
          .putInt("fog_color", fogColor)
          .putInt("water_color", waterColor);

      if (foliageColor != null) {
        result.putInt("foliage_color", foliageColor);
      }
      if (grassColorModifier != null) {
        result.putString("grass_color_modifier", grassColorModifier);
      }
      if (music != null) {
        result.put("music", music.encode());
      }
      if (ambientSound != null) {
        result.putString("ambient_sound", ambientSound);
      }
      if (additionsSound != null) {
        result.put("additions_sound", additionsSound.encode());
      }
      if (moodSound != null) {
        result.put("mood_sound", moodSound.encode());
      }
      if (particle != null) {
        result.put("particle", particle.encode());
      }
      return result.build();
    }

    public static EffectsBuilder builder(int skyColor, int waterFogColor, int fogColor, int waterColor) {
      return new EffectsBuilder().skyColor(skyColor).waterFogColor(waterFogColor).fogColor(fogColor)
          .waterColor(waterColor);
    }

    @Data
    @RequiredArgsConstructor(staticName = "of")
    public static class MoodSound {

      private final int tickDelay;
      private final double offset;
      private final int blockSearchExtent;
      @NonNull
      private final String sound;

      public CompoundBinaryTag encode() {
        return CompoundBinaryTag.builder()
            .putInt("tick_delay", tickDelay)
            .putDouble("offset", offset)
            .putInt("block_search_extent", blockSearchExtent)
            .putString("sound", sound).build();
      }
    }

    @Data
    @RequiredArgsConstructor(staticName = "of")
    public static class Music {

      private final boolean replaceCurrentMusic;
      @NonNull
      private final String sound;
      private final int maxDelay;
      private final int minDelay;

      public CompoundBinaryTag encode() {
        return CompoundBinaryTag.builder()
            .putBoolean("replace_current_music", replaceCurrentMusic)
            .putString("sound", sound)
            .putInt("max_delay", maxDelay)
            .putInt("min_delay", minDelay).build();
      }
    }

    @Data
    @RequiredArgsConstructor(staticName = "of")
    public static class AdditionsSound {

      @NonNull
      private final String sound;
      private final double tickChance;

      public CompoundBinaryTag encode() {
        return CompoundBinaryTag.builder()
            .putString("sound", sound)
            .putDouble("tick_chance", tickChance).build();
      }
    }

    @Data
    @RequiredArgsConstructor(staticName = "of")
    public static class Particle {

      private final float probability;
      @NonNull
      private final ParticleOptions options;

      public CompoundBinaryTag encode() {
        return CompoundBinaryTag.builder()
            .putFloat("probability", probability)
            .put("options", options.encode()).build();
      }

      @Data
      @RequiredArgsConstructor
      public static class ParticleOptions {

        @NonNull
        private final String type;

        public CompoundBinaryTag encode() {
          return CompoundBinaryTag.builder()
              .putString("type", type).build();
        }
      }
    }
  }
}
