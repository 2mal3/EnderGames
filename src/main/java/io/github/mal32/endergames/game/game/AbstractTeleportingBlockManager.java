package io.github.mal32.endergames.game.game;

import io.github.mal32.endergames.EnderGames;
import io.github.mal32.endergames.game.phases.PhaseController;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.*;
import org.bukkit.block.Biome;
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
    int preferredBlockCount = Math.max((int) (chunkCount * getAvgBocksPerChunk()), 1);

    // Too many blocks in the world: do nothing and let them despawn
    if (blocks.size() > preferredBlockCount) return;

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
    final int MAX_ATTEMPTS = 25;

    final Location center = gameWorld.getWorldBorder().getCenter();
    final int size = (int) gameWorld.getWorldBorder().getSize();

    Location targetLocation = null;
    for (int attempts = 0; attempts < MAX_ATTEMPTS; attempts++) {
      final int randomX =
          (ThreadLocalRandom.current().nextInt(size) - (size / 2)) + center.getBlockX();
      final int randomZ =
          (ThreadLocalRandom.current().nextInt(size) - (size / 2)) + center.getBlockZ();
      targetLocation = new Location(Bukkit.getWorld("world"), randomX, 65, randomZ);

      final double minHorizontalDistance = getMinHorizontalDistanceToPlayers(targetLocation);
      if (minHorizontalDistance < MIN_PLAYER_DISTANCE) {
        continue;
      }

      Biome biome = gameWorld.getBiome(targetLocation);
      if (ThreadLocalRandom.current().nextDouble() > getBiomeSelectChance(biome)) {
        continue;
      }

      plugin.getComponentLogger().info("Took " + attempts + " attempts");
      break;
    }

    return targetLocation;
  }

  private double getBiomeSelectChance(Biome biome) {
    // Up
    if (biome == Biome.BAMBOO_JUNGLE) return 0.75;
    if (biome == Biome.BIRCH_FOREST) return 0.75;
    if (biome == Biome.DARK_FOREST) return 0.75;
    if (biome == Biome.FOREST) return 0.75;
    if (biome == Biome.FLOWER_FOREST) return 0.75;
    if (biome == Biome.JUNGLE) return 0.75;
    if (biome == Biome.PALE_GARDEN) return 0.75;
    if (biome == Biome.MANGROVE_SWAMP) return 0.75;
    if (biome == Biome.OLD_GROWTH_BIRCH_FOREST) return 0.75;
    if (biome == Biome.OLD_GROWTH_PINE_TAIGA) return 0.75;
    if (biome == Biome.OLD_GROWTH_SPRUCE_TAIGA) return 0.75;
    if (biome == Biome.SUNFLOWER_PLAINS) return 0.75;
    if (biome == Biome.SWAMP) return 0.75;
    if (biome == Biome.TAIGA) return 0.75;
    if (biome == Biome.SNOWY_TAIGA) return 0.75;

    // Down
    if (biome == Biome.DESERT) return 0.25;
    if (biome == Biome.PLAINS) return 0.25;
    if (biome == Biome.MEADOW) return 0.25;
    if (biome == Biome.SNOWY_PLAINS) return 0.25;
    if (biome == Biome.MUSHROOM_FIELDS) return 0.25;
    if (biome == Biome.SAVANNA) return 0.25;
    if (biome == Biome.FROZEN_RIVER) return 0.25;

    // Default
    return 0.5;
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
