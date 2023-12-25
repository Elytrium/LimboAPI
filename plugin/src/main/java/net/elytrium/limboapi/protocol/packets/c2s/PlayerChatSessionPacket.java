/*
 * Copyright (C) 2021 - 2023 Elytrium
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

package net.elytrium.limboapi.protocol.packets.c2s;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.crypto.IdentifiedKey;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.client.ClientPlaySessionHandler;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.UUID;
import net.elytrium.commons.utils.reflection.ReflectionException;

@SuppressWarnings("unused")
public class PlayerChatSessionPacket implements MinecraftPacket {

  public static final MethodHandle PLAYER_FIELD;

  private UUID holderId;
  private IdentifiedKey playerKey;

  @Override
  public void decode(ByteBuf byteBuf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    this.holderId = ProtocolUtils.readUuid(byteBuf);
    this.playerKey = ProtocolUtils.readPlayerKey(protocolVersion, byteBuf);
  }

  @Override
  public void encode(ByteBuf byteBuf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    ProtocolUtils.writeUuid(byteBuf, this.holderId);
    ProtocolUtils.writePlayerKey(byteBuf, this.playerKey);
  }

  @Override
  public boolean handle(MinecraftSessionHandler minecraftSessionHandler) {
    // LimboAPI hook - discard if there is no identified key or unmatched UUID
    if (minecraftSessionHandler instanceof ClientPlaySessionHandler playSessionHandler) {
      try {
        ConnectedPlayer player = (ConnectedPlayer) PLAYER_FIELD.invokeExact(playSessionHandler);
        return player.getIdentifiedKey() == null || player.getUniqueId() != this.holderId;
      } catch (Throwable e) {
        throw new ReflectionException(e);
      }
    }

    return false;
  }

  public UUID getHolderId() {
    return this.holderId;
  }

  public void setHolderId(UUID holderId) {
    this.holderId = holderId;
  }

  public IdentifiedKey getPlayerKey() {
    return this.playerKey;
  }

  public void setPlayerKey(IdentifiedKey playerKey) {
    this.playerKey = playerKey;
  }

  static {
    try {
      PLAYER_FIELD = MethodHandles.privateLookupIn(ClientPlaySessionHandler.class, MethodHandles.lookup())
          .findGetter(ClientPlaySessionHandler.class, "player", ConnectedPlayer.class);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new ReflectionException(e);
    }
  }

}
