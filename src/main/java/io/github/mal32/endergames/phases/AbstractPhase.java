package io.github.mal32.endergames.phases;

import io.github.mal32.endergames.EnderGames;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

public abstract class AbstractPhase implements Listener {
  protected final EnderGames plugin;
  protected final PhaseController controller;

  public AbstractPhase(EnderGames plugin, PhaseController controller) {
    this.plugin = plugin;
    this.controller = controller;
    Bukkit.getPluginManager().registerEvents(this, plugin);
  }

  public abstract AbstractPhase nextPhase();

  public void disable() {
    HandlerList.unregisterAll(this);
  }
}
