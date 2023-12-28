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
import com.velocitypowered.proxy.protocol.packet.PluginMessage;
import com.velocitypowered.proxy.protocol.packet.chat.keyed.KeyedPlayerChat;
import com.velocitypowered.proxy.protocol.packet.chat.keyed.KeyedPlayerCommand;
import com.velocitypowered.proxy.protocol.packet.chat.legacy.LegacyChat;
import com.velocitypowered.proxy.protocol.packet.chat.session.SessionPlayerChat;
import com.velocitypowered.proxy.protocol.packet.chat.session.SessionPlayerCommand;
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
import net.elytrium.limboapi.Settings;

public abstract class ConfirmHandler implements MinecraftSessionHandler {

  private static final MethodHandle TEARDOWN_METHOD;

  protected final LimboAPI plugin;
  protected final CompletableFuture<Object> confirmation = new CompletableFuture<>();
  protected final List<MinecraftPacket> queuedPackets = new ArrayList<>();
  protected final MinecraftConnection connection;
  protected ConnectedPlayer player;
  protected int genericBytes;
  protected boolean disableRelease;

  public ConfirmHandler(LimboAPI plugin, MinecraftConnection connection) {
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
      this.disableRelease = true;
      try {
        runnable.run();
      } catch (Throwable throwable) {
        LimboAPI.getLogger().error("Failed to confirm transition for " + this.player, throwable);
      }

      try {
        this.processQueued();
      } catch (Throwable throwable) {
        LimboAPI.getLogger().error("Failed to process confirmation queue for " + this.player, throwable);
      }
      this.disableRelease = false;
    });
  }

  @Override
  public boolean handle(LegacyChat packet) {
    return this.handleChat(packet.getMessage());
  }

  @Override
  public boolean handle(KeyedPlayerChat packet) {
    return this.handleChat(packet.getMessage());
  }

  @Override
  public boolean handle(KeyedPlayerCommand packet) {
    return this.handleChat("/" + packet.getCommand());
  }

  @Override
  public boolean handle(SessionPlayerChat packet) {
    return this.handleChat(packet.getMessage());
  }

  @Override
  public boolean handle(SessionPlayerCommand packet) {
    return this.handleChat("/" + packet.getCommand());
  }

  private boolean handleChat(String message) {
    int messageLength = message.length();
    this.genericBytes += messageLength;
    if (messageLength > Settings.IMP.MAIN.MAX_CHAT_MESSAGE_LENGTH) {
      this.close("chat", messageLength);
    } else if (this.genericBytes > Settings.IMP.MAIN.MAX_MULTI_GENERIC_PACKET_LENGTH) {
      this.close("chat, multi", this.genericBytes);
    }

    return true;
  }

  @Override
  public void handleUnknown(ByteBuf packet) {
    int readableBytes = packet.readableBytes();
    this.genericBytes += readableBytes;
    if (readableBytes > Settings.IMP.MAIN.MAX_UNKNOWN_PACKET_LENGTH) {
      this.close("unknown", readableBytes);
    } else if (this.genericBytes > Settings.IMP.MAIN.MAX_MULTI_GENERIC_PACKET_LENGTH) {
      this.close("unknown, multi", this.genericBytes);
    }
  }

  @Override
  public void handleGeneric(MinecraftPacket packet) {
    if (packet instanceof PluginMessage pluginMessage) {
      int singleLength = pluginMessage.content().readableBytes() + pluginMessage.getChannel().length() * 4;
      this.genericBytes += singleLength;
      if (singleLength > Settings.IMP.MAIN.MAX_SINGLE_GENERIC_PACKET_LENGTH) {
        this.close("generic (PluginMessage packet (custom payload)), single", singleLength);
        return;
      } else if (this.genericBytes > Settings.IMP.MAIN.MAX_MULTI_GENERIC_PACKET_LENGTH) {
        this.close("generic (PluginMessage packet (custom payload)), multi", this.genericBytes);
        return;
      }
    }

    if (this.connection.getState() == StateRegistry.CONFIG) {
      this.queuedPackets.add(ReferenceCountUtil.retain(packet));
    }
  }

  public void close(String type, int length) {
    this.connection.close();

    if (Settings.IMP.MAIN.LOGGING_ENABLED) {
      LimboAPI.getLogger().warn(
          "{} sent too big packet while confirming transition. (type: {}, length: {})", this.player, type, length);
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
      if (!this.disableRelease) {
        this.releaseQueue();
      }
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
