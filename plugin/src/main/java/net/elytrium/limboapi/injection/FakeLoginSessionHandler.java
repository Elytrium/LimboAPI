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
 *
 * This file contains some parts of Velocity, licensed under the AGPLv3 License (AGPLv3).
 *
 * Copyright (C) 2018 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.elytrium.limboapi.injection;

import static com.google.common.net.UrlEscapers.urlFormParameterEscaper;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_8;
import static com.velocitypowered.proxy.VelocityServer.GENERAL_GSON;
import static com.velocitypowered.proxy.connection.VelocityConstants.EMPTY_BYTE_ARRAY;
import static com.velocitypowered.proxy.util.EncryptionUtils.decryptRsa;
import static com.velocitypowered.proxy.util.EncryptionUtils.generateServerId;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.DisconnectEvent.LoginStatus;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent.PreLoginComponentResult;
import com.velocitypowered.api.event.permission.PermissionsSetupEvent;
import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.permission.PermissionFunction;
import com.velocitypowered.api.permission.PermissionProvider;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.api.util.UuidUtils;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.config.PlayerInfoForwarding;
import com.velocitypowered.proxy.config.VelocityConfiguration;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.connection.client.InitialConnectSessionHandler;
import com.velocitypowered.proxy.connection.client.InitialInboundConnection;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.EncryptionRequest;
import com.velocitypowered.proxy.protocol.packet.EncryptionResponse;
import com.velocitypowered.proxy.protocol.packet.ServerLogin;
import com.velocitypowered.proxy.protocol.packet.ServerLoginSuccess;
import com.velocitypowered.proxy.protocol.packet.SetCompression;
import io.netty.buffer.ByteBuf;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import lombok.SneakyThrows;
import net.elytrium.limboapi.api.event.LoginLimboRegisterEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asynchttpclient.ListenableFuture;
import org.asynchttpclient.Response;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

public class FakeLoginSessionHandler implements MinecraftSessionHandler {

  private static final Logger logger = LogManager.getLogger(FakeLoginSessionHandler.class);
  private static final String MOJANG_HASJOINED_URL =
      "https://sessionserver.mojang.com/session/minecraft/hasJoined?username=%s&serverId=%s";

  private final VelocityServer server;
  private final MinecraftConnection mcConnection;
  private final InitialInboundConnection inbound;
  private @MonotonicNonNull ServerLogin login;
  private byte[] verify = EMPTY_BYTE_ARRAY;
  private @MonotonicNonNull ConnectedPlayer connectedPlayer;

  FakeLoginSessionHandler(VelocityServer server, MinecraftConnection mcConnection,
                          InitialInboundConnection inbound) {
    this.server = Preconditions.checkNotNull(server, "server");
    this.mcConnection = Preconditions.checkNotNull(mcConnection, "mcConnection");
    this.inbound = Preconditions.checkNotNull(inbound, "inbound");
  }

  @Override
  public boolean handle(ServerLogin packet) {
    this.login = packet;
    beginPreLogin();
    return true;
  }

  @Override
  public boolean handle(EncryptionResponse packet) {
    ServerLogin login = this.login;
    if (login == null) {
      throw new IllegalStateException("No ServerLogin packet received yet.");
    }

    if (verify.length == 0) {
      throw new IllegalStateException("No EncryptionRequest packet sent yet.");
    }

    try {
      KeyPair serverKeyPair = server.getServerKeyPair();
      byte[] decryptedVerifyToken = decryptRsa(serverKeyPair, packet.getVerifyToken());
      if (!MessageDigest.isEqual(verify, decryptedVerifyToken)) {
        throw new IllegalStateException("Unable to successfully decrypt the verification token.");
      }

      byte[] decryptedSharedSecret = decryptRsa(serverKeyPair, packet.getSharedSecret());
      String serverId = generateServerId(decryptedSharedSecret, serverKeyPair.getPublic());

      String playerIp = ((InetSocketAddress) mcConnection.getRemoteAddress()).getHostString();
      String url = String.format(MOJANG_HASJOINED_URL,
          urlFormParameterEscaper().escape(login.getUsername()), serverId);

      if (server.getConfiguration().shouldPreventClientProxyConnections()) {
        url += "&ip=" + urlFormParameterEscaper().escape(playerIp);
      }

      ListenableFuture<Response> hasJoinedResponse = server.getAsyncHttpClient().prepareGet(url)
          .execute();
      hasJoinedResponse.addListener(() -> {
        if (mcConnection.isClosed()) {
          // The player disconnected after we authenticated them.
          return;
        }

        // Go ahead and enable encryption. Once the client sends EncryptionResponse, encryption
        // is enabled.
        try {
          mcConnection.enableEncryption(decryptedSharedSecret);
        } catch (GeneralSecurityException e) {
          throw new RuntimeException(e);
        }

        try {
          Response profileResponse = hasJoinedResponse.get();
          if (profileResponse.getStatusCode() == 200) {
            // All went well, initialize the session.
            initializePlayer(GENERAL_GSON.fromJson(profileResponse.getResponseBody(),
                GameProfile.class), true);
          } else if (profileResponse.getStatusCode() == 204) {
            // Apparently an offline-mode user logged onto this online-mode proxy.
            inbound.disconnect(Component.translatable("velocity.error.online-mode-only",
                NamedTextColor.RED));
          } else {
            // Something else went wrong
            logger.error(
                "Got an unexpected error code {} whilst contacting Mojang to log in {} ({})",
                profileResponse.getStatusCode(), login.getUsername(), playerIp);
            mcConnection.close(true);
          }
        } catch (ExecutionException e) {
          logger.error("Unable to authenticate with Mojang", e);
          mcConnection.close(true);
        } catch (InterruptedException e) {
          // not much we can do usefully
          Thread.currentThread().interrupt();
        }
      }, mcConnection.eventLoop());
    } catch (GeneralSecurityException e) {
      logger.error("Unable to enable encryption", e);
      mcConnection.close(true);
    }
    return true;
  }

  private void beginPreLogin() {
    ServerLogin login = this.login;
    if (login == null) {
      throw new IllegalStateException("No ServerLogin packet received yet.");
    }

    PreLoginEvent event = new PreLoginEvent(inbound, login.getUsername());
    server.getEventManager().fire(event)
        .thenRunAsync(() -> {
          if (mcConnection.isClosed()) {
            // The player was disconnected
            return;
          }

          PreLoginComponentResult result = event.getResult();
          Optional<Component> disconnectReason = result.getReasonComponent();
          if (disconnectReason.isPresent()) {
            // The component is guaranteed to be provided if the connection was denied.
            inbound.disconnect(disconnectReason.get());
            return;
          }

          if (!result.isForceOfflineMode()
              && (server.getConfiguration().isOnlineMode() || result.isOnlineModeAllowed())) {
            requestEncryption();
          } else {
            initializeOfflineMode();
          }
        }, mcConnection.eventLoop())
        .exceptionally((ex) -> {
          logger.error("Exception in pre-login stage", ex);
          return null;
        });
  }

  private void requestEncryption() {
    EncryptionRequest request = generateEncryptionRequest();
    this.verify = Arrays.copyOf(request.getVerifyToken(), 4);
    mcConnection.write(request);
  }

  private void initializeOfflineMode() {
    initializePlayer(GameProfile.forOfflinePlayer(login.getUsername()), false);
  }

  private EncryptionRequest generateEncryptionRequest() {
    byte[] verify = new byte[4];
    ThreadLocalRandom.current().nextBytes(verify);

    EncryptionRequest request = new EncryptionRequest();
    request.setPublicKey(server.getServerKeyPair().getPublic().getEncoded());
    request.setVerifyToken(verify);
    return request;
  }

  private void initializePlayer(GameProfile profile, boolean onlineMode) {
    // Some connection types may need to alter the game profile.
    profile = mcConnection.getType().addGameProfileTokensIfRequired(profile,
        server.getConfiguration().getPlayerInfoForwardingMode());
    GameProfileRequestEvent profileRequestEvent = new GameProfileRequestEvent(inbound, profile,
        onlineMode);
    final GameProfile finalProfile = profile;

    server.getEventManager().fire(profileRequestEvent).thenComposeAsync(profileEvent -> {
      if (mcConnection.isClosed()) {
        // The player disconnected while event execution
        return CompletableFuture.completedFuture(null);
      }

      // Initiate a regular connection and move over to it.
      try {
        Constructor<ConnectedPlayer> ctor = ConnectedPlayer.class.getDeclaredConstructor(
            VelocityServer.class, GameProfile.class, MinecraftConnection.class, InetSocketAddress.class, boolean.class);

        ctor.setAccessible(true);
        ConnectedPlayer player = ctor.newInstance(server, profileEvent.getGameProfile(),
            mcConnection, inbound.getVirtualHost().orElse(null), onlineMode);
        this.connectedPlayer = player;
        if (!server.canRegisterConnection(player)) {
          player.disconnect0(
              Component.translatable("velocity.error.already-connected-proxy", NamedTextColor.RED), true);
          return CompletableFuture.completedFuture(null);
        }

        logger.info("{} has connected", player);

      return server.getEventManager()
          .fire(new LoginLimboRegisterEvent(player))
          .thenComposeAsync(event -> CompletableFuture
              .allOf(event.getCallbacks().toArray(new CompletableFuture[0]))
              .thenAcceptAsync(unused -> initialize(player)), mcConnection.eventLoop());
      } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
        e.printStackTrace();
      }

      return CompletableFuture.completedFuture(null);
    }, mcConnection.eventLoop()).exceptionally((ex) -> {
      logger.error("Exception during connection of {}", finalProfile, ex);
      return null;
    });
  }

  /*
  private void completeLoginProtocolPhaseAndInitialize(ConnectedPlayer player) {
    int threshold = server.getConfiguration().getCompressionThreshold();
    if (threshold >= 0 && mcConnection.getProtocolVersion().compareTo(MINECRAFT_1_8) >= 0) {
      mcConnection.write(new SetCompression(threshold));
      mcConnection.setCompressionThreshold(threshold);
    }
    VelocityConfiguration configuration = server.getConfiguration();
    UUID playerUniqueId = player.getUniqueId();
    if (configuration.getPlayerInfoForwardingMode() == PlayerInfoForwarding.NONE) {
      playerUniqueId = UuidUtils.generateOfflinePlayerUuid(player.getUsername());
    }
    ServerLoginSuccess success = new ServerLoginSuccess();
    success.setUsername(player.getUsername());
    success.setUuid(playerUniqueId);
    mcConnection.write(success);

    mcConnection.setAssociation(player);
    mcConnection.setState(StateRegistry.PLAY);

    server.getEventManager().fire(new LoginEvent(player))
        .thenAcceptAsync(event -> {
          if (mcConnection.isClosed()) {
            // The player was disconnected
            server.getEventManager().fireAndForget(new DisconnectEvent(player,
                LoginStatus.CANCELLED_BY_USER_BEFORE_COMPLETE));
            return;
          }

          Optional<Component> reason = event.getResult().getReasonComponent();
          if (reason.isPresent()) {
            player.disconnect0(reason.get(), true);
          } else {
            if (!server.registerConnection(player)) {
              player.disconnect0(Component.translatable("velocity.error.already-connected-proxy"),
                  true);
              return;
            }

            mcConnection.setSessionHandler(new InitialConnectSessionHandler(player));
            server.getEventManager().fire(new PostLoginEvent(player))
                .thenCompose((ignored) -> connectToInitialServer(player))
                .exceptionally((ex) -> {
                  logger.error("Exception while connecting {} to initial server", player, ex);
                  return null;
                });
          }
        }, mcConnection.eventLoop())
        .exceptionally((ex) -> {
          logger.error("Exception while completing login initialisation phase for {}", player, ex);
          return null;
        });
  }
  */

  public void completeLoginProtocol(ConnectedPlayer player, boolean flush) {
    int threshold = server.getConfiguration().getCompressionThreshold();
    if (threshold >= 0 && mcConnection.getProtocolVersion().compareTo(MINECRAFT_1_8) >= 0) {
      mcConnection./*write*/delayedWrite(new SetCompression(threshold));
      mcConnection.setCompressionThreshold(threshold);
    }
    VelocityConfiguration configuration = server.getConfiguration();
    UUID playerUniqueId = player.getUniqueId();
    if (configuration.getPlayerInfoForwardingMode() == PlayerInfoForwarding.NONE) {
      playerUniqueId = UuidUtils.generateOfflinePlayerUuid(player.getUsername());
    }
    ServerLoginSuccess success = new ServerLoginSuccess();
    success.setUsername(player.getUsername());
    success.setUuid(playerUniqueId);
    mcConnection./*write*/delayedWrite(success);

    if (flush) {
      mcConnection.flush();
    }
  }

  public void initialize(ConnectedPlayer player) {
    mcConnection.setAssociation(player);
    mcConnection.setState(StateRegistry.PLAY);

    try {
      Field defaultPermissions = ConnectedPlayer.class.getDeclaredField("DEFAULT_PERMISSIONS");
      defaultPermissions.setAccessible(true);

      server.getEventManager()
          .fire(new PermissionsSetupEvent(player, (PermissionProvider) defaultPermissions.get(null)))
          .thenComposeAsync(premissionEvent -> {

            if (mcConnection.isClosed()) {
              return CompletableFuture.completedFuture(null);
            }

            // wait for permissions to load, then set the players permission function
            final PermissionFunction function = premissionEvent.createFunction(player);
            if (function == null) {
              logger.error(
                  "A plugin permission provider {} provided an invalid permission function"
                      + " for player {}. This is a bug in the plugin, not in Velocity. Falling"
                      + " back to the default permission function.",
                  premissionEvent.getProvider().getClass().getName(),
                  player.getUsername());
            } else {
              try {
                Method setPermissionFunction = ConnectedPlayer.class.getDeclaredMethod("setPermissionFunction", PermissionFunction.class);
                setPermissionFunction.setAccessible(true);
                setPermissionFunction.invoke(player, function);
              } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
              }
            }

            return server.getEventManager().fire(new LoginEvent(player))
                .thenAcceptAsync(event -> {
                  if (mcConnection.isClosed()) {
                    // The player was disconnected
                    server.getEventManager().fireAndForget(new DisconnectEvent(player,
                        LoginStatus.CANCELLED_BY_USER_BEFORE_COMPLETE));
                    return;
                  }

                  Optional<Component> reason = event.getResult().getReasonComponent();
                  if (reason.isPresent()) {
                    player.disconnect0(reason.get(), true);
                  } else {
                    if (!server.registerConnection(player)) {
                      player.disconnect0(Component.translatable("velocity.error.already-connected-proxy"), true);
                      return;
                    }

                    try {
                      Constructor<InitialConnectSessionHandler> initialCtor
                          = InitialConnectSessionHandler.class.getDeclaredConstructor(ConnectedPlayer.class);
                      initialCtor.setAccessible(true);
                      mcConnection.setSessionHandler(initialCtor.newInstance(player));
                    } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                      e.printStackTrace();
                    }
                    server.getEventManager().fire(new PostLoginEvent(player))
                        .thenCompose((ignored) -> connectToInitialServer(player))
                        .exceptionally((ex) -> {
                          logger.error("Exception while connecting {} to initial server", player, ex);
                          return null;
                        });
                  }
                }, mcConnection.eventLoop());
          }, mcConnection.eventLoop())
          .exceptionally((ex) -> {
            logger.error("Exception while completing login initialisation phase for {}", player, ex);
            return null;
          });
    } catch (IllegalAccessException | NoSuchFieldException e) {
      e.printStackTrace();
    }
  }

  private CompletableFuture<Void> connectToInitialServer(ConnectedPlayer player) {
    Optional<RegisteredServer> initialFromConfig = player.getNextServerToTry();
    PlayerChooseInitialServerEvent event = new PlayerChooseInitialServerEvent(player,
        initialFromConfig.orElse(null));

    return server.getEventManager().fire(event)
        .thenRunAsync(() -> {
          Optional<RegisteredServer> toTry = event.getInitialServer();
          if (toTry.isEmpty()) {
            player.disconnect0(Component.translatable("velocity.error.no-available-servers",
                NamedTextColor.RED), true);
            return;
          }
          player.createConnectionRequest(toTry.get()).connect();
        }, mcConnection.eventLoop());
  }

  @Override
  public void handleUnknown(ByteBuf buf) {
    mcConnection.close(true);
  }

  @SneakyThrows
  @Override
  public void disconnected() {
    if (connectedPlayer != null) {
      Method teardown = ConnectedPlayer.class.getDeclaredMethod("teardown");
      teardown.setAccessible(true);
      teardown.invoke(connectedPlayer);
    }
  }
}
