package io.github.mal32.endergames.game.game;

import io.github.mal32.endergames.EnderGames;
import io.github.mal32.endergames.game.phases.PhaseController;
import java.util.*;
import org.bukkit.*;
import org.bukkit.block.Block;
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
  private final Random random = new Random();

  public AbstractTeleportingBlockManager(EnderGames plugin, Location spawnLocation) {
    super(plugin);

    this.spawnLocation = spawnLocation;
  }

  private static double getMinHorizontalDistanceToPlayers(Location chunkBlockLocation) {
    double minHorizontalDistance = Double.MAX_VALUE;
    for (Player player : PhaseController.getPlayersInGame()) {
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
      block.ticksToLive -= getDelayTicks();
      if (block.ticksToLive <= 0) {
        removeBlock(block);
      }
    }
  }

  private void spawnNewBlock() {
    double worldSizeChunks = gameWorld.getWorldBorder().getSize() / 16.0;
    double chunkCount = worldSizeChunks * worldSizeChunks;
    int preferedBlockCount = Math.max((int) (chunkCount * getAvgBocksPerChunk()), 1);

    // Too many blocks in the world: do nothing and let them despawn
    if (blocks.size() > preferedBlockCount) return;

    Location randomHorizontalLocation = getRandomHorizontalLocation();

    loadChunkIfNotLoaded(randomHorizontalLocation);
    int verticalPosition = getVerticalPosition(randomHorizontalLocation);
    randomHorizontalLocation.setY(verticalPosition);

    B newBlock = getNewBlock(randomHorizontalLocation);
    blocks.add(newBlock);
  }

  private int getVerticalPosition(Location horizontalLocation) {
    int y = horizontalLocation.getWorld().getMaxHeight() + 1;
    Location location = horizontalLocation.clone();
    Block block;
    do {
      y--;
      location.setY(y);
      block = location.getBlock();
    } while (block.isPassable() || Tag.LEAVES.isTagged(block.getType()));

    return y + 1;
  }

  private void loadChunkIfNotLoaded(Location location) {
    World world = location.getWorld();
    int chunkX = location.getBlockX() >> 4;
    int chunkZ = location.getBlockZ() >> 4;
    boolean chunkLoaded = world.isChunkLoaded(chunkX, chunkZ);
    if (!chunkLoaded) {
      world.loadChunk(chunkX, chunkZ, true);
    }
  }

  protected void removeBlock(B block) {
    block.destroy();
    blocks.remove(block);
  }

  protected Location getRandomHorizontalLocation() {
    final int MIN_PLAYER_DISTANCE = 32;
    final int MAX_ATTEMPTS = 50;

    final Location center = gameWorld.getWorldBorder().getCenter();
    final int size = (int) gameWorld.getWorldBorder().getSize();

    Location targetLocation = null;
    for (int attempts = 0; attempts < MAX_ATTEMPTS; attempts++) {
      final int randomX = (random.nextInt(size) - (size / 2)) + center.getBlockX();
      final int randomZ = (random.nextInt(size) - (size / 2)) + center.getBlockZ();
      targetLocation = new Location(Bukkit.getWorld("world"), randomX, 0, randomZ);

      final double minHorizontalDistance = getMinHorizontalDistanceToPlayers(targetLocation);
      if (minHorizontalDistance < MIN_PLAYER_DISTANCE) {
        continue;
      }
      break;
    }

    return targetLocation;
  }

  @Override
  protected int getDelayTicks() {
    return 10;
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
