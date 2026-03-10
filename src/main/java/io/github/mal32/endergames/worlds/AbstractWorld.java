package io.github.mal32.endergames.worlds;

import io.github.mal32.endergames.EnderGames;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;

public abstract class AbstractWorld implements Listener {
  protected final EnderGames plugin;

  public AbstractWorld(EnderGames plugin) {
    this.plugin = plugin;

    Bukkit.getPluginManager().registerEvents(this, plugin);
  }

  protected abstract void setupWorld();

  public abstract void resetWorld();

  public abstract World getWorld();

  public abstract Location getSpawnLocation();

  public void disable() {
    HandlerList.unregisterAll(this);
  }

  public abstract void initPlayer(Player player);

  protected abstract boolean isInThisWorld(Player player);

  protected void resetPlayer(Player player) {
    player.getInventory().clear();
    player.setFireTicks(0);
    player.setFoodLevel(20);
    player.setHealth(Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).getValue());
    player.setFallDistance(0);
    player.setVelocity(new Vector(0, 0, 0));
    for (PotionEffect effect : player.getActivePotionEffects()) {
      player.removePotionEffect(effect.getType());
    }
  }

  protected void teleport(Player player, Location location) {
    player.teleportAsync(location);
  }

  protected abstract void shutdown();
}
