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

package net.elytrium.limboapi.mapping;

import java.nio.file.Path;

public final class MappingGeneratorMain {

  private MappingGeneratorMain() {
  }

  public static void main(String[] args) throws Exception {
    if (args.length != 5) {
      throw new IllegalArgumentException("Expected arguments: <outputDir> <cacheDir> <seedsDir> <manifestUrl> <cacheValidMillis>");
    }

    MappingGenerator generator = new MappingGenerator(
        Path.of(args[0]),
        Path.of(args[1]),
        Path.of(args[2]),
        args[3],
        Long.parseLong(args[4])
    );
    generator.generateAll();
  }
}

