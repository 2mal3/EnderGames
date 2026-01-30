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

  protected abstract int blocksPerPlayer();

  protected abstract B getNewBlock(Location location);

  public void task() {
    if (blocks.isEmpty()) return;
    B block = chooseBlock();

    Location horizontalLocation = getRandomHorizontalLocation();
    int y =
        horizontalLocation
            .getWorld()
            .getHighestBlockAt(horizontalLocation, HeightMap.OCEAN_FLOOR)
            .getY();
    horizontalLocation.setY(y + 1);

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
    final int MIN_PLAYER_DISTANCE = 48;
    final int MAX_PLAYER_DISTANCE = 96;

    final World world = Objects.requireNonNull(Bukkit.getWorld("world"));
    ArrayList<Chunk> loadedChunks = new ArrayList<>(Arrays.asList(world.getLoadedChunks()));

    Chunk targetChunk = null;
    while (targetChunk == null) {
      int randomIndex = new Random().nextInt(loadedChunks.size());
      var chunk = loadedChunks.get(randomIndex);
      loadedChunks.remove(randomIndex);

      // replace with kd-tree when to slow
      Location chunkBlockLocation = chunk.getBlock(0, 0, 0).getLocation();
      double minHorizontalDistance = getMinHorizontalDistanceToPlayers(chunkBlockLocation);

      if (minHorizontalDistance > MIN_PLAYER_DISTANCE
          && minHorizontalDistance < MAX_PLAYER_DISTANCE) {
        targetChunk = chunk;
      }
    }

    // calculate random offset
    int xOffset = new Random().nextInt(16);
    int zOffset = new Random().nextInt(16);

    return targetChunk.getBlock(xOffset, 0, zOffset).getLocation();
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
