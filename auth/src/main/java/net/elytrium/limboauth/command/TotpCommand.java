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

package net.elytrium.limboauth.command;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.UpdateBuilder;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.recovery.RecoveryCodeGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Locale;
import net.elytrium.limboauth.config.Settings;
import net.elytrium.limboauth.handler.AuthSessionHandler;
import net.elytrium.limboauth.model.RegisteredPlayer;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class TotpCommand implements SimpleCommand {

  private final SecretGenerator secretGenerator = new DefaultSecretGenerator();
  private final Dao<RegisteredPlayer, String> playerDao;

  public TotpCommand(Dao<RegisteredPlayer, String> playerDao) {
    this.playerDao = playerDao;
  }

  @Override
  public void execute(final Invocation invocation) {
    final CommandSource source = invocation.source();
    final String[] args = invocation.arguments();

    if (args.length == 0 || !(source instanceof Player)) {
      this.sendUsage(source);
    } else {
      String username = ((Player) source).getUsername();

      switch (args[0]) {
        case "enable":
          RegisteredPlayer playerInfo =
              AuthSessionHandler.fetchInfo(this.playerDao, username.toLowerCase(Locale.ROOT));

          if (playerInfo == null) {
            source.sendMessage(
                LegacyComponentSerializer
                    .legacyAmpersand()
                    .deserialize(Settings.IMP.MAIN.STRINGS.TOTP_NON_REGISTERED));
            return;
          }

          if (!playerInfo.totpToken.isBlank()) {
            source.sendMessage(
                LegacyComponentSerializer
                    .legacyAmpersand()
                    .deserialize(Settings.IMP.MAIN.STRINGS.TOTP_ALREADY_ENABLED));
            return;
          }

          String secret = this.secretGenerator.generate();

          UpdateBuilder<RegisteredPlayer, String> updateBuilder = this.playerDao.updateBuilder();
          try {
            updateBuilder.where().eq("nickname", username);
            updateBuilder.updateColumnValue("totpToken", secret);
            updateBuilder.update();
          } catch (SQLException e) {
            e.printStackTrace();

            source.sendMessage(
                LegacyComponentSerializer
                    .legacyAmpersand()
                    .deserialize(Settings.IMP.MAIN.STRINGS.ERROR_OCCURRED));
          }

          source.sendMessage(
              LegacyComponentSerializer
                  .legacyAmpersand()
                  .deserialize(Settings.IMP.MAIN.STRINGS.TOTP_SUCCESSFUL));

          QrData data = new QrData.Builder()
              .label(username)
              .secret(secret)
              .issuer(Settings.IMP.MAIN.TOTP_ISSUER)
              .build();

          String qrUrl = Settings.IMP.MAIN.QR_GENERATOR_URL
              .replace("{data}", URLEncoder.encode(data.getUri(), StandardCharsets.UTF_8));

          source.sendMessage(
              LegacyComponentSerializer
                  .legacyAmpersand()
                  .deserialize(Settings.IMP.MAIN.STRINGS.TOTP_QR)
                  .clickEvent(ClickEvent.openUrl(qrUrl)));

          source.sendMessage(
              LegacyComponentSerializer
                  .legacyAmpersand()
                  .deserialize(Settings.IMP.MAIN.STRINGS.TOTP_TOKEN + secret)
                  .clickEvent(ClickEvent.suggestCommand(secret)));

          RecoveryCodeGenerator recoveryCodes = new RecoveryCodeGenerator();
          String[] codes = recoveryCodes.generateCodes(16);

          source.sendMessage(
              LegacyComponentSerializer
                  .legacyAmpersand()
                  .deserialize(Settings.IMP.MAIN.STRINGS.TOTP_RECOVERY + String.join(", ", codes)));
          break;
        case "disable":
          if (args.length != 2) {
            this.sendUsage(source);
            return;
          }

          playerInfo = AuthSessionHandler.fetchInfo(this.playerDao, username.toLowerCase(Locale.ROOT));

          if (playerInfo == null) {
            source.sendMessage(
                LegacyComponentSerializer
                    .legacyAmpersand()
                    .deserialize(Settings.IMP.MAIN.STRINGS.TOTP_NON_REGISTERED));
            return;
          }

          if (AuthSessionHandler.getVerifier().isValidCode(playerInfo.totpToken, args[1])) {
            try {
              updateBuilder = this.playerDao.updateBuilder();
              updateBuilder.where().eq("nickname", username);
              updateBuilder.updateColumnValue("totpToken", "");
              updateBuilder.update();

              source.sendMessage(
                  LegacyComponentSerializer
                      .legacyAmpersand()
                      .deserialize(Settings.IMP.MAIN.STRINGS.TOTP_DISABLED));
            } catch (SQLException e) {
              e.printStackTrace();

              source.sendMessage(
                  LegacyComponentSerializer
                      .legacyAmpersand()
                      .deserialize(Settings.IMP.MAIN.STRINGS.ERROR_OCCURRED));
            }
          } else {
            source.sendMessage(
                LegacyComponentSerializer
                    .legacyAmpersand()
                    .deserialize(Settings.IMP.MAIN.STRINGS.TOTP_WRONG));
          }
          break;
        default:
          this.sendUsage(source);
          break;
      }
    }
  }

  private void sendUsage(CommandSource source) {
    source.sendMessage(
        LegacyComponentSerializer
            .legacyAmpersand()
            .deserialize(Settings.IMP.MAIN.STRINGS.TOTP_USAGE));
  }
}
