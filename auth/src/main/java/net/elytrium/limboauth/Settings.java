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

package net.elytrium.limboauth;

import java.io.File;
import net.elytrium.limboapi.config.Config;

public class Settings extends Config {

  @Ignore
  public static final Settings IMP = new Settings();

  public String PREFIX = "LimboAuth &6>>&f";

  @Create
  public MAIN MAIN;

  public static class MAIN {

    //public boolean ENABLE_BOSSBAR = true;
    public boolean ONLINE_MODE_NEED_AUTH = true;
    public boolean FORCE_OFFLINE_UUID = false;
    @Comment({
        "Forcibly set player's UUID to the value from the database",
        "If the player had the cracked account, and switched to the premium account, the cracked UUID will be used."
    })
    public boolean SAVE_UUID = true;
    public boolean ENABLE_TOTP = true;
    public boolean TOTP_NEED_PASS = true;
    public boolean REGISTER_NEED_REPEAT_PASSWORD = true;
    public boolean CHANGE_PASSWORD_NEED_OLD_PASS = true;
    @Comment({
        "If you want to migrate your database from another plugin, which is not using BCrypt",
        "You can set an old hash algorithm to migrate from. Currently, only AUTHME is supported yet"
    })
    public String MIGRATION_HASH = "";
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
    @Comment({
        "Regex of allowed nicknames",
        "^ means the start of the line, $ means the end of the line",
        "[A-Za-z0-9_] is a character set of A-Z, a-z, 0-9 and _",
        "{3,16} means that allowed length is from 3 to 16 chars"
    })
    public String ALLOWED_NICKNAME_REGEX = "^[A-Za-z0-9_]{3,16}$";

    public boolean LOAD_WORLD = false;
    @Comment("World file type: schematic")
    public String WORLD_FILE_TYPE = "schematic";
    public String WORLD_FILE_PATH = "world.schematic";
    @Comment({
        "Custom isPremium URL",
        "You can use Mojang one's API (set by default)",
        "Or CloudFlare one's: https://api.ashcon.app/mojang/v1/user/%s",
        "Or use this code to make your own API: https://blog.cloudflare.com/minecraft-api-with-workers-coffeescript/",
        "Or implement your own API, it should just respond with HTTP code 200 only if the player is premium"
    })
    public String ISPREMIUM_AUTH_URL = "https://api.mojang.com/users/profiles/minecraft/%s";

    @Create
    public Settings.MAIN.WORLD_COORDS WORLD_COORDS;

    public static class WORLD_COORDS {

      public int X = 0;
      public int Y = 0;
      public int Z = 0;
    }

    @Create
    public MAIN.STRINGS STRINGS;

    //@Comment("Leave empty to disable.")
    public static class STRINGS {

      public String RELOAD = "{PRFX} &aReloaded successfully!";
      public String RELOAD_FAILED = "{PRFX} &cReload failed, check console for details.";
      public String ERROR_OCCURRED = "{PRFX} &cAn internal error has occurred!";

      public String NOT_PLAYER = "{PRFX} &cСonsole is not allowed to execute this command!";
      public String NOT_REGISTERED = "{PRFX} &cYou are not registered!";
      public String PASSWORD_WRONG = "{PRFX} &cPassword is wrong!";

      public String LOGIN = "{PRFX} Please, login using &6/login &6<password>";
      public String LOGIN_SUCCESS = "{PRFX} &aSuccessfully logged in!";
      //public String LOGIN_TITLE = "";
      //public String LOGIN_SUBTITLE = "";
      //public String LOGIN_SUCCESS_TITLE = "";
      //public String LOGIN_SUCCESS_SUBTITLE = "";

      @Comment("Or if register-need-repeat-password set to false remove the \"<repeat password>\" part.")
      public String REGISTER = "{PRFX} Please, register using &6/register <password> <repeat password>";
      //public String REGISTER_TITLE = "";
      //public String REGISTER_SUBTITLE = "";
      public String DIFFERENT_PASSWORDS = "{PRFX} The entered passwords differ from each other.";
      public String KICK_PASSWORD_WRONG = "{NL}{NL}&cYou've entered the wrong password numerous times!";

      public String WRONG_NICKNAME_CASE = "{NL}{NL}&cThe case of your nickname is wrong. Nickname is CaSe SeNsItIvE.";
      public String NICKNAME_INVALID = "{NL}{NL}&cYour nickname contains forbidden characters. Please, change your nickname!";
      @Comment("6 hours by default in ip-limit-valid-time")
      public String IP_LIMIT = "{PRFX} &cYour IP has reached max registered accounts. If this is an error, restart your router, or wait about 6 hours.";

      public String UNREGISTER_SUCCESSFUL = "{PRFX}{NL}{NL}&aSuccessfully unregistered!";
      public String UNREGISTER_USAGE = "{PRFX} Usage: &6/unregister <current password> confirm";

      public String FORCE_UNREGISTER_SUCCESSFUL = "{PRFX} &a{0} successfully unregistered!";
      public String FORCE_UNREGISTER_SUCCESSFUL_PLAYER = "{PRFX}{NL}{NL}&aYou have been unregistered by administrator!";
      public String FORCE_UNREGISTER_ERROR = "{PRFX} &cUnable to unregister {0}. Most likely this player has never been on this server.";
      public String FORCE_UNREGISTER_USAGE = "{PRFX} Usage: &6/forceunregister <nickname>";

      public String CHANGE_PASSWORD_SUCCESSFUL = "{PRFX} &aSuccessfully changed password!";
      @Comment("Or if change-password-need-old-pass set to false remove the \"<old password>\" part.")
      public String CHANGE_PASSWORD_USAGE = "{PRFX} Usage: &6/changepassword <old password> <new password>";

      public String TOTP = "{PRFX} Please, enter your 2FA key using &6/2fa <key>";
      public String TOTP_SUCCESSFUL = "{PRFX} &aSuccessfully enabled 2FA!";
      public String TOTP_DISABLED = "{PRFX} &aSuccessfully disabled 2FA!";
      @Comment("Or if totp-need-pass set to false remove the \"<current password>\" part.")
      public String TOTP_USAGE = "{PRFX} Usage: &6/2fa enable <current password>&f or &6/2fa disable <totp key>&f.";
      public String TOTP_WRONG = "{PRFX} &cWrong 2FA key!";
      public String TOTP_ALREADY_ENABLED = "{PRFX} &c2FA is already enabled. Disable it using &6/2fa disable <key>&c.";
      public String TOTP_QR = "{PRFX} Click here to open 2FA QR code in browser.";
      public String TOTP_TOKEN = "{PRFX} &aYour 2FA token &7(Click to copy)&a: &6{0}";
      public String TOTP_RECOVERY = "{PRFX} &aYour recovery codes &7(Click to copy)&a: &6{0}";

      public String DESTROY_SESSION_SUCCESS = "{PRFX} &eYour session is now destroyed, you'll need to log in again after reconnecting.";
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

    @Comment("Database type: mysql, postgresql or h2.")
    public String STORAGE_TYPE = "h2";

    @Comment("Settings for Network-based database (like MySQL, PostgreSQL): ")
    public String HOSTNAME = "127.0.0.1:3306";
    public String USER = "user";
    public String PASSWORD = "password";
    public String DATABASE = "limboauth";
    public String CONNECTION_PARAMETERS = "autoReconnect=true&initialTimeout=1&useSSL=false";
  }

  public void reload(File file) {
    if (this.load(file, this.PREFIX)) {
      this.save(file);
    } else {
      this.save(file);
      this.load(file, this.PREFIX);
    }
  }
}
