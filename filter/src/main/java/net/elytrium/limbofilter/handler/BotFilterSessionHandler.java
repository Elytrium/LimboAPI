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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Objects;
import java.util.logging.Logger;
import lombok.Getter;
import lombok.Setter;
import net.elytrium.limbofilter.FilterPlugin;
import net.elytrium.limbofilter.cache.CachedPackets;
import net.elytrium.limbofilter.cache.CaptchaHandler;
import net.elytrium.limbofilter.config.Settings;
import net.elytrium.limbofilter.stats.Statistics;

@Getter
public class BotFilterSessionHandler extends FallingCheckHandler {

  public static final long TOTAL_TICKS = Settings.IMP.MAIN.FALLING_CHECK_TICKS;
  private static final long TOTAL_TIME = (TOTAL_TICKS * 50) - 100;

  private final Statistics statistics;
  private final FilterPlugin plugin;
  private final ConnectedPlayer player;
  private final Logger logger;
  private final CachedPackets packets;
  private final MinecraftConnection connection;
  private final MinecraftPacket fallingCheckPos;
  private final MinecraftPacket fallingCheckChunk;
  private final MinecraftPacket fallingCheckView;
  private final double captchaY = Settings.IMP.MAIN.CAPTCHA_COORDS.Y;
  @Setter
  private String captchaAnswer;
  private LimboPlayer limboPlayer;
  private int ignoredTicks = 0;
  private long joinTime = System.currentTimeMillis();
  private int attempts = Settings.IMP.MAIN.CAPTCHA_ATTEMPTS;
  private boolean startedListening = false;
  private int nonValidPacketsSize = 0;
  @Getter
  private CheckState state = CheckState.valueOf(Settings.IMP.MAIN.CHECK_STATE);
  private Limbo server;

  public BotFilterSessionHandler(ConnectedPlayer player, FilterPlugin plugin) {
    super(player.getProtocolVersion());

    this.plugin = plugin;
    this.player = player;
    this.statistics = plugin.getStatistics();
    this.logger = plugin.getLogger();
    this.packets = plugin.getPackets();
    this.connection = player.getConnection();

    Settings.MAIN.CAPTCHA_COORDS captchaCoords = Settings.IMP.MAIN.CAPTCHA_COORDS;
    this.fallingCheckPos = packets.createPlayerPosAndLookPacket(
        validX, validY, validZ, (float) captchaCoords.YAW, (float) captchaCoords.PITCH);
    this.fallingCheckChunk = packets.createChunkDataPacket(
        new SimpleChunk(validX >> 4, validZ >> 4), validY);
    this.fallingCheckView = packets.createUpdateViewPosition(validX, validZ);
  }

  @Override
  public void onChat(String message) {
    if ((state == CheckState.CAPTCHA_POSITION || state == CheckState.ONLY_CAPTCHA) && message.length() <= 256) {
      if (message.equals(captchaAnswer)) {
        connection.write(packets.getResetSlot());
        finishCheck();
      } else if (--attempts != 0) {
        sendCaptcha();
      } else {
        statistics.addBlockedBots();
        connection.closeWith(packets.getCaptchaFailed());
      }
    }
  }

  @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
  private void finishCheck() {
    if (System.currentTimeMillis() - joinTime < TOTAL_TIME && state != CheckState.ONLY_CAPTCHA) {
      if (state == CheckState.CAPTCHA_POSITION && ticks < TOTAL_TICKS) {
        state = CheckState.ONLY_POSITION;
      } else {
        if (state == CheckState.CAPTCHA_ON_POSITION_FAILED) {
          changeStateToCaptcha();
        } else {
          statistics.addBlockedBots();
          connection.closeWith(packets.getFallingCheckFailed());
        }
      }
      return;
    }
    state = CheckState.SUCCESSFULLY;
    elytraProxy.cacheSucceedUser(player);

    if (Settings.IMP.MAIN.ONLINE_MODE_VERIFY && !Settings.IMP.MAIN.NEED_TO_RECONNECT) {
      connection.write(packets.getSuccessfulBotFilterChat());
      virtualServerPlayer.disconnect();
    } else {
      connection.closeWith(packets.getSuccessfulBotFilterDisconnect());
    }
  }

  @Override
  public void onMove() {
    if (!startedListening && state != CheckState.ONLY_CAPTCHA) {
      if (x == validX && z == validZ) {
        startedListening = true;
        connection.write(packets.getAntiBotTitle());
      }
      if (nonValidPacketsSize > Settings.IMP.MAIN.NON_VALID_POSITION_XZ_ATTEMPTS) {
        fallingCheckFailed();
        return;
      }
      lastY = validY;
      nonValidPacketsSize++;
    }
    if (startedListening) {
      if (lastY == captchaY || onGround) {
        return;
      }
      if (state == CheckState.ONLY_CAPTCHA) {
        if (lastY != y && waitingTeleportId == -1) {
          setCaptchaPosition(true);
        }
        return;
      }
      if (lastY - y == 0) {
        ignoredTicks++;
        return;
      }
      if (ticks >= TOTAL_TICKS) {
        if (state == CheckState.CAPTCHA_POSITION) {
          changeStateToCaptcha();
        } else {
          finishCheck();
        }
        return;
      }
      // System.out.println("lastY=" + lastY + "; y=" + y + "; diff=" + (lastY - y) + ";" +
      //    " need=" + getLoadedChunkSpeed(ticks) + "; ticks=" + ticks +
      //    "; x=" + x + "; z=" + z + "; vx=" + validX + "; vz=" + validZ);
      if (ignoredTicks > Settings.IMP.MAIN.NON_VALID_POSITION_Y_ATTEMPTS) {
        fallingCheckFailed();
        return;
      }
      if ((x != validX && z != validZ) || checkY()) {
        fallingCheckFailed();
        return;
      }
      if (state == CheckState.CAPTCHA_POSITION && waitingTeleportId == -1) {
        setCaptchaPosition(false);
      }
      if ((state == CheckState.CAPTCHA_ON_POSITION_FAILED || state == CheckState.ONLY_POSITION)) {
        SetExp expBuf = packets.getExperience().get(ticks);
        if (expBuf != null) {
          connection.write(expBuf);
        }
      }
      ticks++;
    }
  }

  @Override
  public void onSpawn(VirtualServer server, VirtualServerPlayer player) {
    this.server = server;
    this.virtualServerPlayer = player;
    if (state == CheckState.ONLY_CAPTCHA) {
      sendCaptcha();
    } else if (state == CheckState.CAPTCHA_POSITION) {
      sendCaptcha();
      sendFallingCheckPackets();
    } else if (state == CheckState.ONLY_POSITION
        || state == CheckState.CAPTCHA_ON_POSITION_FAILED) {
      sendFallingCheckPackets();
    }
    connection.flush();
  }

  private void sendFallingCheckPackets() {
    connection.delayedWrite(fallingCheckPos);
    if (connection.getProtocolVersion().isAfterOrEq(ProtocolVersion.MINECRAFT_1_14)) {
      connection.delayedWrite(fallingCheckView);
    }
    connection.delayedWrite(fallingCheckChunk);
  }

  private void sendCaptcha() {
    CaptchaHandler captchaHandler = CachedCaptcha.randomCaptcha();
    String captchaAnswer = captchaHandler.getAnswer();
    setCaptchaAnswer(captchaAnswer);
    connection.delayedWrite(packets.getSetSlot());
    connection.delayedWrite(captchaHandler.getMap());
    connection.delayedWrite(packets.getCheckingCaptchaChat());
  }

  private boolean checkY() {
    double speed = getLoadedChunkSpeed(ticks);
    return (Math.abs(lastY - y - speed) > Settings.IMP.MAIN.MAX_VALID_POSITION_DIFFERENCE);
  }

  private void fallingCheckFailed() {
    if (state == CheckState.CAPTCHA_ON_POSITION_FAILED) {
      changeStateToCaptcha();
      return;
    }
    statistics.addBlockedBots();
    connection.closeWith(packets.getFallingCheckFailed());
  }

  private void setCaptchaPosition(boolean disableFall) {
    server.respawnPlayer(player);
    if (disableFall) {
      connection.write(packets.getNoAbilities());
    }
    waitingTeleportId = 9876;
  }

  private void changeStateToCaptcha() {
    state = CheckState.ONLY_CAPTCHA;
    joinTime = System.currentTimeMillis() + 3500;
    setCaptchaPosition(true);
    if (captchaAnswer == null) {
      sendCaptcha();
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BotFilterSessionHandler that = (BotFilterSessionHandler) o;
    return player.getUsername().equals(that.player.getUsername());
  }

  @Override
  public int hashCode() {
    return Objects.hash(player.getUsername());
  }

  public enum CheckState {
    ONLY_POSITION,
    ONLY_CAPTCHA,
    CAPTCHA_POSITION,
    CAPTCHA_ON_POSITION_FAILED,
    SUCCESSFULLY,
    FAILED
  }
}
