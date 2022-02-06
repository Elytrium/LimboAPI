package net.elytrium.limboapi.injection.packet;

import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.backend.BackendPlaySessionHandler;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.packet.PlayerListItem;
import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;
import net.elytrium.limboapi.LimboAPI;

public class PlayerListItemHook extends PlayerListItem {
  private static Field serverConnField;

  static {
    try {
      serverConnField = BackendPlaySessionHandler.class.getDeclaredField("serverConn");
      serverConnField.setAccessible(true);
    } catch (NoSuchFieldException e) {
      e.printStackTrace();
    }
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    if (handler instanceof BackendPlaySessionHandler) {
      try {
        List<Item> items = this.getItems();
        for (int i = 0; i < items.size(); i++) {
          Item item = items.get(i);
          ConnectedPlayer player = ((VelocityServerConnection) serverConnField.get(handler)).getPlayer();
          UUID initialUUID = LimboAPI.getInstance().getInitialUUID(player);

          if (initialUUID.equals(item.getUuid())) {
            items.set(i, new Item(player.getUniqueId())
                .setDisplayName(item.getDisplayName())
                .setGameMode(item.getGameMode())
                .setLatency(item.getLatency())
                .setName(item.getName())
                .setProperties(item.getProperties()));
          }
        }
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      }
    }

    return super.handle(handler);
  }
}
