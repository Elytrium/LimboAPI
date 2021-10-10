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

package net.elytrium.limboauth.handler;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.j256.ormlite.dao.Dao;
import com.velocitypowered.api.proxy.Player;
import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.LimboSessionHandler;
import net.elytrium.limboapi.api.player.LimboPlayer;
import net.elytrium.limboauth.AuthPlugin;
import net.elytrium.limboauth.Settings;
import net.elytrium.limboauth.migration.MigrationHash;
import net.elytrium.limboauth.model.RegisteredPlayer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class AuthSessionHandler implements LimboSessionHandler {

  private static final TimeProvider timeProvider = new SystemTimeProvider();
  private static final CodeGenerator codeGenerator = new DefaultCodeGenerator();
  private static final CodeVerifier verifier = new DefaultCodeVerifier(codeGenerator, timeProvider);

  private final Dao<RegisteredPlayer, String> playerDao;
  private final Player proxyPlayer;
  private final RegisteredPlayer playerInfo;

  private LimboPlayer player;
  private String ip;
  private int attempts = Settings.IMP.MAIN.LOGIN_ATTEMPTS;
  private boolean totp = false;

  public AuthSessionHandler(Dao<RegisteredPlayer, String> playerDao, Player proxyPlayer, String lowercaseNickname) {
    this.playerDao = playerDao;
    this.proxyPlayer = proxyPlayer;
    this.playerInfo = this.fetchInfo(lowercaseNickname);
  }

  @Override
  public void onSpawn(Limbo server, LimboPlayer player) {
    this.player = player;
    this.ip = this.proxyPlayer.getRemoteAddress().getAddress().getHostAddress();

    if (this.playerInfo == null) {
      this.checkIp();
    } else {
      this.checkCase();
    }

    this.sendMessage();
  }

  @Override
  public void onChat(String message) {
    String[] args = message.split(" ");
    if (args.length != 0) {
      switch (args[0]) {
        case "/reg":
        case "/register":
        case "/r": {
          if (args.length >= 3 && !this.totp && this.playerInfo == null && args[1].equals(args[2])) {
            this.register(args[1]);
            this.finish();
          } else {
            this.sendMessage();
          }
          break;
        }
        case "/log":
        case "/login":
        case "/l": {
          if (args.length >= 2 && !this.totp && this.playerInfo != null) {
            if (this.checkPassword(args[1])) {
              this.finishOrTotp();
            } else if (this.attempts-- != 0) {
              this.proxyPlayer.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(Settings.IMP.MAIN.STRINGS.PASSWORD_WRONG));
            } else {
              this.proxyPlayer.disconnect(LegacyComponentSerializer.legacyAmpersand().deserialize(Settings.IMP.MAIN.STRINGS.PASSWORD_WRONG));
            }
          } else {
            this.sendMessage();
          }
          break;
        }
        case "/2fa": {
          if (args.length >= 2 && this.totp) {
            if (verifier.isValidCode(this.playerInfo.totpToken, args[1])) {
              this.finish();
            } else {
              this.sendMessage();
            }
          } else {
            this.sendMessage();
          }
          break;
        }
        default: {
          this.sendMessage();
          break;
        }
      }
    }
  }

  public static RegisteredPlayer fetchInfo(Dao<RegisteredPlayer, String> playerDao, String nickname) {
    List<RegisteredPlayer> playerList = null;
    try {
      playerList = playerDao.queryForEq("NICKNAME", nickname);
    } catch (SQLException e) {
      e.printStackTrace();
    }

    return (playerList != null ? playerList.size() : 0) == 0 ? null : playerList.get(0);
  }

  private RegisteredPlayer fetchInfo(String nickname) {
    return fetchInfo(this.playerDao, nickname);
  }

  public static CodeVerifier getVerifier() {
    return verifier;
  }

  private boolean checkPassword(String password) {
    boolean isCorrect = BCrypt.verifyer()
        .verify(password.getBytes(StandardCharsets.UTF_8), this.playerInfo.hash.getBytes(StandardCharsets.UTF_8)).verified;

    if (!isCorrect && !Settings.IMP.MAIN.MIGRATION_HASH.isEmpty()) {
      isCorrect = MigrationHash.valueOf(Settings.IMP.MAIN.MIGRATION_HASH).checkPassword(this.playerInfo.hash, password);

      if (isCorrect) {
        this.playerInfo.hash = genHash(password);
        try {
          this.playerDao.update(this.playerInfo);
        } catch (SQLException e) {
          e.printStackTrace();
        }
      }
    }

    return isCorrect;
  }

  private void checkIp() {
    List<RegisteredPlayer> alreadyRegistered = null;
    try {
      alreadyRegistered = this.playerDao.queryForEq("IP", this.ip);
    } catch (SQLException e) {
      e.printStackTrace();
    }
    assert alreadyRegistered != null;

    AtomicInteger sizeOfValid = new AtomicInteger(alreadyRegistered.size());

    if (Settings.IMP.MAIN.IP_LIMIT_VALID_TIME != 0) {
      long checkDate = System.currentTimeMillis() - Settings.IMP.MAIN.IP_LIMIT_VALID_TIME;

      alreadyRegistered.stream()
          .filter(e -> e.regDate < checkDate)
          .forEach(e -> {
            try {
              e.ip = "";
              this.playerDao.update(e);
              sizeOfValid.decrementAndGet();
            } catch (SQLException ex) {
              ex.printStackTrace();
            }
          });
    }

    if (sizeOfValid.get() >= Settings.IMP.MAIN.IP_LIMIT_REGISTRATIONS) {
      this.proxyPlayer.disconnect(LegacyComponentSerializer.legacyAmpersand().deserialize(Settings.IMP.MAIN.STRINGS.IP_LIMIT)
      );
    }
  }

  private void checkCase() {
    if (!this.proxyPlayer.getUsername().equals(this.playerInfo.nickname)) {
      this.proxyPlayer.disconnect(LegacyComponentSerializer.legacyAmpersand().deserialize(Settings.IMP.MAIN.STRINGS.WRONG_NICKNAME_CASE));
    }
  }

  private void register(String password) {
    RegisteredPlayer registeredPlayer = new RegisteredPlayer(
        this.proxyPlayer.getUsername(),
        this.proxyPlayer.getUsername().toLowerCase(Locale.ROOT),
        genHash(password),
        this.ip,
        "",
        System.currentTimeMillis()
    );

    try {
      this.playerDao.create(registeredPlayer);
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  private void finishOrTotp() {
    if (this.playerInfo.totpToken.isEmpty()) {
      this.finish();
    } else {
      this.totp = true;
      this.sendMessage();
    }
  }

  private void finish() {
    this.proxyPlayer.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(Settings.IMP.MAIN.STRINGS.LOGIN_SUCCESS));
    AuthPlugin.getInstance().cacheAuthUser(this.proxyPlayer);
    this.player.disconnect();
  }

  private void sendMessage() {
    if (this.totp) {
      this.proxyPlayer.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(Settings.IMP.MAIN.STRINGS.TOTP));
    } else if (this.playerInfo == null) {
      this.proxyPlayer.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(Settings.IMP.MAIN.STRINGS.REGISTER));
    } else {
      this.proxyPlayer.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(Settings.IMP.MAIN.STRINGS.LOGIN));
    }
  }

  public static String genHash(String password) {
    return BCrypt.withDefaults().hashToString(Settings.IMP.MAIN.BCRYPT_COST, password.toCharArray());
  }
}
