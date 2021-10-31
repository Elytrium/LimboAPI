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

package net.elytrium.limbofallback;

import java.io.File;
import net.elytrium.limboapi.config.Config;

public class Settings extends Config {

  @Ignore
  public static final Settings IMP = new Settings();

  public String PREFIX = "LimboFallback &6>> &f";

  @Create
  public MAIN MAIN;

  public static class MAIN {

    @Comment("Available dimensions: OVERWORLD, NETHER, THE_END")
    public String DIMENSION = "THE_END";

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
    public FALLBACK_COORDS FALLBACK_COORDS;

    public static class FALLBACK_COORDS {

      public double X = 0;
      public double Y = 0;
      public double Z = 0;
      public double YAW = 0;
      public double PITCH = 0;
    }
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
