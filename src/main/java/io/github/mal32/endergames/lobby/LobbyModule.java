package io.github.mal32.endergames.lobby;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public abstract class LobbyModule implements Listener {
  protected final JavaPlugin plugin;

  protected LobbyModule(JavaPlugin plugin) {
    this.plugin = plugin;
  }

  public void onRegister() {
    Bukkit.getPluginManager().registerEvents(this, plugin);
  }

  public void onPlayerJoinLobby(Player player) {}

  public void onDisable() {
    HandlerList.unregisterAll(this);
  }
}
