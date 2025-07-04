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
  protected final Location spawnLocation;
  private int nextIndex = 0;

  public AbstractTeleportingBlockManager(EnderGames plugin, Location spawnLocation) {
    super(plugin);

    this.spawnLocation = spawnLocation;

    int playerCount = GameWorld.getPlayersInGame().length;
    Location startLocation = spawnLocation.clone();
    startLocation.setY(0);
    for (int i = 0; i < playerCount * blocksPerPlayer(); i++) {
      blocks.add(getNewBlock(startLocation));
    }
  }

  public static <T extends BlockRange> T chooseOnWeight(List<T> items) {
    double totalWeight = 0.0;
    for (T item : items) totalWeight += item.weight();
    double r = Math.random() * totalWeight;
    double cumulativeWeight = 0.0;
    for (T item : items) {
      cumulativeWeight += item.weight();
      if (cumulativeWeight >= r) return item;
    }
    throw new RuntimeException("Should never be shown.");
  }

  protected abstract int blocksPerPlayer();

  protected abstract B getNewBlock(Location location);

  public void task() {
    if (blocks.isEmpty()) return;
    nextIndex = nextIndex % blocks.size();

    B block = blocks.get(nextIndex);

    Location horizontalLocation = getRandomHorizontalLocation();
    int y =
        horizontalLocation
            .getWorld()
            .getHighestBlockAt(horizontalLocation, HeightMap.OCEAN_FLOOR)
            .getY();
    horizontalLocation.setY(y + 1);

    block.teleport(horizontalLocation);
    nextIndex++;
  }

  protected Location getRandomHorizontalLocation() {
    Player[] players = GameWorld.getPlayersInGame();
    World world;
    WorldBorder border;
    Random random = new Random();

    if (players.length == 0) {
      world = Bukkit.getWorlds().get(0);
      border = world.getWorldBorder();
      return getRandomHorizontalBorderLocation(world, border, random);
    }

    // Otherwise, pick a random player
    Player player = players[random.nextInt(players.length)];
    world = player.getWorld();
    border = world.getWorldBorder();

    var randomNumber = random.nextDouble(1);
    List<BlockRange> distances =
        List.of(
            new BlockRange(16, 32, 2),
            new BlockRange(32, 80, 12),
            new BlockRange(80, 120, 33),
            new BlockRange(120, 160, 53));
    BlockRange randomRange = chooseOnWeight(distances);

    // Try a few times to pick a valid "around-player" location inside the border
    final int MAX_ATTEMPTS = 10;
    for (int i = 0; i < MAX_ATTEMPTS; i++) {
      // Choose a random distance in [minDist, maxDist)
      double dist =
          randomRange.min() + (random.nextDouble() * (randomRange.max() - randomRange.min()));
      // Choose a random angle in radians [0, 2π)
      double angle = random.nextDouble() * 2 * Math.PI;

      // Compute offset from the player's X,Z
      double dx = Math.cos(angle) * dist;
      double dz = Math.sin(angle) * dist;

      double x = player.getLocation().getX() + dx;
      double z = player.getLocation().getZ() + dz;

      Location candidate = new Location(world, x, 0, z);
      if (border.isInside(candidate) && world.isChunkLoaded(candidate.getChunk())) {
        return candidate;
      }
    }

    // If we never found a valid around-player spot, fall back to border-only
    return getRandomHorizontalBorderLocation(world, border, random);
  }

  /**
   * Picks a truly random location inside the world border (anywhere in the square), then clamps to
   * ground level +1.
   */
  private Location getRandomHorizontalBorderLocation(
      World world, WorldBorder border, Random random) {
    Location center = border.getCenter();
    double halfSize = border.getSize() / 2.0;

    for (int i = 0; i < 10; i++) {
      double xOffset = (random.nextDouble() * halfSize * 2.0) - halfSize;
      double zOffset = (random.nextDouble() * halfSize * 2.0) - halfSize;

      double x = center.getX() + xOffset;
      double z = center.getZ() + zOffset;

      Location candidate = new Location(world, x, 0, z);
      if (border.isInside(candidate)) {
        return candidate;
      }
    }

    // As a last resort (very unlikely), just return the center at ground level
    return new Location(world, center.getX(), 0, center.getZ());
  }

  @Override
  public int getDelayTicks() {
    return getBlockTeleportDelayTicks() / blocks.size();
  }

  abstract int getBlockTeleportDelayTicks();

  protected B getBlockAtLocation(Location location) {
    for (B b : blocks) {
      if (b.getLocation().getBlockX() == location.getBlockX()
          && b.getLocation().getBlockZ() == location.getBlockZ()
          && b.getLocation().getBlockY() == location.getBlockY()) {
        return b;
      }
    }

    return null;
  }
}

record BlockRange(int min, int max, int weight) {}
