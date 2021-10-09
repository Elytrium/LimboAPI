/*
 * Copyright (C) 2021 Elytrium
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

package net.elytrium.limbofilter.cache;

import net.elytrium.limboapi.protocol.packet.MapDataPacket;

public class CaptchaHandler {

  private final MapDataPacket map;
  private final String answer;

  public CaptchaHandler(MapDataPacket map, String answer) {
    this.map = map;
    this.answer = answer;
  }

  public MapDataPacket getMap() {
    return this.map;
  }

  public String getAnswer() {
    return this.answer;
  }
}
