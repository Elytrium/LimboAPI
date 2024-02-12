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

package net.elytrium.limboapi.injection.dummy;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelId;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelProgressivePromise;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoop;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import java.net.SocketAddress;
import org.checkerframework.checker.nullness.qual.NonNull;

public class ClosedChannel implements Channel {

  private final EventLoop eventLoop;

  public ClosedChannel(EventLoop eventLoop) {
    this.eventLoop = eventLoop;
  }

  @Override
  public ChannelId id() {
    return null;
  }

  @Override
  public EventLoop eventLoop() {
    return this.eventLoop;
  }

  @Override
  public Channel parent() {
    return null;
  }

  @Override
  public ChannelConfig config() {
    return null;
  }

  @Override
  public boolean isOpen() {
    return false;
  }

  @Override
  public boolean isRegistered() {
    return false;
  }

  @Override
  public boolean isActive() {
    return false;
  }

  @Override
  public ChannelMetadata metadata() {
    return null;
  }

  @Override
  public SocketAddress localAddress() {
    return null;
  }

  @Override
  public SocketAddress remoteAddress() {
    return null;
  }

  @Override
  public ChannelFuture closeFuture() {
    return null;
  }

  @Override
  public boolean isWritable() {
    return false;
  }

  @Override
  public long bytesBeforeUnwritable() {
    return 0;
  }

  @Override
  public long bytesBeforeWritable() {
    return 0;
  }

  @Override
  public Unsafe unsafe() {
    return null;
  }

  @Override
  public ChannelPipeline pipeline() {
    return null;
  }

  @Override
  public ByteBufAllocator alloc() {
    return null;
  }

  @Override
  public ChannelFuture bind(SocketAddress localAddress) {
    return null;
  }

  @Override
  public ChannelFuture bind(SocketAddress localAddress, ChannelPromise promise) {
    return null;
  }

  @Override
  public ChannelFuture connect(SocketAddress remoteAddress) {
    return null;
  }

  @Override
  public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress) {
    return null;
  }

  @Override
  public ChannelFuture connect(SocketAddress remoteAddress, ChannelPromise promise) {
    return null;
  }

  @Override
  public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) {
    return null;
  }

  @Override
  public ChannelFuture disconnect() {
    return null;
  }

  @Override
  public ChannelFuture disconnect(ChannelPromise promise) {
    return null;
  }

  @Override
  public ChannelFuture close() {
    return null;
  }

  @Override
  public ChannelFuture close(ChannelPromise promise) {
    return null;
  }

  @Override
  public ChannelFuture deregister() {
    return null;
  }

  @Override
  public ChannelFuture deregister(ChannelPromise promise) {
    return null;
  }

  @Override
  public Channel read() {
    return null;
  }

  @Override
  public ChannelFuture write(Object msg) {
    return null;
  }

  @Override
  public ChannelFuture write(Object msg, ChannelPromise promise) {
    return null;
  }

  @Override
  public Channel flush() {
    return null;
  }

  @Override
  public ChannelFuture writeAndFlush(Object msg, ChannelPromise promise) {
    return null;
  }

  @Override
  public ChannelFuture writeAndFlush(Object msg) {
    return null;
  }

  @Override
  public ChannelPromise newPromise() {
    return null;
  }

  @Override
  public ChannelProgressivePromise newProgressivePromise() {
    return null;
  }

  @Override
  public ChannelFuture newSucceededFuture() {
    return null;
  }

  @Override
  public ChannelFuture newFailedFuture(Throwable cause) {
    return null;
  }

  @Override
  public ChannelPromise voidPromise() {
    return null;
  }

  @Override
  public <T> Attribute<T> attr(AttributeKey<T> key) {
    return null;
  }

  @Override
  public <T> boolean hasAttr(AttributeKey<T> key) {
    return false;
  }

  @Override
  public int compareTo(@NonNull Channel o) {
    return 0;
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof ClosedChannel && this.compareTo((Channel) o) == 0;
  }

  @Override
  public int hashCode() {
    return 0;
  }
}
