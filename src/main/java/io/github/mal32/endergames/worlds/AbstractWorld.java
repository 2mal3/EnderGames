package io.github.mal32.endergames.worlds;

import io.github.mal32.endergames.EnderGames;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

public abstract class AbstractWorld implements Listener {
  protected final EnderGames plugin;

  public AbstractWorld(EnderGames plugin) {
    this.plugin = plugin;

    Bukkit.getPluginManager().registerEvents(this, plugin);
  }

  protected abstract void initPlayer(Player player);
}
