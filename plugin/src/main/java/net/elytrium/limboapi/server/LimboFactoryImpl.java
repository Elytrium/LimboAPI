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


package net.elytrium.limboapi.server;

import lombok.AllArgsConstructor;
import net.elytrium.limboapi.LimboAPI;
import net.elytrium.limboapi.api.chunk.Dimension;
import net.elytrium.limboapi.api.chunk.VirtualBlock;
import net.elytrium.limboapi.api.chunk.VirtualWorld;
import net.elytrium.limboapi.api.material.Block;
import net.elytrium.limboapi.api.material.Item;
import net.elytrium.limboapi.api.material.VirtualItem;
import net.elytrium.limboapi.api.LimboFactory;
import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.server.world.SimpleBlock;
import net.elytrium.limboapi.server.world.SimpleItem;
import net.elytrium.limboapi.server.world.SimpleWorld;

@AllArgsConstructor
public class LimboFactoryImpl implements LimboFactory {

  private final LimboAPI limboAPI;

  public VirtualBlock createSimpleBlock(Block block) {
    return SimpleBlock
        .fromLegacyId((short) block.getId())
        .setData(block.getData());
  }

  public VirtualBlock createSimpleBlock(short legacyId, byte data) {
    return SimpleBlock
        .fromLegacyId(legacyId)
        .setData(data);
  }

  public VirtualBlock createSimpleBlock(
      boolean solid, boolean air, boolean motionBlocking, SimpleBlock.BlockInfo... blockInfos) {
    return new SimpleBlock(solid, air, motionBlocking, blockInfos);
  }

  public VirtualItem getItem(Item item) {
    return SimpleItem.fromItem(item);
  }

  public Limbo createVirtualServer(VirtualWorld world) {
    return new LimboImpl(limboAPI, world);
  }

  public VirtualWorld createVirtualWorld(Dimension dimension, double x, double y, double z, float yaw, float pitch) {
    return new SimpleWorld(dimension, x, y, z, yaw, pitch);
  }
}
