/*
 * This file is part of Velocity-BotFilter, licensed under the AGPLv3 License (AGPLv3).
 *
 * Copyright (C) 2021 Vjatšeslav Maspanov <Leymooo>
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

package net.elytrium.limbofilter.handler;

import com.velocitypowered.api.network.ProtocolVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.concurrent.ThreadLocalRandom;
import net.elytrium.elytraproxy.api.virtual.VirtualServer;
import net.elytrium.elytraproxy.api.virtual.VirtualSessionHandler;
import net.elytrium.elytraproxy.api.virtual.player.VirtualServerPlayer;
import net.elytrium.elytraproxy.config.Settings;

@SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD",
    justification = "ща пока teleportId не юзается но скорее всего потом заюзаем")
public abstract class FallingCheckHandler implements VirtualSessionHandler {
  private static final double[] loadedChunkSpeedCache = new double[Settings.IMP.MAIN.FALLING_CHECK_TICKS];

  static {
    for (int i = 0; i < Settings.IMP.MAIN.FALLING_CHECK_TICKS; ++i) {
      loadedChunkSpeedCache[i] = -((Math.pow(0.98, i) - 1) * 3.92);
    }
  }

  public double x;
  public double y;
  public double z;
  public boolean onGround = false;

  public int teleportId = -1;

  public int waitingTeleportId = 9876;

  public double lastY;
  public int validX;
  public int validY;
  public int validZ;
  public int ticks = 1;

  public final ProtocolVersion version;

  public FallingCheckHandler(ProtocolVersion version) {
    this.version = version;
    validX = ThreadLocalRandom.current().nextInt(16384) + 256;
    validY = ThreadLocalRandom.current().nextInt(256);
    validZ = ThreadLocalRandom.current().nextInt(16384) + 256;

    x = validX;
    y = validY;
    z = validZ;
  }

  @Override
  public String toString() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void onGround(boolean onGround) {
    this.onGround = true;
  }

  @Override
  public void onMove(double x, double y, double z) {
    if (version.isBeforeOrEq(ProtocolVersion.MINECRAFT_1_8)
        && x == validX && y == validY && z == validZ
        && waitingTeleportId == 9876) {
      ticks = 0;
      y = -1;
      waitingTeleportId = -1;
    }
    this.x = x;
    lastY = y;
    this.y = y;
    this.z = z;
    onMove();
  }

  @Override
  public void onTeleport(int teleportId) {
    if (teleportId == waitingTeleportId) {
      ticks = 0;
      y = -1;
      lastY = -1;
      waitingTeleportId = -1;
    }
  }

  public abstract void onMove();

  public static double getLoadedChunkSpeed(int ticks) {
    if (ticks == -1) return 0;
    return loadedChunkSpeedCache[ticks];
  }

  @Override
  public abstract void onSpawn(VirtualServer server, VirtualServerPlayer player);
}
