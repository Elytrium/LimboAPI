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

package net.elytrium.limboapi.protocol.packet;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import net.elytrium.limboapi.api.material.VirtualItem;
import net.kyori.adventure.nbt.CompoundBinaryTag;

public class SetSlot implements MinecraftPacket {

  private final int windowId;
  private final int slot;
  private final VirtualItem item;
  private final int count;
  private final int data;
  private final CompoundBinaryTag nbt;

  public SetSlot(int windowId, int slot, VirtualItem item, int count, int data, CompoundBinaryTag nbt) {
    this.windowId = windowId;
    this.slot = slot;
    this.item = item;
    this.count = count;
    this.data = data;
    this.nbt = nbt;
  }

  public SetSlot() {
    throw new IllegalStateException();
  }

  @Override
  public void decode(ByteBuf byteBuf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {

  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    buf.writeByte(this.windowId);

    if (version.compareTo(ProtocolVersion.MINECRAFT_1_17_1) >= 0) {
      ProtocolUtils.writeVarInt(buf, 0);
    }

    buf.writeShort(this.slot);
    int id = this.item.getId(version);
    boolean present = id > 0;

    if (version.compareTo(ProtocolVersion.MINECRAFT_1_13_2) >= 0) {
      buf.writeBoolean(present);
    }

    if (!present && version.compareTo(ProtocolVersion.MINECRAFT_1_13_2) < 0) {
      buf.writeShort(-1);
    }

    if (present) {
      if (version.compareTo(ProtocolVersion.MINECRAFT_1_13_2) < 0) {
        buf.writeShort(id);
      } else {
        ProtocolUtils.writeVarInt(buf, id);
      }
      buf.writeByte(this.count);
      if (version.compareTo(ProtocolVersion.MINECRAFT_1_13) < 0) {
        buf.writeShort(this.data);
      }

      if (this.nbt == null) {
        buf.writeByte(0);
      } else {
        ProtocolUtils.writeCompoundTag(buf, this.nbt);
      }
    }
  }

  @Override
  public boolean handle(MinecraftSessionHandler minecraftSessionHandler) {
    return true;
  }

  @Override
  public String toString() {
    return "SetSlot{"
        + "windowId=" + this.windowId
        + ", slot=" + this.slot
        + ", item=" + this.item
        + ", count=" + this.count
        + ", data=" + this.data
        + ", nbt=" + this.nbt
        + ")";
  }
}
