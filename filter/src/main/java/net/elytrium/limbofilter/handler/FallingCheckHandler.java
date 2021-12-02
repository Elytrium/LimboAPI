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

package net.elytrium.limbofilter.handler;

import com.velocitypowered.api.network.ProtocolVersion;
import java.util.concurrent.ThreadLocalRandom;
import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.LimboSessionHandler;
import net.elytrium.limboapi.api.player.LimboPlayer;
import net.elytrium.limbofilter.Settings;

public abstract class FallingCheckHandler implements LimboSessionHandler {

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
  public int waitingTeleportId = 9876;
  public double lastY;
  public int validX;
  public int validY;
  public int validZ;
  public int validTeleportId;
  public int ticks = 1;

  public final ProtocolVersion version;

  public FallingCheckHandler(ProtocolVersion version) {
    this.version = version;

    ThreadLocalRandom rnd = ThreadLocalRandom.current();
    this.validX = rnd.nextInt(256, 16384);
    // See https://media.discordapp.net/attachments/878241549857738793/915165038464098314/unknown.png
    this.validY = rnd.nextInt(256 + (this.version.compareTo(ProtocolVersion.MINECRAFT_1_8) < 0 ? 100 : 0), 512);
    this.validZ = rnd.nextInt(256, 16384);
    this.validTeleportId = rnd.nextInt(65535);

    this.x = this.validX;
    this.y = this.validY;
    this.z = this.validZ;
  }

  @Override
  public void onGround(boolean onGround) {
    this.onGround = onGround;
  }

  @Override
  public void onMove(double x, double y, double z) {
    if (this.version.compareTo(ProtocolVersion.MINECRAFT_1_8) <= 0
        && x == this.validX && y == this.validY && z == this.validZ && this.waitingTeleportId == this.validTeleportId) {
      this.ticks = 1;
      this.y = -1;
      this.waitingTeleportId = -1;
    }

    this.x = x;
    this.lastY = this.y;
    this.y = y;
    this.z = z;
    this.onMove();
  }

  @Override
  public abstract void onSpawn(Limbo server, LimboPlayer player);

  public abstract void onMove();

  @Override
  public void onTeleport(int teleportId) {
    if (teleportId == this.waitingTeleportId) {
      this.ticks = 1;
      this.y = -1;
      this.lastY = -1;
      this.waitingTeleportId = -1;
    }
  }

  public static double getLoadedChunkSpeed(int ticks) {
    if (ticks == -1) {
      return 0;
    }

    return loadedChunkSpeedCache[ticks];
  }
}
