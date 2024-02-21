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

package net.elytrium.limboapi.server;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.DisconnectPacket;
import net.elytrium.limboapi.LimboAPI;
import net.elytrium.limboapi.Settings;
import net.elytrium.limboapi.api.protocol.PreparedPacket;

public class CachedPackets {

  private final LimboAPI plugin;

  private PreparedPacket tooBigPacket;
  private PreparedPacket invalidPing;
  private PreparedPacket invalidSwitch;
  private PreparedPacket playTimeOut;
  private PreparedPacket configTimeOut;
  private boolean cached;

  public CachedPackets(LimboAPI plugin) {
    this.plugin = plugin;
  }

  public void createPackets() {
    if (this.cached) {
      this.dispose();
    }

    this.tooBigPacket = this.plugin.createPreparedPacket()
        .prepare(version -> this.createDisconnectPacket(Settings.IMP.MAIN.MESSAGES.TOO_BIG_PACKET, version)).build();
    this.invalidPing = this.plugin.createPreparedPacket()
        .prepare(version -> this.createDisconnectPacket(Settings.IMP.MAIN.MESSAGES.INVALID_PING, version)).build();
    this.invalidSwitch = this.plugin.createConfigPreparedPacket()
        .prepare(version -> this.createDisconnectPacket(Settings.IMP.MAIN.MESSAGES.INVALID_SWITCH, version), ProtocolVersion.MINECRAFT_1_20_2).build();
    this.playTimeOut = this.plugin.createPreparedPacket()
        .prepare(version -> this.createDisconnectPacket(Settings.IMP.MAIN.MESSAGES.TIME_OUT, version)).build();
    this.configTimeOut = this.plugin.createConfigPreparedPacket()
        .prepare(version -> this.createDisconnectPacket(Settings.IMP.MAIN.MESSAGES.TIME_OUT, version), ProtocolVersion.MINECRAFT_1_20_2).build();

    this.cached = true;
  }

  private DisconnectPacket createDisconnectPacket(String message, ProtocolVersion version) {
    return DisconnectPacket.create(LimboAPI.getSerializer().deserialize(message), version, StateRegistry.PLAY);
  }

  public PreparedPacket getTooBigPacket() {
    return this.tooBigPacket;
  }

  public PreparedPacket getInvalidPing() {
    return this.invalidPing;
  }

  public PreparedPacket getInvalidSwitch() {
    return this.invalidSwitch;
  }

  public PreparedPacket getTimeOut(StateRegistry stateRegistry) {
    if (stateRegistry == StateRegistry.CONFIG) {
      return this.configTimeOut;
    }

    return this.playTimeOut;
  }

  public void dispose() {
    this.tooBigPacket.release();
    this.invalidPing.release();
    this.invalidSwitch.release();
    this.playTimeOut.release();
    this.configTimeOut.release();
  }
}
