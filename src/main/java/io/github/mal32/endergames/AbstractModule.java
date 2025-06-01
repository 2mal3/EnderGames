package io.github.mal32.endergames;

import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

public abstract class AbstractModule implements Listener {
  protected final EnderGames plugin;

  public AbstractModule(EnderGames plugin) {
    this.plugin = plugin;
  }

  public void enable() {
    Bukkit.getPluginManager().registerEvents(this, plugin);
  }

  public void disable() {
    HandlerList.unregisterAll(this);
  }
}
