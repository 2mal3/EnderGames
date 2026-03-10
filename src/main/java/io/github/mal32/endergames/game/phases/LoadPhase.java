package io.github.mal32.endergames.game.phases;

import static io.github.mal32.endergames.game.GaussBlur.gaussianBlurAndDim;

import io.github.mal32.endergames.EnderGames;
import io.github.mal32.endergames.MapPixel;
import java.awt.Color;
import java.util.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.structure.Structure;
import org.bukkit.structure.StructureManager;
import org.bukkit.util.BlockVector;

public class LoadPhase extends AbstractPhase {
  private final int MAP_SIZE = 600;
  private final Queue<Queue<Location>> chunkLinesToLoad = new LinkedList<>();
  private final BukkitTask chunkGenTask;
  private volatile boolean mapGenRunning = true;
  private Color[][] currentMatrix = new Color[MAP_SIZE][MAP_SIZE];
  private double targetChunksPerTick = 1.0;
  private double accumulatedChunkGens = 0.0;

  public LoadPhase(EnderGames plugin, PhaseController controller) {
    super(plugin, controller);

    controller.getGameWorld().findAndSaveNewSpawnLocation(); // TODO run scheduler?
    plugin
        .getComponentLogger()
        .info("Spawn location: {}", controller.getGameWorld().getSpawnLocation());

    placeSpawnPlatform();
    scheduleChunkLists();
    chunkGenTask = Bukkit.getScheduler().runTaskTimer(plugin, this::chunkGenWorker, 20 * 5, 1);
  }

  private static Color getDirectBukkitColor(Block block) {
    var blockBukkitColor = block.getBlockData().getMapColor();
    return new Color(
        blockBukkitColor.getRed(), blockBukkitColor.getGreen(), blockBukkitColor.getBlue());
  }

  private static Color getBlockColorWithSnowEffect(Block block) {
    if (block.getRelative(BlockFace.UP).getType() == Material.SNOW) {
      if (block.getBlockData() instanceof Leaves) { // check for leaves for better contrast of trees
        Color c = getDirectBukkitColor(block);
        float t = 0.6f; // mixing 60% white

        int r = (int) (c.getRed() + (255 - c.getRed()) * t);
        int g = (int) (c.getGreen() + (255 - c.getGreen()) * t);
        int b = (int) (c.getBlue() + (255 - c.getBlue()) * t);
        return new Color(r, g, b);
      } else {
        return new Color(255, 255, 255); // white for snow
      }
    } else {
      return getDirectBukkitColor(block);
    }
  }

  private static Color getBlockColor(Block block, int waterBlocksAbove) {
    Color blockColor = getBlockColorWithSnowEffect(block);

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

  @Override
  public AbstractPhase nextPhase() {
    return new StartPhase(plugin, controller);
  }

  private void placeSpawnPlatform() {
    StructureManager manager = Bukkit.getServer().getStructureManager();
    Structure structure = manager.loadStructure(new NamespacedKey("enga", "spawn_platform"));
    if (structure == null) return;

    BlockVector structureSize = structure.getSize();
    Location spawnLocation = controller.getGameWorld().getSpawnLocation();
    double posX = spawnLocation.getBlockX() - (structureSize.getBlockX() / 2.0) + 1;
    double posZ = spawnLocation.getBlockZ() - (structureSize.getBlockZ() / 2.0);
    Location location = new Location(spawnLocation.getWorld(), posX, spawnLocation.getY(), posZ);
    structure.place(location, true, StructureRotation.NONE, Mirror.NONE, 0, 1.0f, new Random());
  }

  private void scheduleChunkLists() {
    final float LOAD_RADIUS_CHUNKS_FLOAT = ((float) MAP_SIZE) / 16; // 37.5
    final int LOAD_RADIUS_CHUNKS_INT = (int) Math.floor((LOAD_RADIUS_CHUNKS_FLOAT)); // 37
    var location = controller.getGameWorld().getSpawnLocation();

    Queue<Location> currentChunkList;
    int invert = 1;

    for (int r = 0; r < LOAD_RADIUS_CHUNKS_FLOAT + 0.99; r++) {
      currentChunkList = new LinkedList<>();
      for (int l1 = 0; l1 < r; l1++) {
        currentChunkList.add(location.clone());
        location.add(0, 0, invert * 16);
      }
      if (!currentChunkList.isEmpty()) {
        chunkLinesToLoad.add(currentChunkList);
      }

      if (r <= LOAD_RADIUS_CHUNKS_INT) {
        currentChunkList = new LinkedList<>();
        for (int l2 = 0; l2 < r; l2++) {
          currentChunkList.add(location.clone());
          location.add(invert * 16, 0, 0);
        }
        if (!currentChunkList.isEmpty()) {
          chunkLinesToLoad.add(currentChunkList);
        }
      }

      invert *= -1;
    }
  }

  private void chunkGenWorker() {
    long[] tickTimes5Sec = Bukkit.getServer().getTickTimes();
    if (tickTimes5Sec.length < 5) {
      return;
    }
    long[] shortTickTimes =
        Arrays.copyOfRange(tickTimes5Sec, tickTimes5Sec.length - 5, tickTimes5Sec.length);
    double averageTickTimeNanos = Arrays.stream(shortTickTimes).average().orElse(0);
    double averageTickTime = averageTickTimeNanos / 1_000_000.0;

    if (averageTickTime > 35.0) {
      targetChunksPerTick *= 0.95;
    } else if (averageTickTime < 25.0) {
      targetChunksPerTick *= 1.025;
    }
    targetChunksPerTick = Math.max(0.1, Math.min(40.0, targetChunksPerTick));

    // plugin
    //     .getComponentLogger()
    //     .info(String.format("mspt: %.1f; cpt: %.1f", averageTickTime, targetChunksPerTick));

    accumulatedChunkGens += targetChunksPerTick;

    ArrayList<MapPixel> pixelBatch = new ArrayList<>();

    while (accumulatedChunkGens >= 1.0) {
      accumulatedChunkGens -= 1.0;

      var newChunkPixelBatch = chunkGenTick();
      if (newChunkPixelBatch != null) {
        pixelBatch.addAll(newChunkPixelBatch);
      } else {
        mapGenRunning = false;
        chunkGenTask.cancel();
        break;
      }
    }

    plugin.changeMapPixelsInLobby(pixelBatch, false);
  }

  private ArrayList<MapPixel> chunkGenTick() {
    if (chunkLinesToLoad.isEmpty()) { // Done with rendering
      return null;
    }
    Queue<Location> currentChunkLine = chunkLinesToLoad.peek();
    if (currentChunkLine.isEmpty()) {
      if (!mapGenRunning) {
        // map gen has stopped and our line is done
        prepareMapForNextLoadPhase();
        return null;
      } else if (chunkLinesToLoad.size() > 1) {
        // we are finished with our line but there are more to come
        chunkLinesToLoad.remove();
        currentChunkLine = chunkLinesToLoad.peek();
      } else {
        // we are finished with our line and there are no more to come
        return null;
      }
    }

    Location chunkLocation = currentChunkLine.poll();
    ArrayList<MapPixel> newChunkPixelBatch = getPixelsOfChunk(chunkLocation);

    // addChunkPixelsToMatrix
    for (MapPixel pixel : newChunkPixelBatch) {
      currentMatrix[pixel.y()][pixel.x()] = pixel.color();
    }

    return newChunkPixelBatch;
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

  private ArrayList<MapPixel> getPixelsOfChunk(Location location) {
    Chunk chunk = location.getWorld().getChunkAt(location);
    chunk.load(true);

    Location spawnHorizontalLocation = controller.getGameWorld().getSpawnLocation();
    spawnHorizontalLocation.setY(0);

    ArrayList<MapPixel> pixelBatch = new ArrayList<>();

    for (int x = 0; x < 16; x++) {
      for (int y = 0; y < 16; y++) {
        Location blockHorizontalLocation = location.clone().add(x, 0, y);
        Block highestBlock =
            controller.getGameWorld().getWorld().getHighestBlockAt(blockHorizontalLocation);

        Block highestNonWaterBlock = highestBlock;

        if (isIgnoredWaterBlock(highestBlock)) {
          highestNonWaterBlock =
              controller
                  .getGameWorld()
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

  private boolean isIgnoredWaterBlock(Block block) {
    Material blockMaterial = block.getType();
    BlockData blockData = block.getBlockData();

    return blockMaterial == Material.WATER
        || blockMaterial == Material.BUBBLE_COLUMN
        || blockMaterial == Material.SEAGRASS
        || blockMaterial == Material.TALL_SEAGRASS
        || blockMaterial == Material.KELP
        || blockMaterial == Material.KELP_PLANT
        || blockData instanceof Waterlogged && ((Waterlogged) blockData).isWaterlogged();
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
