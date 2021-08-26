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

package net.elytrium.limbofilter.stats;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Statistics {

  private final AtomicLong blockedConnections = new AtomicLong(0L);
  private final AtomicInteger connectionsPerSecond = new AtomicInteger();
  private final AtomicInteger pingsPerSecond = new AtomicInteger();

  public void addBlockedConnection() {
    blockedConnections.incrementAndGet();
  }

  public void addConnectionPerSecond() {
    connectionsPerSecond.incrementAndGet();
  }

  public void addPingPerSecond() {
    pingsPerSecond.incrementAndGet();
  }

  public long getBlockedConnections() {
    return blockedConnections.longValue();
  }

  public int getConnectionsPerSecond() {
    return connectionsPerSecond.get();
  }

  public int getPingsPerSecond() {
    return pingsPerSecond.get();
  }

  public int getTotalConnectionsPerSecond() {
    return pingsPerSecond.get() + connectionsPerSecond.get();
  }

  public void startUpdating() {
    new Timer().scheduleAtFixedRate(new TimerTask() {
      int cpsBefore = 0;
      int ppsBefore = 0;

      public void run() {
        int currentCps = connectionsPerSecond.get();
        if (currentCps > 0) {
          connectionsPerSecond.set(connectionsPerSecond.get() - cpsBefore);
          cpsBefore = connectionsPerSecond.get();
        }

        int currentPps = pingsPerSecond.get();
        if (currentPps > 0) {
          pingsPerSecond.set(pingsPerSecond.get() - ppsBefore);
          ppsBefore = pingsPerSecond.get();
        }

      }
    }, 1000, 1000);
  }
}
