/*
 * Copyright (C) 2021 - 2022 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.anvil;

import java.io.DataInputStream;
import java.io.IOException;
import java.time.Instant;

public class TimestampTable {

  private final Instant[] timestamps = new Instant[1024];

  public TimestampTable(Instant[] timestamps) {
    System.arraycopy(timestamps, 0, this.timestamps, 0, timestamps.length);
  }

  public TimestampTable(DataInputStream stream) throws IOException {
    for (int i = 0; i < this.timestamps.length; ++i) {
      this.timestamps[i] = this.decodeTimestamp(stream);
    }
  }

  private Instant decodeTimestamp(int data) {
    if (data == 0) {
      return null;
    } else {
      return Instant.ofEpochSecond(data);
    }
  }

  private Instant decodeTimestamp(DataInputStream stream) throws IOException {
    return this.decodeTimestamp(stream.readInt());
  }

  public Instant getTimestamp(int id) {
    return this.timestamps[id];
  }

  public Instant getTimestamp(int x, int z) {
    return this.timestamps[(z & 31) * 32 + (x & 31)];
  }

  public Instant[] getTimestamps() {
    return this.timestamps;
  }
}
