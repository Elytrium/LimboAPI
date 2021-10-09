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
    this.blockedConnections.incrementAndGet();
  }

  public void addConnectionPerSecond() {
    this.connectionsPerSecond.incrementAndGet();
  }

  public void addPingPerSecond() {
    this.pingsPerSecond.incrementAndGet();
  }

  public long getBlockedConnections() {
    return this.blockedConnections.longValue();
  }

  public int getConnectionsPerSecond() {
    return this.connectionsPerSecond.get();
  }

  public int getPingsPerSecond() {
    return this.pingsPerSecond.get();
  }

  public int getTotalConnectionsPerSecond() {
    return this.pingsPerSecond.get() + this.connectionsPerSecond.get();
  }

  public void startUpdating() {
    new Timer().scheduleAtFixedRate(new TimerTask() {
      int cpsBefore = 0;
      int ppsBefore = 0;

      public void run() {
        int currentCps = Statistics.this.connectionsPerSecond.get();
        if (currentCps > 0) {
          Statistics.this.connectionsPerSecond.set(Statistics.this.connectionsPerSecond.get() - this.cpsBefore);
          this.cpsBefore = Statistics.this.connectionsPerSecond.get();
        }

        int currentPps = Statistics.this.pingsPerSecond.get();
        if (currentPps > 0) {
          Statistics.this.pingsPerSecond.set(Statistics.this.pingsPerSecond.get() - this.ppsBefore);
          this.ppsBefore = Statistics.this.pingsPerSecond.get();
        }
      }
    }, 1000, 1000);
  }
}
