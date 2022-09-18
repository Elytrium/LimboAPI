/*
 * Copyright (C) 2021 - 2022 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.anvil;

import java.io.DataInputStream;
import java.io.IOException;

public class LocationTable {

  public static class Entry {
    private final int offset;
    private final byte sectorCount;

    public Entry(int offset, byte sectorCount) {
      this.offset = offset;
      this.sectorCount = sectorCount;
    }

    public int getSectorOffset() {
      return this.offset;
    }

    public byte getSectorCount() {
      return this.sectorCount;
    }

    public long getByteOffset() {
      return this.offset * 4096L;
    }

    public int getSizeInBytes() {
      return this.sectorCount * 4096;
    }

    @Override
    public String toString() {
      return "Entry{"
          + "offset=" + this.offset
          + ", sectorCount=" + this.sectorCount
          + '}';
    }
  }

  private final Entry[] entries = new Entry[1024];

  public LocationTable(Entry[] entries) {
    System.arraycopy(entries, 0, this.entries, 0, entries.length);
  }

  public LocationTable(DataInputStream stream) throws IOException {
    for (int i = 0; i < this.entries.length; ++i) {
      this.entries[i] = this.decodeEntry(stream);
    }
  }

  private Entry decodeEntry(int data) {
    if (data == 0) {
      return null;
    } else {
      return new Entry(data >>> 8 & 0xFFFFFF, (byte) (data & 0xFF));
    }
  }

  private Entry decodeEntry(DataInputStream stream) throws IOException {
    return this.decodeEntry(stream.readInt());
  }

  public Entry getEntry(int id) {
    return this.entries[id];
  }

  public Entry getEntry(int x, int z) {
    return this.entries[(z & 31) * 32 + (x & 31)];
  }

  public Entry[] getEntries() {
    return this.entries;
  }
}
