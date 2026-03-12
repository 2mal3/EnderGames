package io.github.mal32.endergames.world;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public abstract class AbstractWorld implements Listener {
  protected final JavaPlugin plugin;
  protected final PlayerInitService playerInitService;

  public AbstractWorld(JavaPlugin plugin, PlayerInitService playerInitService) {
    this.plugin = plugin;
    this.playerInitService = playerInitService;

    Bukkit.getPluginManager().registerEvents(this, plugin);
  }

  public abstract void setupWorld();

  public abstract void resetWorld();

  public abstract World getWorld();

  public abstract Location getSpawnLocation();

  public void initPlayer(Player player) {
    playerInitService.init(player, getSpawnLocation());
  }

  protected abstract boolean isInThisWorld(Player player);

  public void disable() {
    HandlerList.unregisterAll(this);
  }
}
