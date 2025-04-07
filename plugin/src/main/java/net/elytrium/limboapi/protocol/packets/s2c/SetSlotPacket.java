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

package net.elytrium.limboapi.protocol.packets.s2c;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import net.elytrium.limboapi.api.material.VirtualItem;
import net.elytrium.limboapi.api.protocol.item.ItemComponentMap;
import net.elytrium.limboapi.utils.ProtocolTools;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.checkerframework.checker.nullness.qual.Nullable;

public class SetSlotPacket implements MinecraftPacket {

  private final int windowID;
  private final int slot;
  private final VirtualItem item;
  private final int count;
  private final int data;
  @Nullable
  private final CompoundBinaryTag nbt;
  @Nullable
  private final ItemComponentMap map;

  public SetSlotPacket(int windowID, int slot, VirtualItem item, int count, int data,
      @Nullable CompoundBinaryTag nbt, @Nullable ItemComponentMap map) {
    this.windowID = windowID;
    this.slot = slot;
    this.item = item;
    this.count = count;
    this.data = data;
    this.nbt = nbt;
    this.map = map;
  }

  public SetSlotPacket() {
    throw new IllegalStateException();
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    throw new IllegalStateException();
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_20_5) >= 0) {
      this.encodeModern(buf, direction, protocolVersion);
    } else {
      this.encodeLegacy(buf, direction, protocolVersion);
    }
  }

  public void encodeModern(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    ProtocolTools.writeContainerId(buf, protocolVersion, this.windowID);
    ProtocolUtils.writeVarInt(buf, 0);
    buf.writeShort(this.slot);

    int id = this.item.getID(protocolVersion);
    if (id == 0) {
      ProtocolUtils.writeVarInt(buf, 0);
    } else {
      ProtocolUtils.writeVarInt(buf, this.count);
      ProtocolUtils.writeVarInt(buf, id);

      if (this.map != null) {
        this.map.write(protocolVersion, buf);
      } else {
        ProtocolUtils.writeVarInt(buf, 0);
        ProtocolUtils.writeVarInt(buf, 0);
      }
    }
  }

  public void encodeLegacy(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    buf.writeByte(this.windowID);

    if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_17_1) >= 0) {
      ProtocolUtils.writeVarInt(buf, 0); // State ID.
    }

    buf.writeShort(this.slot);
    int id = this.item.getID(protocolVersion);
    boolean present = id > 0;

    if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_13_2) >= 0) {
      buf.writeBoolean(present);
    }

    if (!present && protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_13_2) < 0) {
      buf.writeShort(-1);
    }

    if (present) {
      if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_13_2) < 0) {
        buf.writeShort(id);
      } else {
        ProtocolUtils.writeVarInt(buf, id);
      }
      buf.writeByte(this.count);
      if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_13) < 0) {
        buf.writeShort(this.data);
      }

      if (this.nbt == null) {
        if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_8) < 0) {
          buf.writeShort(-1);
        } else {
          buf.writeByte(0);
        }
      } else {
        ProtocolUtils.writeBinaryTag(buf, protocolVersion, this.nbt);
      }
    }
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return true;
  }

  @Override
  public String toString() {
    return "SetSlot{"
        + "windowID=" + this.windowID
        + ", slot=" + this.slot
        + ", count=" + this.count
        + ", data=" + this.data
        + ", nbt=" + this.nbt
        + ")";
  }
}
