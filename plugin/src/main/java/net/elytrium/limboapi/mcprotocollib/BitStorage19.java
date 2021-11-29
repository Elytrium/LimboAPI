/*
 * This file is part of MCProtocolLib, licensed under the MIT License (MIT).
 *
 * Copyright (C) 2013-2021 Steveice10
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE
 * OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.elytrium.limboapi.mcprotocollib;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import net.elytrium.limboapi.api.chunk.util.CompactStorage;

@SuppressFBWarnings({"EI_EXPOSE_REP2", "EI_EXPOSE_REP"})
public class BitStorage19 implements CompactStorage {

  private final long[] data;
  private final int bitsPerEntry;
  private final int size;
  private final long maxEntryValue;

  public BitStorage19(int bitsPerEntry, int size) {
    this(bitsPerEntry, new long[roundToNearest(size * bitsPerEntry, 64) / 64]);
  }

  public BitStorage19(int bitsPerEntry, long[] data) {
    if (bitsPerEntry < 4) {
      bitsPerEntry = 4;
    }

    this.bitsPerEntry = bitsPerEntry;
    this.data = data;

    this.size = this.data.length * 64 / this.bitsPerEntry;
    this.maxEntryValue = (1L << this.bitsPerEntry) - 1;
  }

  public int getBitsPerEntry() {
    return this.bitsPerEntry;
  }

  public int getSize() {
    return this.size;
  }

  @Override
  public int get(int index) {
    if (index < 0 || index > this.size - 1) {
      throw new IndexOutOfBoundsException();
    }

    int bitIndex = index * this.bitsPerEntry;
    int startIndex = bitIndex / 64;
    int endIndex = ((index + 1) * this.bitsPerEntry - 1) / 64;
    int startBitSubIndex = bitIndex % 64;
    if (startIndex == endIndex) {
      return (int) (this.data[startIndex] >>> startBitSubIndex & this.maxEntryValue);
    } else {
      int endBitSubIndex = 64 - startBitSubIndex;
      return (int) ((this.data[startIndex] >>> startBitSubIndex | this.data[endIndex] << endBitSubIndex) & this.maxEntryValue);
    }
  }

  @Override
  public void set(int index, int value) {
    if (index < 0 || index > this.size - 1) {
      throw new IndexOutOfBoundsException();
    }

    if (value < 0 || value > this.maxEntryValue) {
      throw new IllegalArgumentException("Value cannot be outside of accepted range.");
    }

    int bitIndex = index * this.bitsPerEntry;
    int startIndex = bitIndex / 64;
    int endIndex = ((index + 1) * this.bitsPerEntry - 1) / 64;
    int startBitSubIndex = bitIndex % 64;
    this.data[startIndex] = this.data[startIndex]
        & ~(this.maxEntryValue << startBitSubIndex) | ((long) value & this.maxEntryValue) << startBitSubIndex;
    if (startIndex != endIndex) {
      int endBitSubIndex = 64 - startBitSubIndex;
      this.data[endIndex] = this.data[endIndex] >>> endBitSubIndex << endBitSubIndex | ((long) value & this.maxEntryValue) >> endBitSubIndex;
    }
  }

  @Override
  public int getDataLength() {
    return ProtocolUtils.varIntBytes(this.data.length) + this.data.length * 8;
  }

  @Override
  public void write(ByteBuf buf, ProtocolVersion version) {
    ProtocolUtils.writeVarInt(buf, this.data.length);
    for (long l : this.data) {
      buf.writeLong(l);
    }
  }

  @Override
  public CompactStorage copy() {
    return new BitStorage19(this.bitsPerEntry, Arrays.copyOf(this.data, this.data.length));
  }

  @SuppressFBWarnings("EI_EXPOSE_REP")
  @Override
  public long[] getData() {
    return this.data;
  }

  @SuppressWarnings("SameParameterValue")
  private static int roundToNearest(int value, int roundTo) {
    if (roundTo == 0) {
      return 0;
    } else if (value == 0) {
      return roundTo;
    } else {
      if (value < 0) {
        roundTo *= -1;
      }

      int remainder = value % roundTo;
      return remainder != 0 ? value + roundTo - remainder : value;
    }
  }
}
