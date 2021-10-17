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

package net.elytrium.limboapi.api.mcprotocollib;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;

@SuppressFBWarnings("MALICIOUS_CODE")
public class NibbleArray3d {

  private final byte[] data;

  public NibbleArray3d(int size) {
    this.data = new byte[size >> 1];
  }

  public NibbleArray3d(int size, int defValue) {
    this.data = new byte[size >> 1];
    this.fill(defValue);
  }

  public NibbleArray3d(byte[] array) {
    this.data = array;
  }

  public byte[] getData() {
    return this.data;
  }

  public int get(int x, int y, int z) {
    int key = y << 8 | z << 4 | x;
    int index = key >> 1;
    int part = key & 1;
    return part == 0 ? this.data[index] & 15 : this.data[index] >> 4 & 15;
  }

  public void set(int x, int y, int z, int val) {
    int key = y << 8 | z << 4 | x;
    this.set(key, val);
  }

  public void set(int key, int val) {
    int index = key >> 1;
    int part = key & 1;
    if (part == 0) {
      this.data[index] = (byte) (this.data[index] & 240 | val & 15);
    } else {
      this.data[index] = (byte) (this.data[index] & 15 | (val & 15) << 4);
    }
  }

  public void fill(int val) {
    for (int index = 0; index < this.data.length << 1; index++) {
      this.set(index, val);
    }
  }

  public NibbleArray3d copy() {
    return new NibbleArray3d(Arrays.copyOf(this.data, this.data.length));
  }
}
