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
import com.velocitypowered.proxy.protocol.packet.chat.ComponentHolder;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.kyori.adventure.text.Component;

public class ComponentsItemComponent extends WriteableItemComponent<List<Component>> {

  public ComponentsItemComponent(String name) {
    super(name);
  }

  @Override
  public void write(ProtocolVersion version, ByteBuf buffer) {
    ProtocolUtils.writeVarInt(buffer, this.getValue().size());
    for (Component component : this.getValue()) {
      new ComponentHolder(version, component).write(buffer);
    }
  }
}
