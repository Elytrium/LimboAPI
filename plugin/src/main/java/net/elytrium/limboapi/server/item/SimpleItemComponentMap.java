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

package net.elytrium.limboapi.server.item;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.elytrium.limboapi.api.protocol.item.ItemComponent;
import net.elytrium.limboapi.api.protocol.item.ItemComponentMap;
import net.elytrium.limboapi.server.item.type.WriteableItemComponent;

public class SimpleItemComponentMap implements ItemComponentMap {

  private final List<WriteableItemComponent<?>> addedComponents = new ArrayList<>();
  private final List<WriteableItemComponent<?>> removedComponents = new ArrayList<>();
  private final SimpleItemComponentManager manager;

  public SimpleItemComponentMap(SimpleItemComponentManager manager) {
    this.manager = manager;
  }

  @Override
  public <T> ItemComponentMap add(ProtocolVersion version, String name, T value) {
    this.addedComponents.add((WriteableItemComponent<?>) this.manager.createComponent(version, name).setValue(value));
    return this;
  }

  @Override
  public ItemComponentMap remove(ProtocolVersion version, String name) {
    this.removedComponents.add(this.manager.createComponent(version, name));
    return null;
  }

  @Override
  public List<ItemComponent> getAdded() {
    return (List<ItemComponent>) (Object) this.addedComponents;
  }

  @Override
  public List<ItemComponent> getRemoved() {
    return (List<ItemComponent>) (Object) this.removedComponents;
  }

  @Override
  public void read(ProtocolVersion version, Object buffer) {
    // TODO: implement
    throw new UnsupportedOperationException("read");
  }

  @Override
  public void write(ProtocolVersion version, Object buffer) {
    ByteBuf buf = (ByteBuf) buffer;

    ProtocolUtils.writeVarInt(buf, this.getAdded().size());
    ProtocolUtils.writeVarInt(buf, this.getRemoved().size());

    for (WriteableItemComponent<?> component : this.addedComponents) {
      ProtocolUtils.writeVarInt(buf, this.manager.getId(component.getName(), version));
      component.write(version, buf);
    }

    for (WriteableItemComponent<?> component : this.removedComponents) {
      ProtocolUtils.writeVarInt(buf, this.manager.getId(component.getName(), version));
    }
  }
}
