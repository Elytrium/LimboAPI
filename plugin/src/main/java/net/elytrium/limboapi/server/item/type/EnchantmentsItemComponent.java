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

package net.elytrium.limboapi.server.item.type;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.Pair;
import java.util.List;

public class EnchantmentsItemComponent extends WriteableItemComponent<Pair<List<Pair<Integer, Integer>>, Boolean>> {

  public EnchantmentsItemComponent(String name) {
    super(name);
  }

  @Override
  public void write(ProtocolVersion version, ByteBuf buffer) {
    ProtocolUtils.writeVarInt(buffer, this.getValue().left().size());
    for (Pair<Integer, Integer> enchantment : this.getValue().left()) {
      ProtocolUtils.writeVarInt(buffer, enchantment.left());
      ProtocolUtils.writeVarInt(buffer, enchantment.right());
    }
    buffer.writeBoolean(this.getValue().right());
  }
}
