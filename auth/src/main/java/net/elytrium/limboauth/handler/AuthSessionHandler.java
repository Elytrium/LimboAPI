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
import lombok.Getter;
import lombok.SneakyThrows;
import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.LimboSessionHandler;
import net.elytrium.limboapi.api.player.LimboPlayer;
import net.elytrium.limboauth.AuthPlugin;
import net.elytrium.limboauth.config.Settings;
import net.elytrium.limboauth.model.RegisteredPlayer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class AuthSessionHandler implements LimboSessionHandler {
  private static final TimeProvider timeProvider = new SystemTimeProvider();
  private static final CodeGenerator codeGenerator = new DefaultCodeGenerator();
  @Getter
  private static final CodeVerifier verifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
  private final Dao<RegisteredPlayer, String> playerDao;
  private final Player proxyPlayer;
  private final RegisteredPlayer playerInfo;
  private LimboPlayer player;
  private String ip;
  private boolean totp = false;

  private int attempts = Settings.IMP.MAIN.LOGIN_ATTEMPTS;

  public AuthSessionHandler(
      Dao<RegisteredPlayer, String> playerDao, Player proxyPlayer, String lowercaseNickname) {
    this.playerDao = playerDao;
    this.proxyPlayer = proxyPlayer;
    this.playerInfo = fetchInfo(lowercaseNickname);
  }

  public void onChat(String message) {
    String[] args = message.split(" ");
    if (args.length != 0) {
      switch (args[0]) {
        case "/reg":
        case "/register":
        case "/r":
          if (args.length >= 3 && !totp && playerInfo == null && args[1].equals(args[2])) {
            register(args[1]);
            finish();
          } else {
            sendMessage();
          }
          break;
        case "/log":
        case "/login":
        case "/l":
          if (args.length >= 2 && !totp && playerInfo != null) {
            if (checkPassword(args[1])) {
              finishOrTotp();
            } else if (attempts-- != 0) {
              proxyPlayer.sendMessage(
                  LegacyComponentSerializer.legacyAmpersand().deserialize(Settings.IMP.MAIN.STRINGS.PASSWORD_WRONG));
            } else {
              proxyPlayer.disconnect(
                  LegacyComponentSerializer.legacyAmpersand().deserialize(Settings.IMP.MAIN.STRINGS.PASSWORD_WRONG));
            }
          } else {
            sendMessage();
          }
          break;
        case "/2fa":
          if (args.length >= 2 && totp) {
            if (verifier.isValidCode(playerInfo.totpToken, args[1])) {
              finish();
            } else {
              sendMessage();
            }
          } else {
            sendMessage();
          }
          break;
        default:
          sendMessage();
          break;
      }
    }
  }

  @Override
  public void onSpawn(Limbo server, LimboPlayer player) {
    this.player = player;
    this.ip = proxyPlayer.getRemoteAddress().getAddress().getHostAddress();

    if (playerInfo == null) {
      checkIp();
    } else {
      checkCase();
    }
    sendMessage();
  }

  @SneakyThrows
  public static RegisteredPlayer fetchInfo(Dao<RegisteredPlayer, String> playerDao, String nickname) {
    List<RegisteredPlayer> playerList = playerDao.queryForEq("nickname", nickname);
    return playerList.size() == 0 ? null : playerList.get(0);
  }

  private RegisteredPlayer fetchInfo(String nickname) {
    return fetchInfo(playerDao, nickname);
  }

  private boolean checkPassword(String password) {
    return BCrypt.verifyer().verify(
        password.getBytes(StandardCharsets.UTF_8),
        playerInfo.hash.getBytes(StandardCharsets.UTF_8)
    ).verified;
  }

  @SneakyThrows
  private void checkIp() {
    List<RegisteredPlayer> alreadyRegistered = playerDao.queryForEq("ip", ip);

    AtomicInteger sizeOfValid = new AtomicInteger(alreadyRegistered.size());
    long checkDate = System.currentTimeMillis() - Settings.IMP.MAIN.IP_LIMIT_VALID_TIME;

    alreadyRegistered.stream()
        .filter(e -> e.regdate < checkDate)
        .forEach(e -> {
          try {
            e.ip = "";
            playerDao.update(e);
            sizeOfValid.decrementAndGet();
          } catch (SQLException ex) {
            ex.printStackTrace();
          }
        });

    if (sizeOfValid.get() >= Settings.IMP.MAIN.IP_LIMIT_REGISTRATIONS) {
      proxyPlayer.disconnect(
          LegacyComponentSerializer.legacyAmpersand().deserialize(Settings.IMP.MAIN.STRINGS.IP_LIMIT)
      );
    }
  }

  private void checkCase() {
    if (!proxyPlayer.getUsername().equals(playerInfo.nickname)) {
      proxyPlayer.disconnect(
          LegacyComponentSerializer.legacyAmpersand().deserialize(Settings.IMP.MAIN.STRINGS.WRONG_NICKNAME_CASE));
    }
  }

  @SneakyThrows
  private void register(String password) {
    String hash = genHash(password);

    RegisteredPlayer registeredPlayer = new RegisteredPlayer(
        proxyPlayer.getUsername(),
        proxyPlayer.getUsername().toLowerCase(Locale.ROOT),
        hash,
        ip,
        "",
        System.currentTimeMillis()
    );

    playerDao.create(registeredPlayer);
  }

  private void finishOrTotp() {
    if (playerInfo.totpToken.isEmpty()) {
      finish();
    } else {
      totp = true;
      sendMessage();
    }
  }

  private void finish() {
    player.disconnect();
    AuthPlugin.getInstance().cacheAuthUser(proxyPlayer);
  }

  private void sendMessage() {
    if (totp) {
      proxyPlayer.sendMessage(
          LegacyComponentSerializer.legacyAmpersand().deserialize(Settings.IMP.MAIN.STRINGS.TOTP));
    } else if (playerInfo == null) {
      proxyPlayer.sendMessage(
          LegacyComponentSerializer.legacyAmpersand().deserialize(Settings.IMP.MAIN.STRINGS.REGISTER));
    } else {
      proxyPlayer.sendMessage(
          LegacyComponentSerializer.legacyAmpersand().deserialize(Settings.IMP.MAIN.STRINGS.LOGIN));
    }
  }

  public static String genHash(String password) {
    return BCrypt.withDefaults().hashToString(
        Settings.IMP.MAIN.BCRYPT_COST,
        password.toCharArray()
    );
  }
}
