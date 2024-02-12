/*
 * Copyright (C) 2021 - 2024 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.protocol.packets.data;

/**
 * For MapData packet.
 */
public class MapData {

  public static final int MAP_DIM_SIZE = 128;
  public static final int MAP_SIZE = MAP_DIM_SIZE * MAP_DIM_SIZE; // 128² == 16384

  private final int columns;
  private final int rows;
  private final int posX;
  private final int posY;
  private final byte[] data;

  public MapData(byte[] data) {
    this(0, data);
  }

  public MapData(int posX, byte[] data) {
    this(MAP_DIM_SIZE, MAP_DIM_SIZE, posX, 0, data);
  }

  public MapData(int columns, int rows, int posX, int posY, byte[] data) {
    this.columns = columns;
    this.rows = rows;
    this.posX = posX;
    this.posY = posY;
    this.data = data;
  }

  public int getColumns() {
    return this.columns;
  }

  public int getRows() {
    return this.rows;
  }

  public int getX() {
    return this.posX;
  }

  public int getY() {
    return this.posY;
  }

  public byte[] getData() {
    return this.data;
  }

  @Override
  public String toString() {
    return "MapData{"
        + "columns=" + this.columns
        + ", rows=" + this.rows
        + ", posX=" + this.posX
        + ", posY=" + this.posY
        + "}";
  }
}
