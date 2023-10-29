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

package net.elytrium.limboapi.injection.login;

import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.LoginAcknowledged;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.PlatformDependent;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import net.elytrium.limboapi.LimboAPI;

public class LoginTrackHandler implements MinecraftSessionHandler {
  private final CompletableFuture<Object> confirmation = new CompletableFuture<>();
  private final Queue<MinecraftPacket> queuedPackets = PlatformDependent.newMpscQueue();
  private final MinecraftConnection connection;

  public LoginTrackHandler(MinecraftConnection connection) {
    this.connection = connection;
  }

  public void waitForConfirmation(Runnable runnable) {
    this.confirmation.thenRun(runnable).thenRun(this::processQueued);
  }

  @Override
  public void handleGeneric(MinecraftPacket packet) {
    if (this.connection.getState() == StateRegistry.CONFIG) {
      this.queuedPackets.add(packet);
    }
  }

  @Override
  public boolean handle(LoginAcknowledged packet) {
    this.connection.setState(StateRegistry.CONFIG);
    this.confirmation.complete(this);
    return true;
  }

  @Override
  public void handleUnknown(ByteBuf buf) {
    this.connection.close(true);
  }

  public void processQueued() {
    ChannelHandlerContext ctx = this.connection.getChannel().pipeline().context(this.connection);
    MinecraftSessionHandler sessionHandler = this.connection.getActiveSessionHandler();
    if (sessionHandler != null && !sessionHandler.beforeHandle()) {
      for (MinecraftPacket packet : this.queuedPackets) {
        if (!this.connection.isClosed()) {
          try {
            if (!packet.handle(sessionHandler)) {
              sessionHandler.handleGeneric(packet);
            }
          } catch (Throwable throwable) {
            try {
              this.connection.exceptionCaught(ctx, throwable);
            } catch (Throwable t) {
              LimboAPI.getLogger().error("{}: exception handling exception in {}", ctx.channel().remoteAddress(), this, t);
            }
          }
        }

        ReferenceCountUtil.release(packet);
      }
    }

    this.queuedPackets.clear();
  }
}
