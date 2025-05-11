package io.github.mal32.endergames.phases.game;

import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public abstract class AbstractModule implements Listener {
  protected final JavaPlugin plugin;

  public AbstractModule(JavaPlugin plugin) {
    this.plugin = plugin;
  }

  public void enable() {
    Bukkit.getPluginManager().registerEvents(this, plugin);
  }

  public void disable() {
    HandlerList.unregisterAll(this);
  }
}
