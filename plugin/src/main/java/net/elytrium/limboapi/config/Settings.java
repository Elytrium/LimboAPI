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

package net.elytrium.limboapi.config;

import java.io.File;

public class Settings extends Config {

  @Ignore
  public static final Settings IMP = new Settings();

  @Final
  public String VERSION = "1.0.0";

  @Create
  public MESSAGES MESSAGES;

  @Comment("Don't use \\n, use {NL} for new line, and {PRFX} for prefix. Ampersand (&) color codes are supported too.")
  public static class MESSAGES {
    public String PREFIX = "&5Limbo&dAPI&c -> &f";
    public String TOO_BIG_PACKET = "Packet is too big.";
    public String ALREADY_CONNECTED = "You are already connected.";
  }

  @Create
  public MAIN MAIN;

  public static class MAIN {
    public boolean LOGGING_ENABLED = true;
  }

  public void reload(File file) {
    this.load(file);
    this.save(file);
    this.load(file);
  }
}
