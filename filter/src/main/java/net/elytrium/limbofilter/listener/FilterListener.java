package net.elytrium.limbofilter.listener;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import java.util.concurrent.CompletableFuture;
import net.elytrium.limboapi.api.event.LoginLimboRegisterEvent;
import net.elytrium.limbofilter.FilterPlugin;

public class FilterListener {

  @Subscribe(order = PostOrder.FIRST)
  public void onLogin(LoginLimboRegisterEvent e) {
    e.addCallback(CompletableFuture.runAsync(() -> FilterPlugin.getInstance().filter(e.getPlayer())));
  }
}
