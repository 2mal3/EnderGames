package io.github.mal32.endergames.worlds.game;

import static io.github.mal32.endergames.worlds.game.GaussBlur.gaussianBlurAndDim;

import io.github.mal32.endergames.EnderGames;
import io.github.mal32.endergames.MapPixel;
import java.awt.Color;
import java.util.*;
import org.bukkit.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.structure.Structure;
import org.bukkit.structure.StructureManager;
import org.bukkit.util.BlockVector;

public class LoadPhase extends AbstractPhase {
  private final int MAP_SIZE = 600;
  private volatile boolean mapGenRunning = false;
  private final Queue<Queue<Location>> chunkLinesToLoad = new LinkedList<>();
  private Color[][] currentMatrix = new Color[MAP_SIZE][MAP_SIZE];

  public LoadPhase(EnderGames plugin, GameWorld manager, Location spawnLocation) {
    super(plugin, manager, spawnLocation);

    placeSpawnPlatform();
    scheduleChunkLists();
    startMapGen();
  }

  public void disable() {
    super.disable();

    if (mapGenRunning) {
      mapGenRunning = false;
    } else {
      // already finished itself -> run prepareMapForNextLoadPhase() directly
      prepareMapForNextLoadPhase();
    }
  }

  public void startMapGen() {
    BukkitScheduler scheduler = plugin.getServer().getScheduler();
    final int MAP_GEN_DELAY_TICKS = 2;

    mapGenRunning = true;

    Runnable runner =
        new Runnable() {
          @Override
          public void run() {

            Queue<Location> currentChunkLine = chunkLinesToLoad.peek();
            if (currentChunkLine != null && currentChunkLine.isEmpty()) {
              if (!mapGenRunning) {
                // stop only if line is completed
                prepareMapForNextLoadPhase();
                return;
              }
              chunkLinesToLoad.remove();
              currentChunkLine = chunkLinesToLoad.peek();
            }
            if (currentChunkLine == null) {
              mapGenRunning = false;
              return;
            }

            generateMap(currentChunkLine);

            scheduler.runTaskLater(plugin, this, MAP_GEN_DELAY_TICKS);
          }
        };

    scheduler.runTask(plugin, runner);
  }

  public void placeSpawnPlatform() {
    StructureManager manager = Bukkit.getServer().getStructureManager();
    Structure structure = manager.loadStructure(new NamespacedKey("enga", "spawn_platform"));
    if (structure == null) return;

    BlockVector structureSize = structure.getSize();
    double posX = this.spawnLocation.getBlockX() - (structureSize.getBlockX() / 2.0) + 1;
    double posZ = this.spawnLocation.getBlockZ() - (structureSize.getBlockZ() / 2.0);
    Location location =
        new Location(this.spawnLocation.getWorld(), posX, this.spawnLocation.getY(), posZ);
    structure.place(location, true, StructureRotation.NONE, Mirror.NONE, 0, 1.0f, new Random());
  }

  private void scheduleChunkLists() {
    final float LOAD_RADIUS_CHUNKS_FLOAT = ((float) MAP_SIZE) / 16; // 37.5
    final int LOAD_RADIUS_CHUNKS_INT = (int) Math.floor((LOAD_RADIUS_CHUNKS_FLOAT)); // 37
    var location = spawnLocation.clone();

    Queue<Location> currentChunkList;
    int invert = 1;

    for (int r = 0; r < LOAD_RADIUS_CHUNKS_FLOAT + 0.99; r++) {
      currentChunkList = new LinkedList<>();
      for (int l1 = 0; l1 < r; l1++) {
        currentChunkList.add(location.clone());
        location.add(0, 0, invert * 16);
      }
      chunkLinesToLoad.add(currentChunkList);

      if (r <= LOAD_RADIUS_CHUNKS_INT) {
        currentChunkList = new LinkedList<>();
        for (int l2 = 0; l2 < r; l2++) {
          currentChunkList.add(location.clone());
          location.add(invert * 16, 0, 0);
        }
        chunkLinesToLoad.add(currentChunkList);
      }

      invert *= -1;
    }
  }

  private void generateMap(Queue<Location> currentChunkLine) {
    Location chunkLocation = currentChunkLine.poll();
    if (chunkLocation == null) return;
    ArrayList<MapPixel> newChunkPixelBatch = getChunkPixelBatch(chunkLocation);
    addChunkPixelsToMatrix(newChunkPixelBatch);
    plugin.changeMapPixelsInLobby(newChunkPixelBatch, false);
  }

  private ArrayList<MapPixel> getChunkPixelBatch(Location location) {
    Chunk chunk = location.getWorld().getChunkAt(location);
    chunk.load(true);

    Location spawnHorizontalLocation = spawnLocation.clone();
    spawnHorizontalLocation.setY(0);

    ArrayList<MapPixel> pixelBatch = new ArrayList<>();

    for (int x = 0; x < 16; x++) {
      for (int y = 0; y < 16; y++) {
        Location blockHorizontalLocation = location.clone().add(x, 0, y);
        Block highestBlock = spawnLocation.getWorld().getHighestBlockAt(blockHorizontalLocation);

        Block highestNonWaterBlock = highestBlock;
        if (highestBlock.getType() == Material.WATER) {
          highestNonWaterBlock =
              spawnLocation
                  .getWorld()
                  .getHighestBlockAt(blockHorizontalLocation, HeightMap.OCEAN_FLOOR);
        }

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

    return pixelBatch;
  }

  private void addChunkPixelsToMatrix(ArrayList<MapPixel> chunkPixels) {
    for (MapPixel pixel : chunkPixels) {
      currentMatrix[pixel.y()][pixel.x()] = pixel.color();
    }
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

  private void prepareMapForNextLoadPhase() {
    Color[][] oldBlurredMatrix = gaussianBlurAndDim(currentMatrix.clone(), MAP_SIZE, 4, 2.0, 0.70f);
    currentMatrix = new Color[MAP_SIZE][MAP_SIZE];

    ArrayList<MapPixel> fullMapUpdatePixels = new ArrayList<>();
    for (int y = 0; y < MAP_SIZE; y++) {
      for (int x = 0; x < MAP_SIZE; x++) {
        MapPixel newMapPixel = new MapPixel(x, y, oldBlurredMatrix[x][y]);
        fullMapUpdatePixels.add(newMapPixel);
      }
    }
    plugin.changeMapPixelsInLobby(fullMapUpdatePixels, true);
  }
}
