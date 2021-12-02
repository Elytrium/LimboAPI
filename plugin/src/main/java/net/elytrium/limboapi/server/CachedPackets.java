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

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.packet.Disconnect;
import java.util.function.Function;
import net.elytrium.limboapi.LimboAPI;
import net.elytrium.limboapi.Settings;
import net.elytrium.limboapi.api.protocol.PreparedPacket;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class CachedPackets {

  private PreparedPacket tooBigPacket;

  public void createPackets() {
    this.tooBigPacket = LimboAPI.getInstance().createPreparedPacket()
        .prepare((Function<ProtocolVersion, Disconnect>) (version) -> this.createDisconnectPacket(Settings.IMP.MAIN.MESSAGES.TOO_BIG_PACKET, version));
  }

  private Disconnect createDisconnectPacket(String message, ProtocolVersion version) {
    return Disconnect.create(
        LegacyComponentSerializer
            .legacyAmpersand()
            .deserialize(message), version);
  }

  public PreparedPacket getTooBigPacket() {
    return this.tooBigPacket;
  }
}
