package io.github.mal32.endergames;

import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

public abstract class AbstractWorld implements Listener {
  protected final JavaPlugin plugin;

  public AbstractWorld(JavaPlugin plugin) {
    this.plugin = plugin;

    Bukkit.getPluginManager().registerEvents(this, plugin);
  }

  public abstract void setupWorld();

  public abstract void resetWorld();

  public abstract World getWorld();

  public abstract Location getSpawnLocation();

  protected abstract boolean isInThisWorld(Player player);

  public void disable() {
    HandlerList.unregisterAll(this);
  }

  public void initPlayer(Player player) {
    player.getInventory().clear();
    player.setFireTicks(0);
    player.setFoodLevel(20);
    player.setHealth(Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).getValue());
    player.setFallDistance(0);
    player.setVelocity(new Vector(0, 0, 0));
    player.getActivePotionEffects().forEach(e -> player.removePotionEffect(e.getType()));
  }
}
