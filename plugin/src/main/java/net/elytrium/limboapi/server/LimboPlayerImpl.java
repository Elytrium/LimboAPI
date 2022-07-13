/*
 * Copyright (C) 2021 - 2022 Elytrium
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
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.PlayerListItem;
import com.velocitypowered.proxy.protocol.packet.title.GenericTitlePacket;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import net.elytrium.limboapi.LimboAPI;
import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.material.Item;
import net.elytrium.limboapi.api.material.VirtualItem;
import net.elytrium.limboapi.api.player.GameMode;
import net.elytrium.limboapi.api.player.LimboPlayer;
import net.elytrium.limboapi.api.protocol.packets.data.AbilityFlags;
import net.elytrium.limboapi.api.protocol.packets.data.MapData;
import net.elytrium.limboapi.api.protocol.packets.data.MapPalette;
import net.elytrium.limboapi.protocol.packets.s2c.ChangeGameStatePacket;
import net.elytrium.limboapi.protocol.packets.s2c.MapDataPacket;
import net.elytrium.limboapi.protocol.packets.s2c.PlayerAbilitiesPacket;
import net.elytrium.limboapi.protocol.packets.s2c.PositionRotationPacket;
import net.elytrium.limboapi.protocol.packets.s2c.SetSlotPacket;
import net.elytrium.limboapi.protocol.packets.s2c.TimeUpdatePacket;
import net.elytrium.limboapi.server.world.SimpleItem;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.IntBinaryTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;

public class LimboPlayerImpl implements LimboPlayer {

  private final LimboAPI plugin;
  private final LimboImpl server;
  private final ConnectedPlayer player;
  private final MinecraftConnection connection;
  private final ProtocolVersion version;

  private GameMode gameMode = GameMode.ADVENTURE;

  public LimboPlayerImpl(LimboAPI plugin, LimboImpl server, ConnectedPlayer player) {
    this.plugin = plugin;
    this.server = server;
    this.player = player;

    this.connection = this.player.getConnection();
    this.version = this.player.getProtocolVersion();
  }

  @Override
  public void writePacket(Object packetObj) {
    if (packetObj instanceof MinecraftPacket) {
      this.connection.delayedWrite(this.plugin.encodeSingle((MinecraftPacket) packetObj, this.connection.getProtocolVersion()));
    } else {
      this.connection.delayedWrite(packetObj);
    }
  }

  @Override
  public void writePacketAndFlush(Object packetObj) {
    if (packetObj instanceof MinecraftPacket) {
      this.connection.write(this.plugin.encodeSingle((MinecraftPacket) packetObj, this.connection.getProtocolVersion()));
    } else {
      this.connection.write(packetObj);
    }
  }

  @Override
  public void flushPackets() {
    this.connection.flush();
  }

  @Override
  public void closeWith(Object packetObj) {
    this.connection.closeWith(packetObj);
  }

  @Override
  public ScheduledExecutorService getScheduledExecutor() {
    return this.connection.eventLoop();
  }

  @Override
  public void sendImage(BufferedImage image) {
    this.sendImage(0, image, true, true);
  }

  @Override
  public void sendImage(BufferedImage image, boolean sendItem) {
    this.sendImage(0, image, sendItem, true);
  }

  @Override
  public void sendImage(int mapID, BufferedImage image) {
    this.sendImage(mapID, image, true, true);
  }

  @Override
  public void sendImage(int mapID, BufferedImage image, boolean sendItem) {
    this.sendImage(mapID, image, sendItem, true);
  }

  @Override
  public void sendImage(int mapID, BufferedImage image, boolean sendItem, boolean resize) {
    if (sendItem) {
      this.setInventory(
          36,
          SimpleItem.fromItem(Item.FILLED_MAP),
          1,
          mapID,
          this.version.compareTo(ProtocolVersion.MINECRAFT_1_17) < 0
              ? null
              : CompoundBinaryTag.builder().put("map", IntBinaryTag.of(mapID)).build()
      );
    }

    if (image.getWidth() != MapData.MAP_DIM_SIZE || image.getHeight() != MapData.MAP_DIM_SIZE) {
      if (resize) {
        BufferedImage resizedImage = new BufferedImage(MapData.MAP_DIM_SIZE, MapData.MAP_DIM_SIZE, image.getType());

        Graphics2D graphics = resizedImage.createGraphics();
        graphics.drawImage(image.getScaledInstance(MapData.MAP_DIM_SIZE, MapData.MAP_DIM_SIZE, Image.SCALE_SMOOTH), 0, 0, null);
        graphics.dispose();

        image = resizedImage;
      } else {
        throw new IllegalStateException(
            "You either need to provide an image of " + MapData.MAP_DIM_SIZE + "x" + MapData.MAP_DIM_SIZE
                + " pixels or set the resize parameter to true so that API will automatically resize your image."
        );
      }
    }

    int[] toWrite = MapPalette.imageToBytes(image, this.version);
    if (this.version.compareTo(ProtocolVersion.MINECRAFT_1_8) < 0) {
      byte[][] canvas = new byte[MapData.MAP_DIM_SIZE][MapData.MAP_DIM_SIZE];
      for (int i = 0; i < MapData.MAP_SIZE; ++i) {
        canvas[i & 127][i >> 7] = (byte) toWrite[i];
      }

      for (int i = 0; i < MapData.MAP_DIM_SIZE; ++i) {
        this.writePacket(new MapDataPacket(mapID, (byte) 0, new MapData(i, canvas[i])));
      }

      this.flushPackets();
    } else {
      byte[] canvas = new byte[MapData.MAP_SIZE];
      for (int i = 0; i < MapData.MAP_SIZE; ++i) {
        canvas[i] = (byte) toWrite[i];
      }

      this.writePacketAndFlush(new MapDataPacket(mapID, (byte) 0, new MapData(canvas)));
    }
  }

  @Override
  public void setInventory(VirtualItem item, int count) {
    this.writePacketAndFlush(new SetSlotPacket(0, 36, item, count, 0, null));
  }

  @Override
  public void setInventory(VirtualItem item, int slot, int count) {
    this.writePacketAndFlush(new SetSlotPacket(0, slot, item, count, 0, null));
  }

  @Override
  public void setInventory(int slot, VirtualItem item, int count, int data, CompoundBinaryTag nbt) {
    this.writePacketAndFlush(new SetSlotPacket(0, slot, item, count, data, nbt));
  }

  @Override
  public void setGameMode(GameMode gameMode) {
    boolean is17 = this.version.compareTo(ProtocolVersion.MINECRAFT_1_8) < 0;
    if (gameMode != GameMode.SPECTATOR || !is17) { // Spectator game mode was added in 1.8.
      this.gameMode = gameMode;

      int id = this.gameMode.getID();
      this.sendAbilities();
      if (!is17) {
        this.writePacket(new PlayerListItem(PlayerListItem.UPDATE_GAMEMODE, List.of(new PlayerListItem.Item(this.player.getUniqueId()).setGameMode(id))));
      }
      this.writePacket(new ChangeGameStatePacket(3, id));

      this.flushPackets();
    }
  }

  @Override
  public void teleport(double posX, double posY, double posZ, float yaw, float pitch) {
    this.writePacketAndFlush(new PositionRotationPacket(posX, posY, posZ, yaw, pitch, false, 44, true));
  }

  /**
   * @deprecated Use {@link Player#showTitle(Title)}
   */
  @Override
  @Deprecated
  public void sendTitle(Component title, Component subtitle, ProtocolVersion version, int fadeIn, int stay, int fadeOut) {
    {
      GenericTitlePacket packet = GenericTitlePacket.constructTitlePacket(GenericTitlePacket.ActionType.SET_TITLE, version);

      packet.setComponent(ProtocolUtils.getJsonChatSerializer(version).serialize(title));
      this.writePacketAndFlush(packet);
    }
    {
      GenericTitlePacket packet = GenericTitlePacket.constructTitlePacket(GenericTitlePacket.ActionType.SET_SUBTITLE, version);

      packet.setComponent(ProtocolUtils.getJsonChatSerializer(version).serialize(subtitle));
      this.writePacketAndFlush(packet);
    }
    {
      GenericTitlePacket packet = GenericTitlePacket.constructTitlePacket(GenericTitlePacket.ActionType.SET_TIMES, version);

      packet.setFadeIn(fadeIn);
      packet.setStay(stay);
      packet.setFadeOut(fadeOut);
      this.writePacketAndFlush(packet);
    }
  }

  @Override
  public void disableFalling() {
    this.writePacketAndFlush(new PlayerAbilitiesPacket((byte) (this.getAbilities() | AbilityFlags.FLYING | AbilityFlags.ALLOW_FLYING), 0F, 0F));
  }

  @Override
  public void disconnect() {
    LimboSessionHandlerImpl handler = (LimboSessionHandlerImpl) this.connection.getSessionHandler();
    if (handler != null) {
      handler.disconnected();

      if (this.plugin.hasLoginQueue(this.player)) {
        this.plugin.getLoginQueue(this.player).next();
      } else {
        RegisteredServer server = handler.getPreviousServer();
        if (server != null) {
          this.deject();
          this.sendToRegisteredServer(server);
        }
      }
    }
  }

  @Override
  public void disconnect(RegisteredServer server) {
    LimboSessionHandlerImpl handler = (LimboSessionHandlerImpl) this.connection.getSessionHandler();
    if (handler != null) {
      handler.disconnected();

      if (this.plugin.hasLoginQueue(this.player)) {
        this.plugin.setNextServer(this.player, server);
      } else {
        this.deject();
        this.sendToRegisteredServer(server);
      }
    }
  }

  private void deject() {
    this.plugin.fixCompressor(this.connection.getChannel().pipeline(), this.version);
    this.plugin.deject3rdParty(this.connection.getChannel().pipeline());
  }

  private void sendToRegisteredServer(RegisteredServer server) {
    this.connection.eventLoop().execute(() -> {
      this.connection.setState(StateRegistry.PLAY);
      this.player.createConnectionRequest(server).fireAndForget();
    });
  }

  @Override
  public void sendAbilities() {
    this.writePacketAndFlush(new PlayerAbilitiesPacket(this.getAbilities(), 0.05F, 0.1F));
  }

  @Override
  public void sendAbilities(byte abilities, float flySpeed, float walkSpeed) {
    this.writePacketAndFlush(new PlayerAbilitiesPacket(abilities, flySpeed, walkSpeed));
  }

  @Override
  public byte getAbilities() {
    switch (this.gameMode) {
      case CREATIVE: {
        return AbilityFlags.ALLOW_FLYING | AbilityFlags.CREATIVE_MODE | AbilityFlags.INVULNERABLE;
      }
      case SPECTATOR: {
        return AbilityFlags.ALLOW_FLYING | AbilityFlags.INVULNERABLE | AbilityFlags.FLYING;
      }
      default: {
        return 0;
      }
    }
  }

  @Override
  public GameMode getGameMode() {
    return this.gameMode;
  }

  @Override
  public Limbo getServer() {
    return this.server;
  }

  @Override
  public Player getProxyPlayer() {
    return this.player;
  }

  @Override
  public int getPing() {
    LimboSessionHandlerImpl handler = (LimboSessionHandlerImpl) this.connection.getSessionHandler();
    if (handler != null) {
      return handler.getPing();
    } else {
      return -1;
    }
  }

  @Override
  public void setWorldTime(long ticks) {
    this.writePacketAndFlush(new TimeUpdatePacket(ticks, ticks));
  }
}
