package net.elytrium.limboapi.injection;

import com.velocitypowered.api.event.connection.DisconnectEvent;
import lombok.RequiredArgsConstructor;
import net.elytrium.limboapi.LimboAPI;

@RequiredArgsConstructor
public class DisconnectListener {
  private final LimboAPI limboAPI;

  public void onDisconnect(DisconnectEvent e) {
    limboAPI.unsetVirtualServerJoined(e.getPlayer());
  }
}
