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

package net.elytrium.limboapi.injection.login.confirmation;

import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.LoginAcknowledgedPacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ReferenceCountUtil;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.elytrium.commons.utils.reflection.ReflectionException;
import net.elytrium.limboapi.LimboAPI;

public class LoginConfirmHandler implements MinecraftSessionHandler {

  private static final MethodHandle TEARDOWN_METHOD;

  private final LimboAPI plugin;
  private final CompletableFuture<Object> confirmation = new CompletableFuture<>();
  private final List<MinecraftPacket> queuedPackets = new ArrayList<>();
  private final MinecraftConnection connection;
  private ConnectedPlayer player;

  public LoginConfirmHandler(LimboAPI plugin, MinecraftConnection connection) {
    this.plugin = plugin;
    this.connection = connection;
  }

  public void setPlayer(ConnectedPlayer player) {
    this.player = player;
  }

  public boolean isDone() {
    return this.confirmation.isDone();
  }

  public CompletableFuture<Void> thenRun(Runnable runnable) {
    return this.confirmation.thenRun(runnable);
  }

  public void waitForConfirmation(Runnable runnable) {
    this.thenRun(() -> {
      try {
        runnable.run();
      } catch (Throwable throwable) {
        LimboAPI.getLogger().error("Failed to confirm transition for " + this.player, throwable);
      }

      try {
        ChannelHandlerContext ctx = this.connection.getChannel().pipeline().context(this.connection);
        for (MinecraftPacket packet : this.queuedPackets) {
          try {
            this.connection.channelRead(ctx, packet);
          } catch (Throwable throwable) {
            LimboAPI.getLogger().error("{}: exception handling exception in {}", ctx.channel().remoteAddress(),
                this.connection.getActiveSessionHandler(), throwable);
          }
        }

        this.queuedPackets.clear();
      } catch (Throwable throwable) {
        LimboAPI.getLogger().error("Failed to process packet queue for " + this.player, throwable);
      }
    });
  }

  @Override
  public boolean handle(LoginAcknowledgedPacket packet) {
    this.plugin.setState(this.connection, StateRegistry.CONFIG);
    this.confirmation.complete(this);
    return true;
  }

  @Override
  public void handleGeneric(MinecraftPacket packet) {
    // As Velocity/LimboAPI can easly skip packets due to random delays, packets should be queued
    if (this.connection.getState() == StateRegistry.CONFIG) {
      this.queuedPackets.add(ReferenceCountUtil.retain(packet));
    }
  }

  @Override
  public void handleUnknown(ByteBuf buf) {
    this.connection.close(true);
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
      for (MinecraftPacket packet : this.queuedPackets) {
        ReferenceCountUtil.release(packet);
      }
    }
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
