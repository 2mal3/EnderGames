package io.github.mal32.endergames.phases.game;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

public abstract class AbstractTeleportingBlockManager extends AbstractModule {
  private BukkitTask task;

  public AbstractTeleportingBlockManager(JavaPlugin plugin) {
    super(plugin);
  }

  @Override
  public void enable() {
    super.enable();

    BukkitScheduler scheduler = plugin.getServer().getScheduler();
    task = scheduler.runTaskTimer(plugin, this::task, getDelay(), getDelay());
  }

  @Override
  public void disable() {
    super.disable();

    task.cancel();
  }

  public abstract void task();

  public abstract int getDelay();

  protected Location getRandomLocationNearPlayer() {
    List<Player> players =
        Bukkit.getOnlinePlayers().stream()
            .filter(player -> player.getGameMode() == GameMode.SURVIVAL)
            .collect(Collectors.toList());
    if (players.isEmpty()) {
      return null;
    }
    Player player = players.get(new Random().nextInt(players.size()));

    Location location = player.getLocation().getBlock().getLocation().clone();
    // get a random location near the player
    final int range = 64;
    int xOffset = new Random().nextInt(range * 2) - range;
    int zOffset = new Random().nextInt(range * 2) - range;
    location.add(xOffset, 0, zOffset);
    location.setY(location.getWorld().getHighestBlockYAt(location));
    location.add(0, 1, 0);

    return location;
  }

  protected static void playTeleportEffects(Location location) {
    location.getWorld().playSound(location, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 0.5f);
    location.getWorld().spawnParticle(Particle.PORTAL, location, 50, 0, 0, 0);
  }
}
