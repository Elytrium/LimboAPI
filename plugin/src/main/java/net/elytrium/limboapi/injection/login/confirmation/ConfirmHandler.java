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

package net.elytrium.limboapi.injection.login.confirmation;

import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.StateRegistry;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.PlatformDependent;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import net.elytrium.commons.utils.reflection.ReflectionException;
import net.elytrium.limboapi.LimboAPI;

public abstract class ConfirmHandler implements MinecraftSessionHandler {

  private static final MethodHandle TEARDOWN_METHOD;

  protected final CompletableFuture<Object> confirmation = new CompletableFuture<>();
  protected final Queue<MinecraftPacket> queuedPackets = PlatformDependent.newMpscQueue();
  protected final MinecraftConnection connection;
  protected ConnectedPlayer player;

  public ConfirmHandler(MinecraftConnection connection) {
    this.connection = connection;
  }

  public void setPlayer(ConnectedPlayer player) {
    this.player = player;
  }

  public void waitForConfirmation(Runnable runnable) {
    this.confirmation.thenRun(runnable).thenRun(this::processQueued);
  }

  @Override
  public void handleGeneric(MinecraftPacket packet) {
    if (this.connection.getState() == StateRegistry.CONFIG) {
      this.queuedPackets.add(ReferenceCountUtil.retain(packet));
    }
  }

  @Override
  public void disconnected() {
    try {
      if (this.player != null) {
        try {
          TEARDOWN_METHOD.invokeExact(this.player);
        } catch (Throwable e) {
          throw new ReflectionException(e);
        }
      }
    } finally {
      this.releaseQueue();
    }
  }

  public void processQueued() {
    try {
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
        }
      }
    } finally {
      this.releaseQueue();
    }
  }

  public void releaseQueue() {
    for (MinecraftPacket packet : this.queuedPackets) {
      ReferenceCountUtil.release(packet);
    }
    this.queuedPackets.clear();
  }

  static {
    try {
      TEARDOWN_METHOD = MethodHandles.privateLookupIn(ConnectedPlayer.class, MethodHandles.lookup())
          .findVirtual(ConnectedPlayer.class, "teardown", MethodType.methodType(void.class));
    } catch (NoSuchMethodException | IllegalAccessException e) {
      throw new ReflectionException(e);
    }
  }
}
