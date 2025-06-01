package io.github.mal32.endergames.worlds.game.game;

import io.github.mal32.endergames.EnderGames;
import io.github.mal32.endergames.worlds.game.GameWorld;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.bukkit.*;
import org.bukkit.entity.Player;

/*
 * This class is an abstract representation of a teleporting block manager.
 * It handles the teleportation and switching of moving blocks like ender chests in specific time intervals.
 */
public abstract class AbstractTeleportingBlockManager<B extends AbstractTeleportingBlock>
    extends AbstractTask {
  protected final List<B> blocks = new ArrayList<>();
  private int nextIndex = 0;

  public AbstractTeleportingBlockManager(EnderGames plugin) {
    super(plugin);
  }

  public void task() {
    if (blocks.isEmpty()) return;
    nextIndex = nextIndex % blocks.size();

    B block = blocks.get(nextIndex);
    Location location = getRandomLocation();
    block.teleport(location);
    nextIndex++;
  }

  protected Location getRandomLocation() {
    Player[] players = GameWorld.getPlayersInGame();
    World world;
    WorldBorder border;
    Random random = new Random();

    if (players.length == 0) {
      world = Bukkit.getWorlds().get(0);
      border = world.getWorldBorder();
      return getRandomBorderLocation(world, border, random);
    }

    // Otherwise, pick a random player
    Player player = players[random.nextInt(players.length)];
    world = player.getWorld();
    border = world.getWorldBorder();


    final double CHANCE_BAND_0_30   = 0.02;
    final double CHANCE_BAND_30_80 = 0.12;
    final double CHANCE_BAND_80_120 = 0.33;
    final double CHANCE_BAND_120_160 = 0.53; //isn't used cause it's the else case

    double roll = random.nextDouble();
    double minDist, maxDist;

    if (roll < CHANCE_BAND_0_30) {
      minDist = 0;
      maxDist = 30;
    } else if (roll < CHANCE_BAND_0_30 + CHANCE_BAND_30_80) {
      minDist = 30;
      maxDist = 80;
    } else if (roll < CHANCE_BAND_0_30 + CHANCE_BAND_30_80 + CHANCE_BAND_80_120) {
      minDist = 80;
      maxDist = 120;
    } else {
      minDist = 120;
      maxDist = 160;
    }

    // Try a few times to pick a valid "around-player" location inside the border
    final int MAX_ATTEMPTS = 10;
    for (int i = 0; i < MAX_ATTEMPTS; i++) {
      // Choose a random distance in [minDist, maxDist)
      double dist = minDist + (random.nextDouble() * (maxDist - minDist));
      // Choose a random angle in radians [0, 2π)
      double angle = random.nextDouble() * 2 * Math.PI;

      // Compute offset from the player's X,Z
      double dx = Math.cos(angle) * dist;
      double dz = Math.sin(angle) * dist;

      double x = player.getLocation().getX() + dx;
      double z = player.getLocation().getZ() + dz;

      double y = 0;
      Location candidate = new Location(world, x, y, z);
      if (border.isInside(candidate) && world.isChunkLoaded(candidate.getChunk())) {
        double groundY = world.getHighestBlockYAt((int) Math.floor(x), (int) Math.floor(z)) + 1.0;
        candidate.setY(groundY);
        return candidate;
      }
    }

    // If we never found a valid around-player spot, fall back to border-only
    return getRandomBorderLocation(world, border, random);
  }

  /**
   * Picks a truly random location inside the world border (anywhere in the square),
   * then clamps to ground level +1.
   */
  private Location getRandomBorderLocation(World world, WorldBorder border, Random random) {
    Location center = border.getCenter();
    double halfSize = border.getSize() / 2.0;

    for (int i = 0; i < 10; i++) {
      double xOffset = (random.nextDouble() * halfSize * 2.0) - halfSize;
      double zOffset = (random.nextDouble() * halfSize * 2.0) - halfSize;

      double x = center.getX() + xOffset;
      double z = center.getZ() + zOffset;
      int groundY = world.getHighestBlockYAt((int) Math.floor(x), (int) Math.floor(z));
      double y = groundY + 1.0;

      Location candidate = new Location(world, x, y, z);
      if (border.isInside(candidate)) {
        return candidate;
      }
    }

    // As a last resort (very unlikely), just return the center at ground level
    int groundY = world.getHighestBlockYAt(center.getBlockX(), center.getBlockZ());
    return new Location(world, center.getX(), groundY + 1.0, center.getZ());
  }


}
