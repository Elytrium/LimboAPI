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

package net.elytrium.limboapi.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;

public class UpdatesChecker {

  public static void checkForUpdates(Logger logger, String currentVersion, String url, String pluginName, String downloadUrl) {
    try {
      URLConnection conn = new URL(url).openConnection();
      int timeout = (int) TimeUnit.SECONDS.toMillis(5);
      conn.setConnectTimeout(timeout);
      conn.setReadTimeout(timeout);
      try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
        String latestVersion = in.readLine();
        if (latestVersion == null) {
          logger.warn("Unable to check for updates.");
          return;
        }
        String latestVersion0 = getCleanVersion(latestVersion.trim());
        String currentVersion0 = getCleanVersion(currentVersion);
        int latestVersionID = Integer.parseInt(latestVersion0.replace(".", "").replace("$", ""));
        int currentVersionID = Integer.parseInt(currentVersion0.replace(".", "").replace("$", ""));
        if (latestVersion0.endsWith("$")) {
          --latestVersionID;
        }
        if (currentVersion0.endsWith("$")) {
          --currentVersionID;
        }

        if (currentVersionID < latestVersionID) {
          logger.error("****************************************");
          logger.warn("The new " + pluginName + " update was found, please update.");
          logger.error(downloadUrl);
          logger.error("****************************************");
        }
      }
    } catch (IOException ex) {
      logger.warn("Unable to check for updates.", ex);
    }
  }

  private static String getCleanVersion(String version) {
    int indexOf = version.indexOf("-");
    if (indexOf > 0) {
      return version.substring(0, indexOf) + "$"; // "$" - Indicates that the version is snapshot
    } else {
      return version;
    }
  }
}
