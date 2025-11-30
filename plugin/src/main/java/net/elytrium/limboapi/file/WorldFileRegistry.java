/*
 * Copyright (C) 2021 - 2025 Elytrium
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

package net.elytrium.limboapi.file;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import net.elytrium.limboapi.api.world.BuiltInWorldFileType;
import net.elytrium.limboapi.api.world.WorldFile;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.CompoundBinaryTag;

public interface WorldFileRegistry {

  static WorldFile of(BuiltInWorldFileType type, Path file) throws IOException {
    return WorldFileRegistry.of(type, BinaryTagIO.unlimitedReader().read(file, BinaryTagIO.Compression.GZIP));
  }

  static WorldFile of(BuiltInWorldFileType type, InputStream stream) throws IOException {
    return WorldFileRegistry.of(type, BinaryTagIO.unlimitedReader().read(stream, BinaryTagIO.Compression.GZIP));
  }

  static WorldFile of(BuiltInWorldFileType type, CompoundBinaryTag tag) {
    return switch (type) {
      case SCHEMATIC -> new MCEditSchematicFile(tag);
      case WORLDEDIT_SCHEM -> new WorldEditSchemFile(tag);
      case STRUCTURE -> new StructureNbtFile(tag);
    };
  }
}
