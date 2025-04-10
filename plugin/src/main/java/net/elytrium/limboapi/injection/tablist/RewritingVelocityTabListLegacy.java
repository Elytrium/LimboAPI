/*
 * Copyright (C) 2021 - 2025 Elytrium
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

package net.elytrium.limboapi.injection.tablist;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.player.TabListEntry;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.tablist.KeyedVelocityTabListEntry;
import com.velocitypowered.proxy.tablist.VelocityTabListLegacy;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class RewritingVelocityTabListLegacy extends VelocityTabListLegacy implements RewritingTabList {

  // To keep compatibility with other plugins that use internal fields
  protected final ConnectedPlayer player;
  protected final MinecraftConnection connection;
  protected final ProxyServer proxyServer;
  protected final Map<UUID, KeyedVelocityTabListEntry> entries;

  public RewritingVelocityTabListLegacy(ConnectedPlayer player, ProxyServer proxyServer) {
    super(player, proxyServer);
    this.player = super.player;
    this.connection = super.connection;
    this.proxyServer = super.proxyServer;
    this.entries = super.entries;
  }

  @Override
  public void addEntry(TabListEntry entry) {
    super.addEntry(this.rewriteEntry(entry));
  }

  @Override
  public Optional<TabListEntry> getEntry(UUID uuid) {
    return super.getEntry(this.rewriteUuid(uuid));
  }

  @Override
  public boolean containsEntry(UUID uuid) {
    return super.containsEntry(this.rewriteUuid(uuid));
  }

  @Override
  public Optional<TabListEntry> removeEntry(UUID uuid) {
    return super.removeEntry(this.rewriteUuid(uuid));
  }
}
