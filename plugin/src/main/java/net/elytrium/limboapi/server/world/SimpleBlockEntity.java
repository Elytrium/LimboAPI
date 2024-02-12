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

package net.elytrium.limboapi.server.world;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.velocitypowered.api.network.ProtocolVersion;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import net.elytrium.limboapi.LimboAPI;
import net.elytrium.limboapi.api.chunk.BlockEntityVersion;
import net.elytrium.limboapi.api.chunk.VirtualBlockEntity;
import net.kyori.adventure.nbt.CompoundBinaryTag;

public class SimpleBlockEntity implements VirtualBlockEntity {

  private static final Gson GSON = new Gson();

  private static final Map<String, SimpleBlockEntity> MODERN_ID_MAP = new HashMap<>();

  private final String modernId;
  private final Map<BlockEntityVersion, Integer> versionIDs = new EnumMap<>(BlockEntityVersion.class);

  public SimpleBlockEntity(String modernId) {
    this.modernId = modernId;
  }

  @Override
  public int getID(ProtocolVersion version) {
    return this.getID(BlockEntityVersion.from(version));
  }

  @Override
  public int getID(BlockEntityVersion version) {
    return this.versionIDs.get(version);
  }

  @Override
  public boolean isSupportedOn(ProtocolVersion version) {
    return this.versionIDs.containsKey(BlockEntityVersion.from(version));
  }

  @Override
  public boolean isSupportedOn(BlockEntityVersion version) {
    return this.versionIDs.containsKey(version);
  }

  @Override
  public String getModernID() {
    return this.modernId;
  }

  @Override
  public VirtualBlockEntity.Entry getEntry(int posX, int posY, int posZ, CompoundBinaryTag nbt) {
    return new Entry(posX, posY, posZ, nbt);
  }

  @SuppressWarnings("unchecked")
  public static void init() {
    LinkedTreeMap<String, LinkedTreeMap<String, String>> blockEntitiesMapping = GSON.fromJson(
        new InputStreamReader(
            Objects.requireNonNull(LimboAPI.class.getResourceAsStream("/mapping/blockentities_mapping.json")), StandardCharsets.UTF_8
        ),
        LinkedTreeMap.class
    );

    blockEntitiesMapping.forEach((modernId, protocols) -> {
      SimpleBlockEntity simpleBlockEntity = new SimpleBlockEntity(modernId);
      protocols.forEach((key, value) -> simpleBlockEntity.versionIDs.put(BlockEntityVersion.parse(key), Integer.parseInt(value)));
      MODERN_ID_MAP.put(modernId, simpleBlockEntity);
    });
  }

  public static SimpleBlockEntity fromModernID(String id) {
    return MODERN_ID_MAP.get(id);
  }

  public class Entry implements VirtualBlockEntity.Entry {
    private final int posX;
    private final int posY;
    private final int posZ;
    private final CompoundBinaryTag nbt;

    public Entry(int posX, int posY, int posZ, CompoundBinaryTag nbt) {
      this.posX = posX;
      this.posY = posY;
      this.posZ = posZ;
      this.nbt = nbt;
    }

    @Override
    public VirtualBlockEntity getBlockEntity() {
      return SimpleBlockEntity.this;
    }

    @Override
    public int getPosX() {
      return this.posX;
    }

    @Override
    public int getPosY() {
      return this.posY;
    }

    @Override
    public int getPosZ() {
      return this.posZ;
    }

    @Override
    public CompoundBinaryTag getNbt() {
      return this.nbt;
    }

    @Override
    public int getID(ProtocolVersion version) {
      return SimpleBlockEntity.this.getID(version);
    }

    @Override
    public int getID(BlockEntityVersion version) {
      return SimpleBlockEntity.this.getID(version);
    }

    @Override
    public boolean isSupportedOn(ProtocolVersion version) {
      return SimpleBlockEntity.this.isSupportedOn(version);
    }

    @Override
    public boolean isSupportedOn(BlockEntityVersion version) {
      return SimpleBlockEntity.this.isSupportedOn(version);
    }
  }
}
