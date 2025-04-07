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

package net.elytrium.limboapi.server.item.type;

import com.velocitypowered.api.network.ProtocolVersion;
import io.netty.buffer.ByteBuf;
import net.elytrium.limboapi.api.protocol.item.ItemComponent;

public abstract class WriteableItemComponent<T> implements ItemComponent<T> {

  private final String name;
  private T value;

  public WriteableItemComponent(String name) {
    this.name = name;
  }

  public abstract void write(ProtocolVersion version, ByteBuf buffer);

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public ItemComponent<T> setValue(T value) {
    this.value = value;
    return this;
  }

  @Override
  public T getValue() {
    return this.value;
  }
}
