package io.github.mal32.endergames.worlds.game;

import io.github.mal32.endergames.EnderGames;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

public abstract class AbstractPhase implements Listener {
  protected final EnderGames plugin;
  protected final Location spawnLocation;
  protected final GameWorld manager;

  public AbstractPhase(EnderGames plugin, GameWorld manager, Location spawnLocation) {
    this.plugin = plugin;
    this.spawnLocation = spawnLocation;
    this.manager = manager;

    Bukkit.getPluginManager().registerEvents(this, plugin);
  }

  public void disable() {
    HandlerList.unregisterAll(this);
  }
}
