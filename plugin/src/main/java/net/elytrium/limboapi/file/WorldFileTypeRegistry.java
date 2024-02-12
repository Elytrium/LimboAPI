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

package net.elytrium.limboapi.file;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.function.Function;
import net.elytrium.limboapi.api.file.BuiltInWorldFileType;
import net.elytrium.limboapi.api.file.WorldFile;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.CompoundBinaryTag;

public enum WorldFileTypeRegistry {
  SCHEMATIC(BuiltInWorldFileType.SCHEMATIC, MCEditSchematicFile::new),
  WORLDEDIT_SCHEM(BuiltInWorldFileType.WORLDEDIT_SCHEM, WorldEditSchemFile::new),
  STRUCTURE(BuiltInWorldFileType.STRUCTURE, StructureNbtFile::new);

  private static final EnumMap<BuiltInWorldFileType, WorldFileTypeRegistry> API_TYPE_MAP = new EnumMap<>(BuiltInWorldFileType.class);
  private final BuiltInWorldFileType apiType;
  private final Function<CompoundBinaryTag, WorldFile> worldFileFunction;

  static {
    for (WorldFileTypeRegistry pluginType : WorldFileTypeRegistry.values()) {
      API_TYPE_MAP.put(pluginType.apiType, pluginType);
    }
  }

  WorldFileTypeRegistry(BuiltInWorldFileType apiType, Function<CompoundBinaryTag, WorldFile> worldFileFunction) {
    this.apiType = apiType;
    this.worldFileFunction = worldFileFunction;
  }

  public static WorldFileTypeRegistry fromApiType(BuiltInWorldFileType apiType) {
    return API_TYPE_MAP.get(apiType);
  }

  public static WorldFile fromApiType(BuiltInWorldFileType apiType, Path file) throws IOException {
    return fromApiType(apiType).fromNbt(file);
  }

  public static WorldFile fromApiType(BuiltInWorldFileType apiType, InputStream stream) throws IOException {
    return fromApiType(apiType).fromNbt(stream);
  }

  public static WorldFile fromApiType(BuiltInWorldFileType apiType, CompoundBinaryTag tag) {
    return fromApiType(apiType).fromNbt(tag);
  }

  public WorldFile fromNbt(Path file) throws IOException {
    return this.fromNbt(BinaryTagIO.unlimitedReader().read(file, BinaryTagIO.Compression.GZIP));
  }

  public WorldFile fromNbt(InputStream stream) throws IOException {
    return this.fromNbt(BinaryTagIO.unlimitedReader().read(stream, BinaryTagIO.Compression.GZIP));
  }

  public WorldFile fromNbt(CompoundBinaryTag tag) {
    return this.worldFileFunction.apply(tag);
  }
}
