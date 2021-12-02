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

package net.elytrium.limboapi;

import java.io.File;
import net.elytrium.limboapi.config.Config;
import org.slf4j.Logger;

public class Settings extends Config {

  @Ignore
  public static final Settings IMP = new Settings();

  @Final
  public String VERSION = BuildConstants.LIMBO_VERSION;

  public String PREFIX = "LimboAPI &6>>&f";

  @Create
  public MAIN MAIN;

  @Comment("Don't use \\n, use {NL} for new line, and {PRFX} for prefix. Ampersand (&) color codes are supported too.")
  public static class MAIN {

    @Comment("Logging for connect and disconnect messages.")
    public boolean LOGGING_ENABLED = true;
    @Comment({
        "Change the parameters below, if you want to reduce the RAM consumption.",
        "If you want to completely block Minecraft versions, use VelocityTools (https://github.com/Elytrium/VelocityTools/releases/latest)",
        "Available versions: 1_7_2, 1_7_6, 1_8, 1_9, 1_9_1, 1_9_2, 1_9_4, 1_10, 1_11, 1_11_1, 1_12, 1_12_1, 1_12_2, "
            + "1_13, 1_13_1, 1_13_2, 1_14, 1_14_1, 1_14_2, 1_14_3, 1_14_4, 1_15, 1_15_1, 1_15_2, 1_16, 1_16_1, 1_16_2, 1_16_3, 1_16_4, 1_17, 1_17_1, 1_18"
    })
    public String PREPARE_MIN_VERSION = "1_7_2";
    public String PREPARE_MAX_VERSION = "1_18";

    @Create
    public MESSAGES MESSAGES;

    public static class MESSAGES {

      public String TOO_BIG_PACKET = "{PRFX} Packet is too big.";
    }
  }

  public void reload(File file) {
    if (this.load(file, this.PREFIX)) {
      this.save(file);
    } else {
      Logger logger = LimboAPI.getInstance().getLogger();
      logger.warn("************* FIRST LAUNCH *************");
      logger.warn("Thanks for installing LimboAPI!");
      logger.warn("(c) 2021 Elytrium");
      logger.warn("");
      logger.warn("Check out our plugins here: https://ely.su/github <3");
      logger.warn("Discord: https://ely.su/discord");
      logger.warn("****************************************");

      this.save(file);
      this.load(file, this.PREFIX);
    }
  }
}
