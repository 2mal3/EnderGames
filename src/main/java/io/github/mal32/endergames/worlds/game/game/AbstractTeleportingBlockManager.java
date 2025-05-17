package io.github.mal32.endergames.worlds.game.game;

import io.github.mal32.endergames.EnderGames;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import org.bukkit.*;
import org.bukkit.entity.Player;

/*
 * This class is an abstract representation of a teleporting block manager.
 * It handles the teleportation and switching of moving blocks like ender chests in specific time intervals.
 */
public abstract class AbstractTeleportingBlockManager extends AbstractTask {
  public AbstractTeleportingBlockManager(EnderGames plugin) {
    super(plugin);
  }

  protected static void playTeleportEffects(Location location) {
    location.getWorld().playSound(location, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 0.5f);
    location.getWorld().spawnParticle(Particle.PORTAL, location, 50, 0, 0, 0);
  }

  protected Location getRandomLocationNearPlayer() {
    List<Player> players =
        Bukkit.getOnlinePlayers().stream()
            .filter(plugin::playerIsInGameWorld)
            .filter(p -> p.getGameMode() != GameMode.SPECTATOR)
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
}
