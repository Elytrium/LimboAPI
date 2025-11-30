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

package net.elytrium.limboapi.server.item;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import net.elytrium.limboapi.api.world.item.datacomponent.DataComponentType;
import net.elytrium.limboapi.api.world.item.datacomponent.DataComponentMap;
import net.elytrium.limboapi.utils.Unit;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class SimpleDataComponentMap implements DataComponentMap {

  private Map<DataComponentType, @Nullable Object> components;

  @Override
  public void setData(DataComponentType.NonValued type) {
    if (this.components == null) {
      this.components = new HashMap<>();
    }

    this.components.put(type, Unit.INSTANCE);
  }

  @Override
  public <T> void setData(DataComponentType.Valued<@NonNull T> type, @NonNull T value) {
    Objects.requireNonNull(value, "value cannot be null");
    if (this.components == null) {
      this.components = new HashMap<>();
    }

    this.components.put(type, value);
  }

  @Override
  public void unsetData(DataComponentType type) {
    if (this.components == null) {
      this.components = new HashMap<>();
    }

    this.components.put(type, null);
  }

  @Override
  public void resetData(DataComponentType type) {
    if (this.components != null && !this.components.isEmpty()) {
      this.components.remove(type);
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getData(DataComponentType.Valued<@NonNull T> type) {
    return this.components == null || this.components.isEmpty() ? null : (T) this.components.get(type);
  }

  @Override
  public boolean hasData(DataComponentType type) {
    return this.components != null && !this.components.isEmpty() && this.components.containsKey(type);
  }

  @Override
  public DataComponentMap read(Object bufObj, ProtocolVersion version) {
    ByteBuf buf = (ByteBuf) bufObj;

    int added = ProtocolUtils.readVarInt(buf);
    int removed = ProtocolUtils.readVarInt(buf);
    if (added != 0 || removed != 0) {
      this.read(buf, version, added, removed);
    }

    return this;
  }

  private void read(ByteBuf buf, ProtocolVersion version, int added, int removed) {
    if (this.components == null) {
      this.components = new HashMap<>(added + removed);
    }

    for (int i = 0; i < added; ++i) {
      DataComponentRegistry.DataComponentTypeImpl<?> type = (DataComponentRegistry.DataComponentTypeImpl<?>) DataComponentRegistry.getType(ProtocolUtils.readVarInt(buf), version);
      this.components.put(type, type.codec().decode(buf, version));
    }

    for (int i = 0; i < removed; ++i) {
      this.components.put(DataComponentRegistry.getType(ProtocolUtils.readVarInt(buf), version), null);
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public DataComponentMap write(Object bufObj, ProtocolVersion version) {
    ByteBuf buf = (ByteBuf) bufObj;

    if (this.components == null || this.components.isEmpty()) {
      ProtocolUtils.writeVarInt(buf, 0);
      ProtocolUtils.writeVarInt(buf, 0);
    } else {
      int added = 0;
      int removed = 0;
      for (Object component : this.components.values()) {
        if (component == null) {
          ++removed;
        } else {
          ++added;
        }
      }

      ProtocolUtils.writeVarInt(buf, added);
      ProtocolUtils.writeVarInt(buf, removed);

      this.components.forEach((key, value) -> {
        if (value != null) {
          int id = DataComponentRegistry.getId(key, version);
          if (id != Integer.MIN_VALUE) { // allow plugins to send version-specific components, so don't throw exception if no id for this version
            ProtocolUtils.writeVarInt(buf, id);
            ((DataComponentRegistry.DataComponentTypeImpl<Object>) key).codec().encode(buf, version, value);
          }
        }
      });

      this.components.forEach((key, component) -> {
        if (component == null) {
          int id = DataComponentRegistry.getId(key, version);
          if (id != Integer.MIN_VALUE) {
            ProtocolUtils.writeVarInt(buf, id);
          }
        }
      });
    }

    return this;
  }

  @Override
  public String toString() {
    return "SimpleDataComponentMap{"
        + "components=" + this.components
        + "}";
  }

  public static DataComponentMap read(ByteBuf buf, ProtocolVersion version) {
    int added = ProtocolUtils.readVarInt(buf);
    int removed = ProtocolUtils.readVarInt(buf);
    if (added == 0 && removed == 0) {
      return null;
    } else {
      SimpleDataComponentMap map = new SimpleDataComponentMap();
      map.read(buf, version, added, removed);
      return map;
    }
  }
}
