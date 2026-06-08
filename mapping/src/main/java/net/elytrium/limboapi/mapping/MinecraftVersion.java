/*
 * Copyright (C) 2021 - 2025 Elytrium
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

package net.elytrium.limboapi.mapping;

import java.util.List;

public enum MinecraftVersion {
  MINECRAFT_1_7_2(4),
  MINECRAFT_1_7_6(5),
  MINECRAFT_1_8(47),
  MINECRAFT_1_9(107),
  MINECRAFT_1_9_1(108),
  MINECRAFT_1_9_2(109),
  MINECRAFT_1_9_4(110),
  MINECRAFT_1_10(210),
  MINECRAFT_1_11(315),
  MINECRAFT_1_11_1(316),
  MINECRAFT_1_12(335),
  MINECRAFT_1_12_1(338),
  MINECRAFT_1_12_2(340),
  MINECRAFT_1_13(393),
  MINECRAFT_1_13_1(401),
  MINECRAFT_1_13_2(404),
  MINECRAFT_1_14(477),
  MINECRAFT_1_14_1(480),
  MINECRAFT_1_14_2(485),
  MINECRAFT_1_14_3(490),
  MINECRAFT_1_14_4(498),
  MINECRAFT_1_15(573),
  MINECRAFT_1_15_1(575),
  MINECRAFT_1_15_2(578),
  MINECRAFT_1_16(735),
  MINECRAFT_1_16_1(736),
  MINECRAFT_1_16_2(751),
  MINECRAFT_1_16_3(753),
  MINECRAFT_1_16_4(754),
  MINECRAFT_1_17(755),
  MINECRAFT_1_17_1(756),
  MINECRAFT_1_18(757),
  MINECRAFT_1_18_2(758),
  MINECRAFT_1_19(759),
  MINECRAFT_1_19_1(760),
  MINECRAFT_1_19_3(761),
  MINECRAFT_1_19_4(762),
  MINECRAFT_1_20(763),
  MINECRAFT_1_20_2(764),
  MINECRAFT_1_20_3(765),
  MINECRAFT_1_20_5(766),
  MINECRAFT_1_21(767),
  MINECRAFT_1_21_2(768),
  MINECRAFT_1_21_4(769),
  MINECRAFT_1_21_5(770),
  MINECRAFT_1_21_6(771),
  MINECRAFT_1_21_7(772),
  MINECRAFT_1_21_9(773),
  MINECRAFT_1_21_11(774),
  MINECRAFT_26_1(774);

  public static final List<MinecraftVersion> WORLD_VERSIONS = List.of(
      MINECRAFT_1_13,
      MINECRAFT_1_13_2,
      MINECRAFT_1_14,
      MINECRAFT_1_15,
      MINECRAFT_1_16,
      MINECRAFT_1_16_2,
      MINECRAFT_1_17,
      MINECRAFT_1_19,
      MINECRAFT_1_19_3,
      MINECRAFT_1_19_4,
      MINECRAFT_1_20,
      MINECRAFT_1_20_3,
      MINECRAFT_1_20_5,
      MINECRAFT_1_21_2,
      MINECRAFT_1_21_4,
      MINECRAFT_1_21_5,
      MINECRAFT_1_21_6,
      MINECRAFT_1_21_7,
      MINECRAFT_1_21_9,
      MINECRAFT_1_21_11,
      MINECRAFT_26_1
  );

  public static final MinecraftVersion MINIMUM_VERSION = MINECRAFT_1_7_2;
  public static final MinecraftVersion MAXIMUM_VERSION = values()[values().length - 1];

  private final String versionName = name().substring(10).replace('_', '.');
  private final int protocolVersion;

  MinecraftVersion(int protocolVersion) {
    this.protocolVersion = protocolVersion;
  }

  public static MinecraftVersion fromVersionName(String name) {
    return valueOf("MINECRAFT_" + name.replace('.', '_'));
  }

  public int getProtocolVersion() {
    return this.protocolVersion;
  }

  public String getVersionName() {
    return this.versionName;
  }

  public boolean isAtLeast(MinecraftVersion other) {
    return this.compareTo(other) >= 0;
  }

  public MinecraftVersion previous() {
    if (this.ordinal() == 0) {
      throw new IllegalStateException("No previous version for " + this);
    }

    return values()[this.ordinal() - 1];
  }
}

