package net.elytrium.limboapi.injection.packet;

import com.velocitypowered.proxy.protocol.packet.ServerLoginSuccess;
import net.elytrium.fastprepare.dummy.DummyPacket;

public class ServerLoginSuccessHook extends ServerLoginSuccess implements DummyPacket {
}
