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
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.packet.ClientSettings;
import com.velocitypowered.proxy.protocol.packet.PluginMessage;
import com.velocitypowered.proxy.protocol.util.PluginMessageUtil;
import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;
import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.player.LimboPlayer;
import net.elytrium.limboapi.protocol.packet.SetExp;
import net.elytrium.limboapi.server.LimboImpl;
import net.elytrium.limboapi.server.world.chunk.SimpleChunk;
import net.elytrium.limbofilter.FilterPlugin;
import net.elytrium.limbofilter.Settings;
import net.elytrium.limbofilter.cache.CachedPackets;
import net.elytrium.limbofilter.cache.CaptchaHandler;
import net.elytrium.limbofilter.stats.Statistics;
import org.slf4j.Logger;

public class BotFilterSessionHandler extends FallingCheckHandler {

  private static long TOTAL_TICKS;
  private static double CAPTCHA_Y;
  private static long TOTAL_TIME;

  private final ConnectedPlayer player;
  private final FilterPlugin plugin;
  private final Statistics statistics;
  private final Logger logger;
  private final CachedPackets packets;
  private final MinecraftConnection connection;

  private MinecraftPacket fallingCheckPos;
  private MinecraftPacket fallingCheckChunk;
  private MinecraftPacket fallingCheckView;

  private CheckState state = CheckState.valueOf(Settings.IMP.MAIN.CHECK_STATE);
  private LimboPlayer limboPlayer;
  private Limbo server;
  private String captchaAnswer;
  private int attempts = Settings.IMP.MAIN.CAPTCHA_ATTEMPTS;
  private int ignoredTicks = 0;
  private int nonValidPacketsSize = 0;
  private int genericBytes = 0;
  private long joinTime = System.currentTimeMillis();
  private boolean startedListening = false;
  private boolean checkedBySettings = false;
  private boolean checkedByBrand = false;

  public BotFilterSessionHandler(ConnectedPlayer player, FilterPlugin plugin) {
    super(player.getProtocolVersion());

    this.player = player;
    this.plugin = plugin;

    this.statistics = this.plugin.getStatistics();
    this.logger = this.plugin.getLogger();
    this.packets = this.plugin.getPackets();
    this.connection = this.player.getConnection();
  }

  @Override
  public void onGeneric(MinecraftPacket packet) {
    if (packet instanceof PluginMessage) {
      PluginMessage pluginMessage = (PluginMessage) packet;
      int singleLength = pluginMessage.content().readableBytes();
      singleLength += pluginMessage.getChannel().length() * 4;
      this.genericBytes += singleLength;
      if (singleLength > Settings.IMP.MAIN.MAX_SINGLE_GENERIC_PACKET_LENGTH || this.genericBytes > Settings.IMP.MAIN.MAX_MULTI_GENERIC_PACKET_LENGTH) {
        this.connection.closeWith(this.packets.getTooBigPacket());
        this.logger.error("{} sent too big packet", this.player);
        this.statistics.addBlockedConnection();
      }
      if (PluginMessageUtil.isMcBrand(pluginMessage) && !this.checkedByBrand) {
        String brand = PluginMessageUtil.readBrandMessage(pluginMessage.content());
        this.logger.info("{} has client brand {}", this.player, brand);
        if (!Settings.IMP.MAIN.BLOCKED_CLIENT_BRANDS.contains(brand)) {
          this.checkedByBrand = true;
        }
      }
    } else if (packet instanceof ClientSettings) {
      ClientSettings clientSettings = (ClientSettings) packet;
      if (!this.checkedBySettings && Settings.IMP.MAIN.CHECK_CLIENT_SETTINGS) {
        if (packet.toString().contains("null")) {
          this.connection.closeWith(this.packets.getKickClientCheckSettings());
          this.logger.error("{} has null in settings packet", this.player);
          this.statistics.addBlockedConnection();
        } else if (!clientSettings.isChatColors()) {
          this.connection.closeWith(this.packets.getKickClientCheckSettingsChat());
          this.logger.error("{} didn't send isChatColors packet", this.player);
          this.statistics.addBlockedConnection();
        } else if (clientSettings.getSkinParts() == 0) {
          this.connection.closeWith(this.packets.getKickClientCheckSettingsSkin());
          this.logger.error("{} didn't send skin parts packet", this.player);
          this.statistics.addBlockedConnection();
        }
      }

      this.checkedBySettings = true;
    }
  }

  @Override
  public void onChat(String message) {
    if ((this.state == CheckState.CAPTCHA_POSITION || this.state == CheckState.ONLY_CAPTCHA) && message.length() <= 256) {
      if (message.equals(this.captchaAnswer)) {
        this.connection.write(this.packets.getResetSlot());
        this.finishCheck();
      } else if (--this.attempts != 0) {
        this.sendCaptcha();
      } else {
        this.statistics.addBlockedConnection();
        this.connection.closeWith(this.packets.getCaptchaFailed());
      }
    }
  }

  private void finishCheck() {
    if (System.currentTimeMillis() - this.joinTime < TOTAL_TIME && this.state != CheckState.ONLY_CAPTCHA) {
      if (this.state == CheckState.CAPTCHA_POSITION && this.ticks < TOTAL_TICKS) {
        this.state = CheckState.ONLY_POSITION;
      } else {
        if (this.state == CheckState.CAPTCHA_ON_POSITION_FAILED) {
          this.changeStateToCaptcha();
        } else {
          this.statistics.addBlockedConnection();
          this.connection.closeWith(this.packets.getFallingCheckFailed());
        }
      }
      return;
    }

    if ((!this.checkedBySettings) && Settings.IMP.MAIN.CHECK_CLIENT_SETTINGS) {
      this.connection.closeWith(this.packets.getKickClientCheckSettings());
      this.statistics.addBlockedConnection();
    }
    if ((!this.checkedByBrand) && Settings.IMP.MAIN.CHECK_CLIENT_BRAND) {
      this.connection.closeWith(this.packets.getKickClientCheckBrand());
      this.statistics.addBlockedConnection();
    }

    this.state = CheckState.SUCCESSFUL;
    this.plugin.cacheFilterUser(this.player);

    if (this.plugin.checkCpsLimit(Settings.IMP.MAIN.FILTER_AUTO_TOGGLE.ONLINE_MODE_VERIFY)
        || this.plugin.checkCpsLimit(Settings.IMP.MAIN.FILTER_AUTO_TOGGLE.NEED_TO_RECONNECT)) {
      this.connection.closeWith(this.packets.getSuccessfulBotFilterDisconnect());
    } else {
      this.connection.write(this.packets.getSuccessfulBotFilterChat());
      this.limboPlayer.disconnect();
    }
  }

  @Override
  @SuppressWarnings("ConstantConditions")
  public void onMove() {
    if (!this.startedListening && this.state != CheckState.ONLY_CAPTCHA) {
      if (this.x == this.validX && this.z == this.validZ) {
        this.startedListening = true;
      }
      if (this.nonValidPacketsSize > Settings.IMP.MAIN.NON_VALID_POSITION_XZ_ATTEMPTS) {
        this.fallingCheckFailed();
        return;
      }

      this.lastY = this.validY;
      this.nonValidPacketsSize++;
    }
    if (this.startedListening) {
      if (this.lastY == CAPTCHA_Y || this.onGround) {
        return;
      }
      if (this.state == CheckState.ONLY_CAPTCHA) {
        if (this.lastY != this.y && this.waitingTeleportId == -1) {
          this.setCaptchaPosition(true);
        }
        return;
      }
      if (this.lastY - this.y == 0) {
        this.ignoredTicks++;
        return;
      }
      if (this.ticks >= TOTAL_TICKS) {
        if (this.state == CheckState.CAPTCHA_POSITION) {
          this.changeStateToCaptcha();
        } else {
          this.finishCheck();
        }
        return;
      }
      if (Settings.IMP.MAIN.FALLING_CHECK_DEBUG) {
        System.out.println(
            "lastY=" + this.lastY + "; y=" + this.y + "; diff=" + (this.lastY - this.y)
            + "; need=" + getLoadedChunkSpeed(this.ticks) + "; ticks=" + this.ticks
            + "; x=" + this.x + "; z=" + this.z + "; validX=" + this.validX + "; validZ=" + this.validZ
            + "; startedListening=" + this.startedListening + "; state=" + this.state
            + "; onGround=" + this.onGround
        );
      }
      if (this.ignoredTicks > Settings.IMP.MAIN.NON_VALID_POSITION_Y_ATTEMPTS) {
        this.fallingCheckFailed();
        return;
      }
      if ((this.x != this.validX && this.z != this.validZ) || this.checkY()) {
        this.fallingCheckFailed();
        return;
      }
      if (this.state == CheckState.CAPTCHA_ON_POSITION_FAILED || this.state == CheckState.ONLY_POSITION) {
        SetExp expBuf = this.packets.getExperience().get(this.ticks);
        if (expBuf != null) {
          this.connection.write(expBuf);
        }
      }

      this.ticks++;
    }
  }

  @Override
  public void onSpawn(Limbo server, LimboPlayer player) {
    this.server = server;
    this.limboPlayer = player;

    Settings.MAIN.COORDS coords = Settings.IMP.MAIN.COORDS;
    this.fallingCheckPos = ((LimboImpl) this.server).createPlayerPosAndLookPacket(
        this.validX, this.validY, this.validZ, (float) coords.FALLING_CHECK_YAW, (float) coords.FALLING_CHECK_PITCH
    );
    this.fallingCheckChunk = ((LimboImpl) this.server).createChunkDataPacket(new SimpleChunk(this.validX >> 4, this.validZ >> 4), this.validY);
    this.fallingCheckView = ((LimboImpl) this.server).createUpdateViewPosition(this.validX, this.validZ);

    if (this.state == CheckState.ONLY_CAPTCHA) {
      this.sendCaptcha();
    } else if (this.state == CheckState.CAPTCHA_POSITION) {
      this.sendFallingCheckPackets();
      this.sendCaptcha();
    } else if (this.state == CheckState.ONLY_POSITION || this.state == CheckState.CAPTCHA_ON_POSITION_FAILED) {
      this.connection.delayedWrite(this.packets.getCheckingTitle());
      this.connection.delayedWrite(this.packets.getCheckingChat());
      this.sendFallingCheckPackets();
    }

    this.connection.flush();
  }

  private void sendFallingCheckPackets() {
    this.connection.delayedWrite(this.fallingCheckPos);
    if (this.connection.getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_14) >= 0) {
      this.connection.delayedWrite(this.fallingCheckView);
    }

    this.connection.delayedWrite(this.fallingCheckChunk);
  }

  private void sendCaptcha() {
    CaptchaHandler captchaHandler = this.plugin.getCachedCaptcha().randomCaptcha();
    String captchaAnswer = captchaHandler.getAnswer();
    this.setCaptchaAnswer(captchaAnswer);
    Settings.MAIN.STRINGS strings = Settings.IMP.MAIN.STRINGS;
    if (this.attempts == Settings.IMP.MAIN.CAPTCHA_ATTEMPTS) {
      this.connection.delayedWrite(this.packets.createChatPacket(MessageFormat.format(strings.CHECKING_CAPTCHA_CHAT, this.attempts)));
      this.connection.delayedWrite(
          this.packets.createTitlePacket(strings.CHECKING_CAPTCHA_TITLE, MessageFormat.format(strings.CHECKING_CAPTCHA_SUBTITLE, this.attempts))
      );
    } else {
      this.connection.delayedWrite(this.packets.createChatPacket(MessageFormat.format(strings.CHECKING_WRONG_CAPTCHA_CHAT, this.attempts)));
    }
    this.connection.delayedWrite(this.packets.getSetSlot());
    this.connection.delayedWrite(captchaHandler.getMap());
    this.connection.flush();
  }

  private boolean checkY() {
    return (Math.abs(this.lastY - this.y - getLoadedChunkSpeed(this.ticks)) > Settings.IMP.MAIN.MAX_VALID_POSITION_DIFFERENCE);
  }

  private void fallingCheckFailed() {
    if (this.state == CheckState.CAPTCHA_ON_POSITION_FAILED) {
      List<SetExp> expList = this.packets.getExperience();
      this.connection.write(expList.get(expList.size() - 1));
      this.changeStateToCaptcha();
      return;
    }

    this.statistics.addBlockedConnection();
    this.connection.closeWith(this.packets.getFallingCheckFailed());
  }

  @SuppressWarnings("SameParameterValue")
  private void setCaptchaPosition(boolean disableFall) {
    this.server.respawnPlayer(this.player);
    if (disableFall) {
      this.connection.write(this.packets.getNoAbilities());
    }

    this.waitingTeleportId = this.validTeleportId;
  }

  private void changeStateToCaptcha() {
    this.state = CheckState.ONLY_CAPTCHA;
    this.joinTime = System.currentTimeMillis() + TOTAL_TIME;
    this.setCaptchaPosition(true);
    if (this.captchaAnswer == null) {
      this.sendCaptcha();
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

    return this.player.getUsername().equals(((BotFilterSessionHandler) o).player.getUsername());
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.player.getUsername());
  }

  public static void reload() {
    TOTAL_TICKS = Settings.IMP.MAIN.FALLING_CHECK_TICKS;
    TOTAL_TIME = (TOTAL_TICKS * 50) - 100;
    CAPTCHA_Y = Settings.IMP.MAIN.COORDS.CAPTCHA_Y;
  }

  public void setCaptchaAnswer(String captchaAnswer) {
    this.captchaAnswer = captchaAnswer;
  }

  public static long getTotalTicks() {
    return TOTAL_TICKS;
  }

  @SuppressWarnings("unused")
  public enum CheckState {
    ONLY_POSITION,
    ONLY_CAPTCHA,
    CAPTCHA_POSITION,
    CAPTCHA_ON_POSITION_FAILED,
    SUCCESSFUL,
    FAILED
  }
}
