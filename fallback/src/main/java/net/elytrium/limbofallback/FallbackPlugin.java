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

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import net.elytrium.limboapi.BuildConstants;
import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.LimboFactory;
import net.elytrium.limboapi.api.chunk.Dimension;
import net.elytrium.limboapi.api.chunk.VirtualWorld;
import net.elytrium.limboapi.api.file.SchematicFile;
import net.elytrium.limboapi.api.file.WorldFile;
import net.elytrium.limbofallback.handler.FallBackSessionHandler;
import net.elytrium.limbofallback.listener.FallbackListener;
import org.slf4j.Logger;

@Plugin(
    id = "limbofallback",
    name = "LimboFallback",
    version = BuildConstants.LIMBO_VERSION,
    url = "ely.su",
    authors = {"hevav", "mdxd44"},
    dependencies = {@Dependency(id = "limboapi")}
)

public class FallbackPlugin {

  private static FallbackPlugin instance;

  private final Path dataDirectory;
  private final Logger logger;
  private final ProxyServer server;
  private final LimboFactory factory;
  private Limbo fallbackServer;

  @Inject
  @SuppressWarnings("OptionalGetWithoutIsPresent")
  public FallbackPlugin(ProxyServer server, Logger logger, @Named("limboapi") PluginContainer factory, @DataDirectory Path dataDirectory) {
    setInstance(this);

    this.server = server;
    this.logger = logger;
    this.dataDirectory = dataDirectory;
    this.factory = (LimboFactory) factory.getInstance().get();
  }

  @Subscribe
  public void onProxyInitialization(ProxyInitializeEvent event) {
    this.server.getEventManager().register(this, new FallbackListener());
    this.reload();
  }

  @SuppressWarnings("SwitchStatementWithTooFewBranches")
  public void reload() {
    Settings.IMP.reload(new File(this.dataDirectory.toFile().getAbsoluteFile(), "config.yml"));

    Settings.MAIN.FALLBACK_COORDS fallbackCoords = Settings.IMP.MAIN.FALLBACK_COORDS;
    VirtualWorld fallbackWorld = this.factory.createVirtualWorld(
        Dimension.valueOf(Settings.IMP.MAIN.DIMENSION),
        fallbackCoords.X, fallbackCoords.Y, fallbackCoords.Z,
        (float) fallbackCoords.YAW, (float) fallbackCoords.PITCH
    );

    if (Settings.IMP.MAIN.LOAD_WORLD) {
      try {
        Path path = this.dataDirectory.resolve(Settings.IMP.MAIN.WORLD_FILE_PATH);
        WorldFile file;
        switch (Settings.IMP.MAIN.WORLD_FILE_TYPE) {
          case "schematic": {
            file = new SchematicFile(path);
            break;
          }
          default: {
            this.logger.error("Incorrect world file type.");
            this.server.shutdown();
            return;
          }
        }

        Settings.MAIN.WORLD_COORDS coords = Settings.IMP.MAIN.WORLD_COORDS;
        file.toWorld(this.factory, fallbackWorld, coords.X, coords.Y, coords.Z);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    this.fallbackServer = this.factory.createLimbo(fallbackWorld);
  }

  public void sendToFallBackServer(Player player, RegisteredServer serverToConnect) {
    try {
      this.fallbackServer.spawnPlayer(player, new FallBackSessionHandler(player, serverToConnect));
    } catch (Throwable t) {
      this.logger.error("Error", t);
    }
  }

  private static void setInstance(FallbackPlugin instance) {
    FallbackPlugin.instance = instance;
  }

  public static FallbackPlugin getInstance() {
    return instance;
  }

  public Logger getLogger() {
    return this.logger;
  }

  public ProxyServer getServer() {
    return this.server;
  }

  public LimboFactory getFactory() {
    return this.factory;
  }
}
