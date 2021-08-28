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
import lombok.Getter;
import net.elytrium.limboapi.LimboAPI;
import net.elytrium.limboapi.api.chunk.VirtualBlock;

public class SimpleBlock implements VirtualBlock {

  public static final SimpleBlock AIR = air(BlockInfo.info(Version.LEGACY, 0));

  private static final Gson gson = new Gson();
  private static final HashMap<Short, SimpleBlock> legacyIDsMap = new HashMap<>();

  public static void init() {
    //noinspection unchecked,ConstantConditions
    LinkedTreeMap<String, LinkedTreeMap<String, String>> map = gson.fromJson(
        new InputStreamReader(
            LimboAPI.getInstance().getClass()
                .getClassLoader().getResourceAsStream("mapping/blocks.json"),
            StandardCharsets.UTF_8), LinkedTreeMap.class);

    map.forEach((legacyBlockId, versionMap) -> {
      BlockInfo[] info = versionMap.entrySet().stream()
          .map(e -> BlockInfo.info(Version.parse(e.getKey()), Integer.parseInt(e.getValue())))
          .toArray(BlockInfo[]::new);

      legacyIDsMap.put(Short.valueOf(legacyBlockId), solid(info));
    });

    legacyIDsMap.put((short) 0, AIR);
  }

  @Getter
  private final Map<Version, BlockInfo> blockInfos;
  private final boolean solid;
  private final boolean air;
  private final boolean motionBlocking; //1.14+

  public SimpleBlock(boolean solid, boolean air, boolean motionBlocking, BlockInfo... blockInfos) {
    this.motionBlocking = motionBlocking;
    this.blockInfos = new EnumMap<>(Version.class);
    for (BlockInfo info : blockInfos) {
      for (Version version : EnumSet.range(info.getVersion(), Version.MINECRAFT_1_17)) {
        this.blockInfos.put(version, info);
      }
    }
    this.air = air;
    this.solid = solid;
  }

  public SimpleBlock(SimpleBlock block) {
    this.motionBlocking = block.motionBlocking;
    this.blockInfos = Map.copyOf(block.blockInfos);
    this.air = block.air;
    this.solid = block.solid;
  }

  public short getId(Version version) {
    return this.blockInfos.get(version).getId();
  }

  public short getId(ProtocolVersion version) {
    return getId(Version.map(version));
  }

  public SimpleBlock setData(byte data) {
    this.blockInfos.forEach((e, k) -> k.setData(data));
    return this;
  }

  public byte getData(Version version) {
    return this.blockInfos.get(version).getData();
  }

  public byte getData(ProtocolVersion version) {
    return getData(Version.map(version));
  }

  public boolean isSolid() {
    return this.solid;
  }

  public boolean isAir() {
    return this.air;
  }

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
    return legacyIDsMap.get(id);
  }

  @NonNull
  public static SimpleBlock air(BlockInfo... infos) {
    return new SimpleBlock(false, true, false, infos);
  }
}
