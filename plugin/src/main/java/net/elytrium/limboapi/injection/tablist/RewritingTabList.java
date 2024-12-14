/*
 * Copyright (C) 2021 - 2024 Elytrium
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

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.player.TabListEntry;
import com.velocitypowered.api.util.GameProfile;
import java.util.UUID;
import net.elytrium.limboapi.LimboAPI;

public interface RewritingTabList {

  Player getPlayer();

  default TabListEntry rewriteEntry(TabListEntry entry) {
    if (entry == null || entry.getProfile() == null || !this.getPlayer().getUniqueId().equals(entry.getProfile().getId())) {
      return entry;
    }

    TabListEntry.Builder builder = TabListEntry.builder();
    builder.tabList(entry.getTabList());
    builder.profile(new GameProfile(this.rewriteUuid(entry.getProfile().getId()),
        entry.getProfile().getName(), entry.getProfile().getProperties()));
    builder.listed(entry.isListed());
    builder.latency(entry.getLatency());
    builder.gameMode(entry.getGameMode());
    entry.getDisplayNameComponent().ifPresent(builder::displayName);
    builder.chatSession(entry.getChatSession());
    builder.listOrder(entry.getListOrder());

    return builder.build();
  }

  default UUID rewriteUuid(UUID uuid) {
    if (this.getPlayer().getUniqueId().equals(uuid)) {
      return LimboAPI.INITIAL_ID.getOrDefault(this.getPlayer(), uuid);
    }

    return uuid;
  }
}
