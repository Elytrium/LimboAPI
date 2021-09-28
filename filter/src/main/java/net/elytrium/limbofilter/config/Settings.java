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

package net.elytrium.limbofilter.config;

import java.io.File;
import java.util.List;

public class Settings extends Config {

  @Ignore
  public static final Settings IMP = new Settings();

  public String PREFIX = "LimboFilter &6>>&f";

  @Create
  public MAIN MAIN;

  public static class MAIN {
    public boolean CHECK_CLIENT_SETTINGS = true;
    public boolean CHECK_CLIENT_BRAND = true;
    public long PURGE_CACHE_MILLIS = 3600000;
    public int CAPTCHA_ATTEMPTS = 2;
    public int NON_VALID_POSITION_XZ_ATTEMPTS = 10;
    public int NON_VALID_POSITION_Y_ATTEMPTS = 10;
    public int FALLING_CHECK_TICKS = 128;
    public double MAX_VALID_POSITION_DIFFERENCE = 0.01;
    public boolean FALLING_CHECK_DEBUG = false;
    public int MAX_SINGLE_GENERIC_PACKET_LENGTH = 2048;
    public int MAX_MULTI_GENERIC_PACKET_LENGTH = 131072;
    public String BRAND = "LimboFilter";

    @Comment("Available - ONLY_POSITION, ONLY_CAPTCHA, CAPTCHA_POSITION, CAPTCHA_ON_POSITION_FAILED, SUCCESSFULLY")
    public String CHECK_STATE = "CAPTCHA_POSITION";

    public boolean LOAD_WORLD = false;
    @Comment("World file type: schematic")
    public String WORLD_FILE_TYPE = "schematic";
    public String WORLD_FILE_PATH = "world.schematic";

    @Create
    public Settings.MAIN.CONNECTION_LIMIT CONNECTION_LIMIT;

    @Comment({
        "Minimum total connections per second amount to toggle anti-bot checks",
        "-1 to disable the check",
        "0 to enable on any connections per second"
    })
    public static class CONNECTION_LIMIT {
      @Comment({
          "All players will bypass all anti-bot checks"
      })
      public int ALL_BYPASS = -1;

      @Comment({
          "Online mode players will bypass all anti-bot checks",
          "Doesn't work with online-mode-verify: -1"
      })
      public int ONLINE_MODE_BYPASS = 3;

      @Comment({
          "Verify Online Mode connection before AntiBot.",
          "If connections per second amount is bigger than the limit: online mode players will need to reconnect",
          "Else: Some attacks can consume more cpu and network, and can lead to long-lasting Mojang rate-limiting"
      })
      public int ONLINE_MODE_VERIFY = 3;

      @Comment({
          "The player will need to reconnect after passing AntiBot check.",
      })
      public int NEED_TO_RECONNECT = 15;

      @Comment({
          "Picture in the MOTD Server Ping packet will be disabled.",
      })
      public int DISABLE_MOTD_PICTURE = 15;
    }

    @Create
    public Settings.MAIN.WORLD_COORDS WORLD_COORDS;

    public static class WORLD_COORDS {
      public int X = 0;
      public int Y = 0;
      public int Z = 0;
    }

    @Create
    public MAIN.CAPTCHA_GENERATOR CAPTCHA_GENERATOR;

    public static class CAPTCHA_GENERATOR {
      @Comment("Path to the background image to draw on captcha (any format, 128x128), none if empty")
      public String BACKPLATE_PATH = "";
      @Comment("Path to the font files to draw on captcha (ttf), can be empty")
      public List<String> FONTS_PATH = List.of("");
      @Comment("Use standard fonts(SANS_SERIF/SERIF/MONOSPACED), use false only if you provide fonts path")
      public boolean USE_STANDARD_FONTS = true;
      public double LETTER_SPACING = 1.5;
      public int FONT_SIZE = 50;
      public boolean FONT_OUTLINE = false;
      public boolean FONT_ROTATE = true;
      public boolean FONT_RIPPLE = true;
      public boolean FONT_BLUR = true;
      public boolean STRIKETHROUGH = false;
      public boolean UNDERLINE = true;
      public String PATTERN = "abcdefghijklmnopqrtuvwxyz1234567890";
      public int LENGTH = 3;
    }

    @Comment(
        "Available dimensions: OVERWORLD, NETHER, THE_END"
    )
    public String BOTFILTER_DIMENSION = "THE_END";

    @Create
    public MAIN.STRINGS STRINGS;

    public static class STRINGS {
      public String CHECKING = "{PRFX} Bot-Filter check was started, please wait..";
      public String CHECKING_CAPTCHA = "{PRFX} Please, solve the captcha";
      public String SUCCESSFUL_CRACKED = "{PRFX} Successfully passed Bot-Filter check. ";
      public String SUCCESSFUL_PREMIUM = "{PRFX} Successfully passed Bot-Filter check. Please, rejoin the server";
      public String CAPTCHA_FAILED = "{PRFX} You've mistaken in captcha check. Please, rejoin the server.";
      public String TOO_BIG_PACKET = "{PRFX} Your client sent too big packet.";
      public String FALLING_CHECK_FAILED = "{PRFX} Falling Check was failed. Please, rejoin the server.";
      public String ALREADY_CONNECTED = "{PRFX} You are already connected.";
      public String STATS_FORMAT = "&c&lTotal Blocked: &6&l{0} &c&l| Connections Per Second: &6&l{1} &c&l| Pings Per Second: &6&l{2} &c&l| Total Connections Per Second: &6&l{3} &c&l| Ping: &6&l{4}";
      public String STATS_ENABLED = "{PRFX} &fNow you see statistics in your action bar.";
      public String STATS_DISABLED = "{PRFX} &fYou're no longer see statistics in your action bar.";
      public String KICK_CLIENT_CHECK_SETTINGS = "&cYour client doesn't send settings packets.";
      public String KICK_CLIENT_CHECK_SETTINGS_CHAT_COLOR = "&cPlease enable colors in chat settings to join the server.{NL}&eOptions > Chat Settings";
      public String KICK_CLIENT_CHECK_SETTINGS_SKIN_PARTS = "&cPlease enable any option from the skin customization to join the server.{NL}&eOptions > Skin Customization";
      public String KICK_CLIENT_CHECK_BRAND = "&cYour client doesn't send brand packets.";
    }

    @Create
    public COORDS COORDS;

    public static class COORDS {
      public double CAPTCHA_X = 0;
      public double CAPTCHA_Y = 0;
      public double CAPTCHA_Z = 0;
      public double CAPTCHA_YAW = 90;
      public double CAPTCHA_PITCH = 38;
      public double FALLING_CHECK_YAW = 90;
      public double FALLING_CHECK_PITCH = 10;
    }
  }

  public void reload(File file) {
    load(file);
    save(file);
    load(file);
  }
}
