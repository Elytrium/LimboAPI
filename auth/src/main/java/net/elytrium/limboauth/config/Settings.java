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

package net.elytrium.limboauth.config;

import java.io.File;

public class Settings extends Config {

  @Ignore
  public static final Settings IMP = new Settings();

  @Final
  public String VERSION = "1.0.0";

  public String PREFIX = "&5&fLimboAuth &6>>&f";

  @Create
  public MAIN MAIN;

  public static class MAIN {
    public boolean ONLINE_MODE_NEED_AUTH = true;
    public boolean ONLINE_UUID_IF_POSSIBLE = true;
    @Comment("Available dimensions: OVERWORLD, NETHER, THE_END")
    public String DIMENSION = "THE_END";
    public long PURGE_CACHE_MILLIS = 3600000;
    @Comment("QR Generator URL, set {data} placeholder")
    public String QR_GENERATOR_URL = "https://api.qrserver.com/v1/create-qr-code/?data={data}&size=200x200&ecc=M&margin=30";
    public String TOTP_ISSUER = "LimboAuth by Elytrium";
    public int BCRYPT_COST = 10;
    public int LOGIN_ATTEMPTS = 3;
    public int IP_LIMIT_REGISTRATIONS = 3;
    @Comment("Time in milliseconds, when ip limit works, set to 0 for disable")
    public long IP_LIMIT_VALID_TIME = 21600000;
    @Comment("In lowercase")
    public String ALLOWED_NICKNAME_CHARS = "abcdefghijklmnopqrstuvwxyz_0123456789";

    public boolean LOAD_WORLD = false;
    @Comment("World file type: schematic")
    public String WORLD_FILE_TYPE = "schematic";
    public String WORLD_FILE_PATH = "world.schematic";

    @Create
    public Settings.MAIN.WORLD_COORDS WORLD_COORDS;

    public static class WORLD_COORDS {
      public int X = 0;
      public int Y = 0;
      public int Z = 0;
    }

    @Create
    public MAIN.STRINGS STRINGS;

    public static class STRINGS {
      public String RELOAD = "{PRFX} Reloaded successfully";
      public String RELOAD_FAILED = "{PRFX} &cReload failed, check console for details.";
      public String NOT_PLAYER = "{PRFX} &cСonsole is not allowed to execute this command!";
      public String ERROR_OCCURRED = "{PRFX} &cAn internal error has occurred!";

      public String LOGIN = "{PRFX} Please, login using &a/l &6password";
      public String LOGIN_SUCCESS = "{PRFX} &aSuccessfully logged in";
      public String PASSWORD_WRONG = "{PRFX} &cPassword is wrong";

      public String REGISTER = "{PRFX} Please, register using &a/reg &6password password";

      public String WRONG_NICKNAME_CASE = "{NL}{NL}&cThe case of your nickname is wrong. Nickname is CaSe SeNsItIvE.";
      public String NICKNAME_PREMIUM = "{PRFX} Your nickname is set on premium account. Please, change your nickname";
      public String NICKNAME_INVALID = "{NL}{NL}&cYour nickname contains forbidden characters. Please, change your nickname";
      @Comment("6 hours by default in ip-limit-valid-time")
      public String IP_LIMIT = "{PRFX} Your IP has reached max registered accounts. If this is an error, restart your router, or wait about 6 hours";

      public String UNREGISTER_SUCCESSFUL = "{PRFX} Successfully unregistered player";
      public String UNREGISTER_USAGE = "{PRFX} Usage: /unregister <nickname>";

      public String CHANGE_PASSWORD_USAGE = "{PRFX} Usage: /changepass <old password> <new password>";
      public String CHANGE_PASSWORD_SUCCESSFUL = "{PRFX} Successfully changed password";

      public String TOTP = "{PRFX} Please, enter your 2FA key using &a/2fa key";
      public String TOTP_SUCCESSFUL = "{PRFX} Successfully enabled 2FA";
      public String TOTP_DISABLED = "{PRFX} Successfully disabled 2FA";
      public String TOTP_USAGE = "{PRFX} Usage: &a/2fa enable &for &a/2fa disable <totp key>";
      public String TOTP_WRONG = "{PRFX} &cWrong totp key!";
      public String TOTP_ALREADY_ENABLED = "{PRFX} 2FA is already enabled. Disable it using /2fa disable <totp key>";
      public String TOTP_NON_REGISTERED = "{PRFX} You are not registered!";
      public String TOTP_QR = "{PRFX} Click to open 2FA QR code in browser. ";
      public String TOTP_TOKEN = "{PRFX} Your TOTP token (Click to copy): ";
      public String TOTP_RECOVERY = "{PRFX} Your recovery codes: ";
    }

    @Create
    public MAIN.AUTH_COORDS AUTH_COORDS;

    public static class AUTH_COORDS {
      public double X = 0;
      public double Y = 0;
      public double Z = 0;
      public double YAW = 0;
      public double PITCH = 0;
    }
  }

  @Create
  public DATABASE DATABASE;

  @Comment("Database settings")
  public static class DATABASE {
    @Comment("Database type: mysql, postgresql, h2, or sqlite")
    public String STORAGE_TYPE = "h2";

    @Comment("Settings for File-based databases (Like H2, SQLite): ")
    public String FILENAME = "limboauth.db";

    @Comment("Settings for Network-based database (like MySQL, PostgreSQL): ")
    public String HOSTNAME = "127.0.0.1:3306";
    public String USER = "user";
    public String PASSWORD = "password";
    public String DATABASE = "limboauth";
  }

  public void reload(File file) {
    this.load(file);
    this.save(file);

    // Placeholders fix
    this.load(file);
  }
}
