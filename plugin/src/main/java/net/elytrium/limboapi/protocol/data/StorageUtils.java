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

package net.elytrium.limboapi.protocol.data;

import com.velocitypowered.api.network.ProtocolVersion;

public class StorageUtils {

  public static int fixBitsPerEntry(ProtocolVersion version, int bitsPerEntry) {
    if (bitsPerEntry < 4) {
      return 4;
    } else if (bitsPerEntry < 9) {
      return bitsPerEntry;
    } else if (version.compareTo(ProtocolVersion.MINECRAFT_1_13) < 0) {
      return 13;
    } else if (version.compareTo(ProtocolVersion.MINECRAFT_1_16_4) < 0) {
      return 14;
    } else {
      return 15;
    }
  }
}
