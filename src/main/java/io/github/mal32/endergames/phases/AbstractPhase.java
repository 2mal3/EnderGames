package io.github.mal32.endergames.phases;

import io.github.mal32.endergames.EnderGames;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

public class AbstractPhase implements Listener {
  protected final EnderGames plugin;
  public Location spawnLocation;

  public AbstractPhase(EnderGames plugin, Location spawn) {
    this.plugin = plugin;
    this.spawnLocation = spawn;

    Bukkit.getPluginManager().registerEvents(this, plugin);
  }

  public void stop() {
    HandlerList.unregisterAll(this);
  }
}
