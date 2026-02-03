package io.github.mal32.endergames.worlds.game.game;

import io.github.mal32.endergames.EnderGames;
import io.github.mal32.endergames.worlds.game.GameWorld;
import java.util.*;
import org.bukkit.*;
import org.bukkit.entity.Player;

/*
 * This class is an abstract representation of a teleporting block manager.
 * It handles the teleportation and switching of moving blocks like ender chests in specific time intervals.
 */
public abstract class AbstractTeleportingBlockManager<B extends AbstractTeleportingBlock>
    extends AbstractTask {
  protected final ArrayList<B> blocks = new ArrayList<>();
  protected final Location spawnLocation;
  private final World gameWorld = Objects.requireNonNull(Bukkit.getWorld("world"));

  public AbstractTeleportingBlockManager(EnderGames plugin, Location spawnLocation) {
    super(plugin);

    this.spawnLocation = spawnLocation;
  }

  protected abstract double getAvgBocksPerChunk();

  protected abstract int getBlockSecondsToLive();

  protected abstract B getNewBlock(Location location);

  protected ArrayList<B> getBlocks() {
    return blocks;
  }

  public void task() {
    tickBlocks();
    spawnNewBlock();
  }

  private void tickBlocks() {
    for (int i = blocks.size() - 1; i >= 0; i--) {
      B block = blocks.get(i);
      block.secondsToLive--;
      if (block.secondsToLive <= 0) {
        removeBlock(block);
      }
    }
  }

  private void spawnNewBlock() {
    double worldSizeChunks = gameWorld.getWorldBorder().getSize() / 16.0;
    double chunkCount = worldSizeChunks * worldSizeChunks;
    int preferedBlockCount = (int) (chunkCount * getAvgBocksPerChunk());

    // Too many blocks in the world: do nothing and let them despawn
    if (blocks.size() > preferedBlockCount) return;

    Location randomHorizontalLocation = getRandomHorizontalLocation();
    B newBlock = getNewBlock(randomHorizontalLocation);
    blocks.add(newBlock);
  }

  protected void removeBlock(B block) {
    block.destroy();
    blocks.remove(block);
  }

  protected Location getRandomHorizontalLocation() {
    final int MIN_PLAYER_DISTANCE = 32;
    final int MAX_ATTEMPTS = 100;

    final Location center = gameWorld.getWorldBorder().getCenter();
    final int size = (int) gameWorld.getWorldBorder().getSize();

    Location targetLocation = null;
    for (int attempts = 0; attempts < MAX_ATTEMPTS; attempts++) {
      final int randomX = (new Random().nextInt(size) - (size / 2)) + center.getBlockX();
      final int randomZ = (new Random().nextInt(size) - (size / 2)) + center.getBlockZ();
      targetLocation = new Location(Bukkit.getWorld("world"), randomX, 0, randomZ);

      final double minHorizontalDistance = getMinHorizontalDistanceToPlayers(targetLocation);
      if (minHorizontalDistance < MIN_PLAYER_DISTANCE) {
        continue;
      }
      break;
    }

    return targetLocation;
  }

  private static double getMinHorizontalDistanceToPlayers(Location chunkBlockLocation) {
    double minHorizontalDistance = Double.MAX_VALUE;
    for (Player player : GameWorld.getPlayersInGame()) {
      var playerLocation = player.getLocation();
      double horizontalDistance =
          Math.sqrt(
              Math.pow(playerLocation.getBlockX() - chunkBlockLocation.getBlockX(), 2)
                  + Math.pow(playerLocation.getBlockZ() - chunkBlockLocation.getBlockZ(), 2));
      if (horizontalDistance < minHorizontalDistance) {
        minHorizontalDistance = horizontalDistance;
      }
    }
    return minHorizontalDistance;
  }

  @Override
  protected int getDelayTicks() {
    return 20;
  }

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
