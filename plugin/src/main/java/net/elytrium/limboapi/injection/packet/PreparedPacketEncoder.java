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

package net.elytrium.limboapi.injection.packet;

import com.velocitypowered.api.network.ProtocolVersion;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import java.util.List;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class PreparedPacketEncoder extends MessageToMessageEncoder<PreparedPacketImpl> {

  private final ProtocolVersion protocolVersion;

  @Override
  protected void encode(ChannelHandlerContext ctx, PreparedPacketImpl msg, List<Object> out) throws Exception {
    if (msg.hasPacketsFor(this.protocolVersion)) {
      out.addAll(msg.getPackets(this.protocolVersion));
    }
  }
}
