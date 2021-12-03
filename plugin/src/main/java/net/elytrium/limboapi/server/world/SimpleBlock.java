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

package net.elytrium.limboapi.server.world;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.velocitypowered.api.network.ProtocolVersion;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import net.elytrium.limboapi.LimboAPI;
import net.elytrium.limboapi.api.chunk.VirtualBlock;

@SuppressWarnings("unused")
public class SimpleBlock implements VirtualBlock {

  public static final SimpleBlock AIR = air(BlockInfo.info(Version.LEGACY, 0));

  private static final Gson gson = new Gson();
  private static final HashMap<Short, SimpleBlock> legacyIDsMap = new HashMap<>();

  @SuppressWarnings("unchecked")
  public static void init() {
    LinkedTreeMap<String, LinkedTreeMap<String, String>> map = gson.fromJson(
        new InputStreamReader(
            Objects.requireNonNull(LimboAPI.getInstance().getClass().getClassLoader().getResourceAsStream("mapping/blocks.json")),
            StandardCharsets.UTF_8
        ), LinkedTreeMap.class);

    map.forEach((legacyBlockId, versionMap) -> {
      BlockInfo[] info = versionMap.entrySet().stream()
          .map(e -> BlockInfo.info(Version.parse(e.getKey()), Integer.parseInt(e.getValue())))
          .toArray(BlockInfo[]::new);

      legacyIDsMap.put(Short.valueOf(legacyBlockId), solid(info));
    });

    legacyIDsMap.put((short) 0, AIR);
  }

  private final Map<Version, BlockInfo> blockInfos;
  private final boolean solid;
  private final boolean air;
  private final boolean motionBlocking; // 1.14+

  public SimpleBlock(boolean solid, boolean air, boolean motionBlocking, BlockInfo... blockInfos) {
    this.blockInfos = new EnumMap<>(Version.class);
    this.solid = solid;
    this.air = air;
    this.motionBlocking = motionBlocking;

    for (BlockInfo info : blockInfos) {
      for (Version version : EnumSet.range(info.getVersion(), Version.MINECRAFT_1_17)) {
        this.blockInfos.put(version, info);
      }
    }
  }

  public SimpleBlock(SimpleBlock block) {
    this.blockInfos = Map.copyOf(block.blockInfos);
    this.solid = block.solid;
    this.air = block.air;
    this.motionBlocking = block.motionBlocking;
  }

  @Override
  public SimpleBlock setData(byte data) {
    this.blockInfos.forEach((e, k) -> k.setData(data));
    return this;
  }

  @Override
  public byte getData(Version version) {
    return this.blockInfos.get(version).getData();
  }

  @Override
  public byte getData(ProtocolVersion version) {
    return this.getData(Version.map(version));
  }

  @Override
  public Map<Version, BlockInfo> getBlockInfos() {
    return this.blockInfos;
  }

  @Override
  public short getId(Version version) {
    return this.blockInfos.get(version).getId();
  }

  @Override
  public short getId(ProtocolVersion version) {
    return this.getId(Version.map(version));
  }

  @Override
  public boolean isSolid() {
    return this.solid;
  }

  @Override
  public boolean isAir() {
    return this.air;
  }

  @Override
  public boolean isMotionBlocking() {
    return this.motionBlocking;
  }

  @NonNull
  public static SimpleBlock solid(BlockInfo... infos) {
    return solid(true, infos);
  }

  @NonNull
  public static SimpleBlock solid(boolean motionBlocking, BlockInfo... infos) {
    return new SimpleBlock(true, false, motionBlocking, infos);
  }

  @NonNull
  public static SimpleBlock nonSolid(BlockInfo... infos) {
    return nonSolid(true, infos);
  }

  @NonNull
  public static SimpleBlock nonSolid(boolean motionBlocking, BlockInfo... infos) {
    return new SimpleBlock(false, false, motionBlocking, infos);
  }

  @NonNull
  public static SimpleBlock fromLegacyId(short id) {
    if (legacyIDsMap.containsKey(id)) {
      return legacyIDsMap.get(id);
    } else {
      LimboAPI.getInstance().getLogger().warn("Block #" + id + " is not supported, and was replaced with air");
      return AIR;
    }
  }

  @NonNull
  public static SimpleBlock air(BlockInfo... infos) {
    return new SimpleBlock(false, true, false, infos);
  }
}
