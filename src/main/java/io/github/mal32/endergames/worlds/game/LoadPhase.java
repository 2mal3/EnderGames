package io.github.mal32.endergames.worlds.game;

import io.github.mal32.endergames.EnderGames;
import io.github.mal32.endergames.MapPixel;
import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import org.bukkit.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.structure.Structure;
import org.bukkit.structure.StructureManager;
import org.bukkit.util.BlockVector;

public class LoadPhase extends AbstractPhase {
  private final Queue<Location> chunksToLoad = new LinkedList<>();
  private final BukkitTask chunkGenWorker;
  private final BukkitTask pixelFlushTask;
  private final List<MapPixel> pendingPixels = new ArrayList<>();
  private final int MAP_SIZE = 600;

  public LoadPhase(EnderGames plugin, GameWorld manager, Location spawnLocation) {
    super(plugin, manager, spawnLocation);

    placeSpawnPlatform();

    scheduleChunks();

    BukkitScheduler scheduler = plugin.getServer().getScheduler();
    final int CHUNK_GEN_DELAY_TICKS = 2;
    chunkGenWorker =
        scheduler.runTaskTimer(plugin, this::chunkGenWorker, 20 * 5, CHUNK_GEN_DELAY_TICKS);
    int FLUSH_INTERVAL_TICKS = 20 * 2;
    pixelFlushTask =
        scheduler.runTaskTimer(
            plugin, this::flushPixels, FLUSH_INTERVAL_TICKS, FLUSH_INTERVAL_TICKS);
  }

  @Override
  public void disable() {
    super.disable();

    chunkGenWorker.cancel();
    pixelFlushTask.cancel();
  }

  public void placeSpawnPlatform() {
    StructureManager manager = Bukkit.getServer().getStructureManager();
    Structure structure = manager.loadStructure(new NamespacedKey("enga", "spawn_platform"));

    BlockVector structureSize = structure.getSize();
    double posX = this.spawnLocation.getBlockX() - (structureSize.getBlockX() / 2.0) + 1;
    double posZ = this.spawnLocation.getBlockZ() - (structureSize.getBlockZ() / 2.0);
    Location location =
        new Location(this.spawnLocation.getWorld(), posX, this.spawnLocation.getY(), posZ);
    structure.place(location, true, StructureRotation.NONE, Mirror.NONE, 0, 1.0f, new Random());
  }

  private void scheduleChunks() {
    final int LOAD_RADIUS_CHUNKS = (int) Math.ceil(((float) MAP_SIZE) / 16); // 32
    var location = spawnLocation.clone();

    chunksToLoad.add(location.clone());

    int invert = 1;
    for (int i = 0; i < LOAD_RADIUS_CHUNKS; i++) {
      for (int k = 0; k < i; k++) {
        location.add(invert * 16, 0, 0);
        chunksToLoad.add(location.clone());
      }
      for (int k = 0; k < i; k++) {
        location.add(0, 0, invert * 16);
        chunksToLoad.add(location.clone());
      }

      invert *= -1;
    }
  }

  private void chunkGenWorker() {
    if (chunksToLoad.isEmpty()) {
      chunkGenWorker.cancel();
      return;
    }
    Location location = chunksToLoad.poll();

    Chunk chunk = location.getWorld().getChunkAt(location);
    chunk.load(true);

    Location spawnHorizontalLocation = spawnLocation.clone();
    spawnHorizontalLocation.setY(0);

    ArrayList<MapPixel> pixelBatch = new ArrayList<>();

    for (int x = 0; x < 16; x++) {
      for (int y = 0; y < 16; y++) {
        Location blockHorizontalLocation = location.clone().add(x, 0, y);
        Block highestBlock = spawnLocation.getWorld().getHighestBlockAt(blockHorizontalLocation);
        Block highestNonWaterBlock =
            spawnLocation
                .getWorld()
                .getHighestBlockAt(blockHorizontalLocation, HeightMap.OCEAN_FLOOR);

        int waterBlocksAbove = highestBlock.getY() - highestNonWaterBlock.getY();

        Color color = getBlockColor(highestNonWaterBlock, waterBlocksAbove);

        Location delta = blockHorizontalLocation.clone().subtract(spawnHorizontalLocation);
        Location inverted = delta.multiply(-1);
        int mapX = (int) inverted.getX() + (MAP_SIZE / 2);
        int mapY = (int) inverted.getZ() + (MAP_SIZE / 2);
        if (mapX >= 0 && mapX < MAP_SIZE && mapY >= 0 && mapY < MAP_SIZE) {
          pixelBatch.add(new MapPixel(mapX, mapY, color));
        }
      }
    }

    pendingPixels.addAll(pixelBatch);
  }

  private void flushPixels() {
    if (pendingPixels.isEmpty()) return;
    plugin.sendNewMapPixelsToLobby(new ArrayList<>(pendingPixels));
    pendingPixels.clear();
  }

  private static Color getBlockColor(Block block, int waterBlocksAbove) {
    var blockBukkitColor = block.getBlockData().getMapColor();
    Color blockColor =
        new Color(
            blockBukkitColor.getRed(), blockBukkitColor.getGreen(), blockBukkitColor.getBlue());

    Block aboveHighest = block.getRelative(0, 1, 0);
    var adjacentBlocks =
        new Block[] {
          aboveHighest.getRelative(0, 0, 1),
          aboveHighest.getRelative(1, 0, 0),
          aboveHighest.getRelative(-1, 0, 0),
          aboveHighest.getRelative(0, 0, -1)
        };
    for (Block adjacent : adjacentBlocks) {
      if (adjacent.isSolid() && !adjacent.isLiquid()) {
        if (waterBlocksAbove == 0) {
          blockColor = blockColor.darker();
        } else {
          blockColor = blockColor.brighter();
        }
      }
    }

    if (waterBlocksAbove != 0) {
      blockColor = applyWaterColorEffect(blockColor, waterBlocksAbove);
    }

    return blockColor;
  }

  private static Color applyWaterColorEffect(Color c, int waterBlocksAbove) {
    double depth = 1.0 - Math.exp(-waterBlocksAbove / 12.0);

    double t = 0.65 + 0.10 * depth; // more blue
    double s = 1.00 - (0.40 + 0.07 * depth); // more dark

    int r = (int) Math.round((c.getRed() + (30 - c.getRed()) * t) * s);
    int g = (int) Math.round((c.getGreen() + (80 - c.getGreen()) * t) * s);
    int b = (int) Math.round((c.getBlue() + (255 - c.getBlue()) * t) * s);

    return new Color(colorClamp(r), colorClamp(g), colorClamp(b), c.getAlpha());
  }

  private static int colorClamp(int v) {
    return Math.max(0, Math.min(255, v));
  }
}
