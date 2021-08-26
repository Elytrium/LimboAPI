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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.elytrium.limboapi.protocol.packet.MapDataPacket;

/**
 * @author Leymooo
 * @author hevav
 */
public class CachedCaptcha {

  private static final List<CaptchaHandler> captchas = new ArrayList<>();
  private static final AtomicInteger counterAtomic = new AtomicInteger(0);

  public static void createCaptchaPacket(MapDataPacket map, String answer) {
    captchas.add(new CaptchaHandler(map, answer));
  }

  public static CaptchaHandler randomCaptcha() {
    int counter = counterAtomic.incrementAndGet();
    if (counter >= captchas.size()) {
      counter = 0;
      counterAtomic.set(0);
    }
    return captchas.get(counter);
  }
}
