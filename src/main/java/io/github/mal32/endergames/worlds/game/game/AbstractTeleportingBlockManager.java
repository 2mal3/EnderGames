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
  private int nextIndex = 0;
  protected final Location spawnLocation;
  private final World gameWorld = Objects.requireNonNull(Bukkit.getWorld("world"));

  public AbstractTeleportingBlockManager(EnderGames plugin, Location spawnLocation) {
    super(plugin);

    this.spawnLocation = spawnLocation;

    Location startLocation = spawnLocation.clone();
    startLocation.setY(0);
    for (int i = 0; i < blockCount(); i++) {
      blocks.add(getNewBlock(startLocation));
    }
  }

  protected abstract int blockCount();

  protected abstract B getNewBlock(Location location);

  public void task() {
    if (blocks.isEmpty()) return;
    B block = chooseBlock();

    Location horizontalLocation = getRandomHorizontalLocation();
    block.teleport(horizontalLocation);
  }

  private B chooseBlock() {
    var usedBlocks = new ArrayList<B>();
    for (B b : blocks) {
      if (b.hasBeenUsed) {
        usedBlocks.add(b);
      }
    }

    ArrayList<B> chosenBlocks;
    if (usedBlocks.isEmpty()) {
      chosenBlocks = blocks;
    } else {
      chosenBlocks = usedBlocks;
    }

    nextIndex = nextIndex % chosenBlocks.size();
    var block = chosenBlocks.get(nextIndex);
    nextIndex++;

    return block;
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
