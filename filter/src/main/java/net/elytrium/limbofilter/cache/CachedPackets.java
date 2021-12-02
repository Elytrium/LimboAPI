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

package net.elytrium.limbofilter.cache;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.packet.Chat;
import com.velocitypowered.proxy.protocol.packet.Disconnect;
import com.velocitypowered.proxy.protocol.packet.title.GenericTitlePacket;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.elytrium.limboapi.api.material.Item;
import net.elytrium.limboapi.api.material.VirtualItem;
import net.elytrium.limboapi.api.protocol.PreparedPacket;
import net.elytrium.limboapi.protocol.packet.PlayerAbilities;
import net.elytrium.limboapi.protocol.packet.SetExp;
import net.elytrium.limboapi.protocol.packet.SetSlot;
import net.elytrium.limboapi.server.world.SimpleItem;
import net.elytrium.limbofilter.FilterPlugin;
import net.elytrium.limbofilter.Settings;
import net.elytrium.limbofilter.handler.BotFilterSessionHandler;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.IntBinaryTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class CachedPackets {

  private PreparedPacket tooBigPacket;
  private PreparedPacket captchaFailed;
  private PreparedPacket fallingCheckFailed;
  private PreparedPacket setSlot;
  private PreparedPacket resetSlot;
  private PreparedPacket checkingChat;
  private PreparedPacket checkingTitle;
  private PreparedPacket kickClientCheckSettings;
  private PreparedPacket kickClientCheckBrand;
  private PreparedPacket successfulBotFilterChat;
  private PreparedPacket successfulBotFilterDisconnect;
  private PreparedPacket noAbilities;
  private List<SetExp> experience;

  public void createPackets() {
    Settings.MAIN.STRINGS strings = Settings.IMP.MAIN.STRINGS;

    this.tooBigPacket = this.prepare((version) -> this.createDisconnectPacket(strings.TOO_BIG_PACKET, version));
    this.captchaFailed = this.prepare((version) -> this.createDisconnectPacket(strings.CAPTCHA_FAILED, version));
    this.fallingCheckFailed = this.prepare((version) -> this.createDisconnectPacket(strings.FALLING_CHECK_FAILED, version));

    this.setSlot = FilterPlugin.getInstance().getFactory().createPreparedPacket()
        .prepare(this.createSetSlotPacket(0, 36, SimpleItem.fromItem(Item.FILLED_MAP), 1, 0, null),
            ProtocolVersion.MINIMUM_VERSION, ProtocolVersion.MINECRAFT_1_16_4)
        .prepare(this.createSetSlotPacket(0, 36, SimpleItem.fromItem(Item.FILLED_MAP), 1, 0,
            CompoundBinaryTag.builder().put("map", IntBinaryTag.of(0)).build()), ProtocolVersion.MINECRAFT_1_17);

    this.resetSlot = this.prepare(this.createSetSlotPacket(0, 36, SimpleItem.fromItem(Item.AIR), 0, 0, null));
    this.checkingChat = this.createChatPacket(strings.CHECKING_CHAT);
    this.checkingTitle = this.createTitlePacket(strings.CHECKING_TITLE, strings.CHECKING_SUBTITLE);

    this.kickClientCheckSettings = this.prepare(version -> this.createDisconnectPacket(strings.KICK_CLIENT_CHECK_SETTINGS, version));
    this.kickClientCheckBrand = this.prepare(version -> this.createDisconnectPacket(strings.KICK_CLIENT_CHECK_BRAND, version));

    this.successfulBotFilterChat = this.createChatPacket(strings.SUCCESSFUL_CRACKED);
    this.successfulBotFilterDisconnect = this.prepare((version) -> this.createDisconnectPacket(strings.SUCCESSFUL_PREMIUM, version));

    this.noAbilities = this.prepare(this.createAbilitiesPacket());
    this.experience = this.createExpPackets();
  }

  private PlayerAbilities createAbilitiesPacket() {
    return new PlayerAbilities((byte) 6, 0f, 0f);
  }

  private List<SetExp> createExpPackets() {
    List<SetExp> packets = new ArrayList<>();
    long ticks = BotFilterSessionHandler.getTotalTicks();
    float expInterval = 0.01F;
    for (int i = 0; i < ticks; ++i) {
      int percentage = (int) (i * 100 / ticks);
      packets.add(new SetExp(percentage * expInterval, percentage, 0));
    }

    return packets;
  }

  @SuppressWarnings("SameParameterValue")
  private SetSlot createSetSlotPacket(int windowId, int slot, VirtualItem item, int count, int data, CompoundBinaryTag nbt) {
    return new SetSlot(windowId, slot, item, count, data, nbt);
  }

  private <T extends MinecraftPacket> PreparedPacket prepare(Function<ProtocolVersion, T> packets) {
    return FilterPlugin.getInstance().getFactory().createPreparedPacket().prepare(packets);
  }

  @SafeVarargs
  private <T extends MinecraftPacket> PreparedPacket prepare(T... packets) {
    PreparedPacket preparedPacket = FilterPlugin.getInstance().getFactory().createPreparedPacket();

    for (T packet : packets) {
      preparedPacket.prepare(packet);
    }

    return preparedPacket;
  }

  public PreparedPacket createChatPacket(String text) {
    return FilterPlugin.getInstance().getFactory().createPreparedPacket()
        .prepare(new Chat(
            ProtocolUtils.getJsonChatSerializer(ProtocolVersion.MINIMUM_VERSION).serialize(
                LegacyComponentSerializer.legacyAmpersand().deserialize(text)
            ), Chat.CHAT_TYPE, null
        ), ProtocolVersion.MINIMUM_VERSION, ProtocolVersion.MINECRAFT_1_15_2)
        .prepare(new Chat(
            ProtocolUtils.getJsonChatSerializer(ProtocolVersion.MINECRAFT_1_16).serialize(
                LegacyComponentSerializer.legacyAmpersand().deserialize(text)
            ), Chat.CHAT_TYPE, null
        ), ProtocolVersion.MINECRAFT_1_16);
  }

  private Disconnect createDisconnectPacket(String message, ProtocolVersion version) {
    Component component = LegacyComponentSerializer.legacyAmpersand().deserialize(message);
    return Disconnect.create(component, version);
  }

  public PreparedPacket createTitlePacket(String title, String subtitle) {
    return this.createTitlePacket(title, subtitle, 10, 50, 10);
  }

  @SuppressWarnings("SameParameterValue")
  private PreparedPacket createTitlePacket(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
    PreparedPacket preparedPacket = FilterPlugin.getInstance().getFactory().createPreparedPacket();

    Component titleComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(title);

    preparedPacket.prepare((Function<ProtocolVersion, GenericTitlePacket>) (version) -> {
      GenericTitlePacket packet = GenericTitlePacket.constructTitlePacket(GenericTitlePacket.ActionType.SET_TITLE, version);
      packet.setComponent(ProtocolUtils.getJsonChatSerializer(version).serialize(titleComponent));
      return packet;
    }, ProtocolVersion.MINECRAFT_1_8);

    if (!subtitle.isEmpty()) {
      Component subtitleComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(subtitle);

      preparedPacket.prepare((Function<ProtocolVersion, GenericTitlePacket>) (version) -> {
        GenericTitlePacket packet = GenericTitlePacket.constructTitlePacket(GenericTitlePacket.ActionType.SET_SUBTITLE, version);
        packet.setComponent(ProtocolUtils.getJsonChatSerializer(version).serialize(subtitleComponent));
        return packet;
      }, ProtocolVersion.MINECRAFT_1_8);
    }

    if (!subtitle.isEmpty() && !title.isEmpty()) {
      preparedPacket.prepare((Function<ProtocolVersion, GenericTitlePacket>) (version) -> {
        GenericTitlePacket packet = GenericTitlePacket.constructTitlePacket(GenericTitlePacket.ActionType.SET_TIMES, version);
        packet.setFadeIn(fadeIn);
        packet.setStay(stay);
        packet.setFadeOut(fadeOut);
        return packet;
      }, ProtocolVersion.MINECRAFT_1_8);
    }

    return preparedPacket;
  }

  public PreparedPacket getTooBigPacket() {
    return this.tooBigPacket;
  }

  public PreparedPacket getCaptchaFailed() {
    return this.captchaFailed;
  }

  public PreparedPacket getFallingCheckFailed() {
    return this.fallingCheckFailed;
  }

  public PreparedPacket getSetSlot() {
    return this.setSlot;
  }

  public PreparedPacket getResetSlot() {
    return this.resetSlot;
  }

  public PreparedPacket getCheckingChat() {
    return this.checkingChat;
  }

  public PreparedPacket getCheckingTitle() {
    return this.checkingTitle;
  }

  public PreparedPacket getKickClientCheckSettings() {
    return this.kickClientCheckSettings;
  }

  public PreparedPacket getKickClientCheckBrand() {
    return this.kickClientCheckBrand;
  }

  public PreparedPacket getSuccessfulBotFilterChat() {
    return this.successfulBotFilterChat;
  }

  public PreparedPacket getSuccessfulBotFilterDisconnect() {
    return this.successfulBotFilterDisconnect;
  }

  public PreparedPacket getNoAbilities() {
    return this.noAbilities;
  }

  public List<SetExp> getExperience() {
    return this.experience;
  }
}
