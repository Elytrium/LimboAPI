package net.elytrium.limboapi.injection;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.ConnectionHandshakeEvent;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.InitialInboundConnection;
import com.velocitypowered.proxy.connection.client.LoginSessionHandler;
import java.lang.reflect.Field;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import net.elytrium.limboapi.LimboAPI;

@RequiredArgsConstructor
public class HandshakeListener {
  private final LimboAPI limboAPI;

  @SneakyThrows
  @Subscribe
  public void onHandshakeFinished(ConnectionHandshakeEvent e) {
    Field connectionField = InitialInboundConnection.class.getDeclaredField("connection");
    connectionField.setAccessible(true);

    // TODO: Better injection
    new Thread(() -> {
      try {
        MinecraftConnection connection = (MinecraftConnection) connectionField.get(e.getConnection());
        //noinspection StatementWithEmptyBody
        while (!(connection.getSessionHandler() instanceof LoginSessionHandler)) {
          // busy wait.
        }

        connection.setSessionHandler(
            new FakeLoginSessionHandler(limboAPI.getServer(), connection, (InitialInboundConnection) e.getConnection()));
      } catch (IllegalAccessException ex) {
        ex.printStackTrace();
      }
    }).start();
  }
}
