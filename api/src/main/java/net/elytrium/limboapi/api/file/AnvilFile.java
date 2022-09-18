/*
 * Copyright (C) 2021 - 2022 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.file;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.elytrium.limboapi.api.LimboFactory;
import net.elytrium.limboapi.api.anvil.LocationTable;
import net.elytrium.limboapi.api.anvil.TimestampTable;
import net.elytrium.limboapi.api.chunk.VirtualBlock;
import net.elytrium.limboapi.api.chunk.VirtualWorld;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.kyori.adventure.nbt.StringBinaryTag;

public class AnvilFile implements WorldFile, Closeable {

  private final FileChannel fileChannel;
  private final LocationTable locationTable;
  private final TimestampTable timestampTable;
  private int offsetChunkX = 0;
  private int offsetChunkZ = 0;
  private int chunksX = 32;
  private int chunksZ = 32;
  private boolean ignoreChunkPosition = true;
  private boolean ignoreSectionOffset = true;

  public AnvilFile(Path file) throws IOException {
    this(FileChannel.open(file, StandardOpenOption.READ));
  }

  public AnvilFile(FileChannel fileChannel) throws IOException {
    this.fileChannel = fileChannel;
    DataInputStream stream = new DataInputStream(Channels.newInputStream(fileChannel));
    this.locationTable = new LocationTable(stream);
    this.timestampTable = new TimestampTable(stream);
  }

  @Override
  public void close() throws IOException {
    this.fileChannel.close();
  }

  @Override
  public void toWorld(LimboFactory factory, VirtualWorld world, int offsetX, int offsetY, int offsetZ) {
    this.toWorld(factory, world, offsetX, offsetY, offsetZ, 15);
  }

  @Override
  public void toWorld(LimboFactory factory, VirtualWorld world, int offsetX, int offsetY, int offsetZ, int lightLevel) {
    world.fillSkyLight(lightLevel);

    for (int chunkX = this.offsetChunkX; chunkX < this.offsetChunkX + this.chunksX; ++chunkX) {
      for (int chunkZ = this.offsetChunkZ; chunkZ < this.offsetChunkZ + this.chunksZ; ++chunkZ) {
        this.loadChunk(chunkX, chunkZ, factory, world, offsetX, offsetY, offsetZ);
      }
    }
  }

  private void loadChunk(int chunkX, int chunkZ, LimboFactory factory, VirtualWorld world, int offsetX, int offsetY, int offsetZ) {
    LocationTable.Entry location = this.locationTable.getEntry(chunkX, chunkZ);
    if (location == null) {
      return;
    }

    CompoundBinaryTag chunkData;

    try {
      ByteBuffer header = ByteBuffer.allocate(5);

      this.fileChannel.position(location.getByteOffset());
      this.fileChannel.read(header);

      byte compressionType = header.get(4);

      BinaryTagIO.Compression compression;
      switch (compressionType) {
        case 1: {
          compression = BinaryTagIO.Compression.GZIP;
          break;
        }

        case 2: {
          compression = BinaryTagIO.Compression.ZLIB;
          break;
        }

        case 3: {
          compression = BinaryTagIO.Compression.NONE;
          break;
        }

        default: {
          throw new IllegalStateException("Unexpected value: " + compressionType);
        }
      }

      chunkData = BinaryTagIO.unlimitedReader().read(Channels.newInputStream(this.fileChannel), compression);
    } catch (IOException e) {
      return;
    }

    int blockOffsetX = chunkX;
    int blockOffsetZ = chunkZ;
    int sectionOffset = this.ignoreSectionOffset ? 0 : chunkData.getInt("yPos");

    if (!this.ignoreChunkPosition) {
      blockOffsetX += chunkData.getInt("xPos");
      blockOffsetZ += chunkData.getInt("zPos");
    }

    blockOffsetX = blockOffsetX * 16 + offsetX;
    blockOffsetZ = blockOffsetZ * 16 + offsetZ;

    ListBinaryTag sections = chunkData.getList("sections");
    for (BinaryTag section : sections) {
      this.loadChunkSection((CompoundBinaryTag) section, sectionOffset, blockOffsetX, offsetY, blockOffsetZ, factory, world);
    }
  }

  private void loadChunkSection(CompoundBinaryTag sectionCompound, int sectionOffset, int blockOffsetX, int offsetY,
                                int blockOffsetZ, LimboFactory factory, VirtualWorld world) {
    int sectionY = sectionCompound.getByte("Y") - sectionOffset;

    if (sectionY < 0 || sectionY >= 16) {
      return;
    }

    int blockOffsetY = sectionY * 16 + offsetY;

    List<VirtualBlock> palette = new ArrayList<>();
    CompoundBinaryTag blockStates = sectionCompound.getCompound("block_states");

    blockStates.getList("palette").forEach(entry -> {
      CompoundBinaryTag entryCompound = (CompoundBinaryTag) entry;
      String name = entryCompound.getString("Name");
      Map<String, String> properties = new LinkedHashMap<>();
      entryCompound.getCompound("Properties").forEach(e ->
          properties.put(e.getKey(), ((StringBinaryTag) e.getValue()).value()));
      palette.add(factory.createSimpleBlock(name, properties));
    });

    if (palette.size() == 1) {
      VirtualBlock block = palette.get(0);
      if (!block.isAir()) {
        for (int localY = 0; localY < 16; ++localY) {
          for (int localZ = 0; localZ < 16; ++localZ) {
            for (int localX = 0; localX < 16; ++localX) {
              world.setBlock(
                  blockOffsetX + localX,
                  blockOffsetY + localY,
                  blockOffsetZ + localZ,
                  block);
            }
          }
        }
      }
    } else {
      int bitsPerEntry = Math.max(4, Integer.SIZE - Integer.numberOfLeadingZeros(palette.size() - 1));
      int mask = ~(~0 << bitsPerEntry);

      long[] packedData = blockStates.getLongArray("data");
      short[] unpackedData = new short[4096];

      int currentBit;
      int idx = 0;
      for (long packedLong : packedData) {
        currentBit = 0;
        while (currentBit + bitsPerEntry <= 64) {
          if (idx >= unpackedData.length) {
            break;
          }

          unpackedData[idx++] = (short) (packedLong & mask);
          packedLong >>>= bitsPerEntry;
          currentBit += bitsPerEntry;
        }
      }

      for (int localY = 0; localY < 16; ++localY) {
        for (int localZ = 0; localZ < 16; ++localZ) {
          for (int localX = 0; localX < 16; ++localX) {
            world.setBlock(
                blockOffsetX + localX,
                blockOffsetY + localY,
                blockOffsetZ + localZ,
                palette.get(unpackedData[localY << 8 | localZ << 4 | localX]));
          }
        }
      }
    }
  }

  public LocationTable getLocationTable() {
    return this.locationTable;
  }

  public TimestampTable getTimestampTable() {
    return this.timestampTable;
  }

  public int getOffsetChunkX() {
    return this.offsetChunkX;
  }

  public void setOffsetChunkX(int offsetChunkX) {
    this.offsetChunkX = offsetChunkX;
  }

  public int getOffsetChunkZ() {
    return this.offsetChunkZ;
  }

  public void setOffsetChunkZ(int offsetChunkZ) {
    this.offsetChunkZ = offsetChunkZ;
  }

  public int getChunksX() {
    return this.chunksX;
  }

  public void setChunksX(int chunksX) {
    this.chunksX = chunksX;
  }

  public int getChunksZ() {
    return this.chunksZ;
  }

  public void setChunksZ(int chunksZ) {
    this.chunksZ = chunksZ;
  }

  public boolean isIgnoreChunkPosition() {
    return this.ignoreChunkPosition;
  }

  public void setIgnoreChunkPosition(boolean ignoreChunkPosition) {
    this.ignoreChunkPosition = ignoreChunkPosition;
  }

  public boolean isIgnoreSectionOffset() {
    return this.ignoreSectionOffset;
  }

  public void setIgnoreSectionOffset(boolean ignoreSectionOffset) {
    this.ignoreSectionOffset = ignoreSectionOffset;
  }
}
