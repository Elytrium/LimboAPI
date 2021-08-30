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
import lombok.Getter;
import net.elytrium.limboapi.api.chunk.VirtualChunk;
import net.elytrium.limboapi.api.material.Item;
import net.elytrium.limboapi.api.material.VirtualItem;
import net.elytrium.limboapi.api.protocol.PreparedPacket;
import net.elytrium.limboapi.protocol.packet.PlayerAbilities;
import net.elytrium.limboapi.protocol.packet.PlayerPositionAndLook;
import net.elytrium.limboapi.protocol.packet.SetExp;
import net.elytrium.limboapi.protocol.packet.SetSlot;
import net.elytrium.limboapi.protocol.packet.UpdateViewPosition;
import net.elytrium.limboapi.protocol.packet.world.ChunkData;
import net.elytrium.limboapi.server.world.SimpleItem;
import net.elytrium.limbofilter.FilterPlugin;
import net.elytrium.limbofilter.config.Settings;
import net.elytrium.limbofilter.handler.BotFilterSessionHandler;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.IntBinaryTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

@Getter
public class CachedPackets {

  private PreparedPacket alreadyConnected;
  private PreparedPacket tooBigPacket;
  private PreparedPacket captchaFailed;
  private PreparedPacket fallingCheckFailed;
  private PreparedPacket setSlot;
  private PreparedPacket resetSlot;
  private PreparedPacket checkingChat;
  private PreparedPacket checkingCaptchaChat;
  private PreparedPacket kickClientCheckSettings;
  private PreparedPacket kickClientCheckSettingsChat;
  private PreparedPacket kickClientCheckSettingsSkin;
  private PreparedPacket kickClientCheckBrand;
  private PreparedPacket successfulBotFilterChat;
  private PreparedPacket successfulBotFilterDisconnect;
  private PreparedPacket noAbilities;
  private PreparedPacket antiBotTitle;
  private List<SetExp> experience;

  public void createPackets() {
    experience = createExpPackets();

    noAbilities = prepare(createAbilitiesPacket());
    alreadyConnected = prepare((version) ->
        createDisconnectPacket(Settings.IMP.MAIN.STRINGS.ALREADY_CONNECTED, version));
    tooBigPacket = prepare((version) ->
        createDisconnectPacket(Settings.IMP.MAIN.STRINGS.TOO_BIG_PACKET, version));
    captchaFailed = prepare((version) ->
        createDisconnectPacket(Settings.IMP.MAIN.STRINGS.CAPTCHA_FAILED, version));
    fallingCheckFailed = prepare((version) ->
        createDisconnectPacket(Settings.IMP.MAIN.STRINGS.FALLING_CHECK_FAILED, version));

    setSlot = FilterPlugin.getInstance().getFactory().createPreparedPacket()
        .prepare(createSetSlotPacket(0, 36, SimpleItem.fromItem(Item.FILLED_MAP), 1, 0, null),
            ProtocolVersion.MINIMUM_VERSION, ProtocolVersion.MINECRAFT_1_16_4)
        .prepare(createSetSlotPacket(0, 36, SimpleItem.fromItem(Item.FILLED_MAP), 1, 0,
            CompoundBinaryTag.builder().put("map", IntBinaryTag.of(0)).build()), ProtocolVersion.MINECRAFT_1_17);

    resetSlot = prepare(createSetSlotPacket(0, 36, SimpleItem.fromItem(Item.AIR), 0, 0, null));
    checkingChat = createChatPacket(Settings.IMP.MAIN.STRINGS.CHECKING);
    checkingCaptchaChat = createChatPacket(Settings.IMP.MAIN.STRINGS.CHECKING_CAPTCHA);
    successfulBotFilterChat = createChatPacket(Settings.IMP.MAIN.STRINGS.SUCCESSFUL_CRACKED);
    successfulBotFilterDisconnect = prepare((version) ->
        createDisconnectPacket(Settings.IMP.MAIN.STRINGS.SUCCESSFUL_PREMIUM, version));
    antiBotTitle = createTitlePacket(
        Settings.IMP.MAIN.BRAND,
        Settings.IMP.MAIN.STRINGS.CHECKING_CAPTCHA,
        10, 50, 10);

    kickClientCheckSettings = prepare(version ->
        createDisconnectPacket(Settings.IMP.MAIN.STRINGS.KICK_CLIENT_CHECK_SETTINGS, version));
    kickClientCheckSettingsChat = prepare(version ->
        createDisconnectPacket(Settings.IMP.MAIN.STRINGS.KICK_CLIENT_CHECK_SETTINGS_CHAT_COLOR, version));
    kickClientCheckSettingsSkin = prepare(version ->
        createDisconnectPacket(Settings.IMP.MAIN.STRINGS.KICK_CLIENT_CHECK_SETTINGS_SKIN_PARTS, version));
    kickClientCheckBrand = prepare(version ->
        createDisconnectPacket(Settings.IMP.MAIN.STRINGS.KICK_CLIENT_CHECK_BRAND, version));
  }

  private PlayerAbilities createAbilitiesPacket() {
    return new PlayerAbilities((byte) 6, 0f, 0f);
  }

  public PlayerPositionAndLook createPlayerPosAndLookPacket(double x, double y, double z, float yaw, float pitch) {
    return new PlayerPositionAndLook(x, y, z, yaw, pitch, -133, false, true);
  }

  private List<SetExp> createExpPackets() {
    List<SetExp> packets = new ArrayList<>();
    long ticks = BotFilterSessionHandler.TOTAL_TICKS;
    float expInterval = 0.01f;
    for (int i = 0; i < ticks; ++i) {
      int percentage = (int) (i * 100 / ticks);
      packets.add(new SetExp(percentage * expInterval, percentage, 0));
    }
    return packets;
  }

  public ChunkData createChunkDataPacket(VirtualChunk chunk, int skyLightY) {
    chunk.setSkyLight(chunk.getX() % 16, skyLightY, chunk.getZ() % 16, (byte) 1);
    return new ChunkData(chunk.getFullChunkSnapshot(), true);
  }

  public UpdateViewPosition createUpdateViewPosition(int x, int z) {
    return new UpdateViewPosition(x >> 4, z >> 4);
  }

  private SetSlot createSetSlotPacket(
      int windowId, int slot, VirtualItem item, int count, int data, CompoundBinaryTag nbt) {
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

  private PreparedPacket createChatPacket(String text) {
    return FilterPlugin.getInstance().getFactory().createPreparedPacket()
        .prepare(new Chat(
            ProtocolUtils.getJsonChatSerializer(ProtocolVersion.MINIMUM_VERSION).serialize(
                LegacyComponentSerializer
                    .legacyAmpersand()
                    .deserialize(text)
            ), Chat.CHAT_TYPE, null
        ), ProtocolVersion.MINIMUM_VERSION, ProtocolVersion.MINECRAFT_1_15_2)
        .prepare(new Chat(
            ProtocolUtils.getJsonChatSerializer(ProtocolVersion.MINECRAFT_1_16).serialize(
                LegacyComponentSerializer
                    .legacyAmpersand()
                    .deserialize(text)
            ), Chat.CHAT_TYPE, null
        ), ProtocolVersion.MINECRAFT_1_16);
  }

  private Disconnect createDisconnectPacket(String message, ProtocolVersion version) {
    Component component = LegacyComponentSerializer.legacyAmpersand().deserialize(message);
    return Disconnect.create(component, version);
  }

  private PreparedPacket createTitlePacket(
      String title, String subtitle, int fadeIn, int stay, int fadeOut) {

    PreparedPacket preparedPacket = FilterPlugin.getInstance().getFactory().createPreparedPacket();

    Component titleComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(title);
    Component subtitleComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(subtitle);

    preparedPacket.prepare((Function<ProtocolVersion, GenericTitlePacket>) (version) -> {
      GenericTitlePacket packet = GenericTitlePacket
          .constructTitlePacket(GenericTitlePacket.ActionType.SET_TITLE, version);
      packet.setComponent(ProtocolUtils.getJsonChatSerializer(version).serialize(titleComponent));
      return packet;
    }, ProtocolVersion.MINECRAFT_1_8);

    preparedPacket.prepare((Function<ProtocolVersion, GenericTitlePacket>) (version) -> {
      GenericTitlePacket packet = GenericTitlePacket
          .constructTitlePacket(GenericTitlePacket.ActionType.SET_SUBTITLE, version);
      packet.setComponent(ProtocolUtils.getJsonChatSerializer(version).serialize(subtitleComponent));
      return packet;
    }, ProtocolVersion.MINECRAFT_1_8);

    preparedPacket.prepare((Function<ProtocolVersion, GenericTitlePacket>) (version) -> {
      GenericTitlePacket packet = GenericTitlePacket
          .constructTitlePacket(GenericTitlePacket.ActionType.SET_TIMES, version);
      packet.setFadeIn(fadeIn);
      packet.setStay(stay);
      packet.setFadeOut(fadeOut);
      return packet;
    }, ProtocolVersion.MINECRAFT_1_8);

    return preparedPacket;
  }
}
