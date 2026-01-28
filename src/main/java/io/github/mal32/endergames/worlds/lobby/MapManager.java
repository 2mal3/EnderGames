package io.github.mal32.endergames.worlds.lobby;

import io.github.mal32.endergames.MapPixel;
import java.awt.Color;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapPalette;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

record MapTile(int x, int y) {}

public class MapManager {
  private static final int MATRIX_SIZE = 600;
  private static final int TILE = 128;
  private static final int TILES = 5; // 5x5
  private static final int GRID_PIXEL_DIMENSION = TILES * TILE; // 640
  private static final int CENTER_OFFSET = (GRID_PIXEL_DIMENSION - MATRIX_SIZE) / 2; // 20

  // wall info (your coordinates)
  private static final int WALL_START_X = 5;  // spans x=1..5
  private static final int WALL_START_Y = 75; // spans y=71..75
  private static final int WALL_Z = 6;
  private static final BlockFace WALL_FACING = BlockFace.NORTH;

  // deterministic spiral: center -> outward
  private static final List<MapTile> SPIRAL_ORDER = buildSpiralOrder(TILES);

  private final Color[][] matrix = new Color[MATRIX_SIZE][MATRIX_SIZE];

  // cache: create once, reuse forever
  private final Map<MapTile, MapView> views = new HashMap<>();
  private final Map<MapTile, MatrixMapRenderer> renderers = new HashMap<>();
  private final Map<MapTile, ItemFrame> frames = new HashMap<>();
  private boolean initialized = false;

  private static List<MapTile> buildSpiralOrder(int size) {
    int cx = size / 2;
    int cy = size / 2;

    var out = new ArrayList<MapTile>(size * size);
    int x = cx, y = cy;
    out.add(new MapTile(x, y));

    int step = 1;
    while (out.size() < size * size) {
      // right, down
      for (int i = 0; i < step && out.size() < size * size; i++) out.add(new MapTile(++x, y));
      for (int i = 0; i < step && out.size() < size * size; i++) out.add(new MapTile(x, ++y));
      step++;

      // left, up
      for (int i = 0; i < step && out.size() < size * size; i++) out.add(new MapTile(--x, y));
      for (int i = 0; i < step && out.size() < size * size; i++) out.add(new MapTile(x, --y));
      step++;
    }
    return out;
  }

  private void ensureInitialized(World world) {
    if (initialized) return;

    // find frames + create maps once
    for (int ty = 0; ty < TILES; ty++) {
      for (int tx = 0; tx < TILES; tx++) {
        MapTile tile = new MapTile(tx, ty);

        int worldX = WALL_START_X - tx; // tx 0..4 -> x 5..1
        int worldY = WALL_START_Y - ty; // ty 0..4 -> y 75..71

        ItemFrame frame = findTargetFrame(world, worldX, worldY, WALL_Z);
        frames.put(tile, frame);

        MapView view = Bukkit.createMap(world);
        for (var r : List.copyOf(view.getRenderers())) view.removeRenderer(r);

        view.setTrackingPosition(false);
        view.setUnlimitedTracking(false);
        view.setLocked(true);

        MatrixMapRenderer renderer = new MatrixMapRenderer(matrix, tx, ty);
        view.addRenderer(renderer);

        views.put(tile, view);
        renderers.put(tile, renderer);

        ItemStack filled = new ItemStack(Material.FILLED_MAP);
        MapMeta meta = (MapMeta) filled.getItemMeta();
        meta.setMapView(view);
        filled.setItemMeta(meta);

        // set the map item ONCE
        frame.setItem(filled, false);
      }
    }

    initialized = true;
  }

  private ItemFrame findTargetFrame(World world, int x, int y, int zWall) {
    Location expected = new Location(world, x + 0.5, y + 0.5, zWall + 0.5);

    return world.getNearbyEntities(expected, 1.5, 1.5, 1.5, e -> e instanceof ItemFrame).stream()
            .map(e -> (ItemFrame) e)
            .filter(f -> f.getFacing() == WALL_FACING)
            .filter(f -> f.getLocation().getBlockX() == x && f.getLocation().getBlockY() == y)
            .min(Comparator.comparingDouble(f -> f.getLocation().distanceSquared(expected)))
            .orElseThrow(() -> new IllegalStateException(
                    "No matching ItemFrame at x=" + x + " y=" + y + " near z=" + zWall + " facing " + WALL_FACING));
  }

  private void addChunkPixelsToMatrix(ArrayList<MapPixel> chunkPixels) {
    for (MapPixel pixel : chunkPixels) {
      matrix[pixel.y()][pixel.x()] = pixel.color();
    }
  }

  private int getTileIndexOfCoordinate(int coord) {
    int coordWithBorder = coord + CENTER_OFFSET;
    return coordWithBorder / TILE;
  }

  /**
   * Main entry: pushes pixels into per-tile queues so updates appear gradually without map reload flicker.
   */
  public void addToMapWall(ArrayList<MapPixel> chunkPixels) {
    World world = Bukkit.getWorld("world_enga_lobby");
    if (world == null) return;

    ensureInitialized(world);

    // persist new pixels in the backing matrix (source of truth)
    addChunkPixelsToMatrix(chunkPixels);

    // group pixels by tile (so we can enqueue them in spiral order)
    Map<MapTile, ArrayList<MapPixel>> pixelsByTile = new HashMap<>();
    Set<MapTile> changedTiles = new HashSet<>();

    for (MapPixel p : chunkPixels) {
      MapTile tile = new MapTile(getTileIndexOfCoordinate(p.x()), getTileIndexOfCoordinate(p.y()));
      changedTiles.add(tile);
      pixelsByTile.computeIfAbsent(tile, k -> new ArrayList<>()).add(p);
    }

    // enqueue in deterministic spiral order (center -> outward)
    for (MapTile tile : SPIRAL_ORDER) {
      if (!changedTiles.contains(tile)) continue;

      MatrixMapRenderer renderer = renderers.get(tile);
      if (renderer == null) continue;

      ArrayList<MapPixel> list = pixelsByTile.get(tile);
      if (list != null && !list.isEmpty()) {
        renderer.enqueuePixels(list);
      }
    }
  }
}

class MatrixMapRenderer extends MapRenderer {
  private static final int TILE = 128;
  private static final int MATRIX_SIZE = 600;
  private static final int CENTER_OFFSET = (5 * TILE - MATRIX_SIZE) / 2;

  // How many pixel updates to apply per render call (controls "gradualness")
  private static final int MAX_PIXELS_PER_RENDER = 4096;

  private final Color[][] matrix;
  private final int originX;
  private final int originY;

  private boolean initialDrawDone = false;

  private record QueuedPixel(int x, int y, Color c) {}
  private final ArrayDeque<QueuedPixel> queue = new ArrayDeque<>();

  public MatrixMapRenderer(Color[][] matrix, int tileX, int tileY) {
    this.matrix = matrix;
    this.originX = tileX * TILE - CENTER_OFFSET;
    this.originY = tileY * TILE - CENTER_OFFSET;
  }

  public void enqueuePixels(List<MapPixel> pixels) {
    for (MapPixel p : pixels) {
      int lx = p.x() - originX;
      int ly = p.y() - originY;
      if (lx < 0 || lx >= TILE || ly < 0 || ly >= TILE) continue; // safety
      queue.addLast(new QueuedPixel(lx, ly, p.color()));
    }
  }

  private void drawFullTile(MapCanvas canvas) {
    for (int py = 0; py < TILE; py++) {
      for (int px = 0; px < TILE; px++) {
        int srcX = originX + px;
        int srcY = originY + py;

        Color c =
                (srcX >= 0 && srcX < MATRIX_SIZE && srcY >= 0 && srcY < MATRIX_SIZE)
                        ? matrix[srcY][srcX]
                        : null;

        if (c != null) canvas.setPixelColor(px, py, c);
        else canvas.setPixel(px, py, MapPalette.TRANSPARENT);
      }
    }
  }

  @Override
  public void render(MapView mapView, MapCanvas canvas, Player player) {
    // First time: draw whatever we have (so maps don't show vanilla/placeholder)
    if (!initialDrawDone) {
      drawFullTile(canvas);
      initialDrawDone = true;
    }

    // Apply a limited number of queued pixel updates per call to make it "gradual"
    int n = 0;
    while (n < MAX_PIXELS_PER_RENDER && !queue.isEmpty()) {
      QueuedPixel qp = queue.removeFirst();
      if (qp.c() != null) canvas.setPixelColor(qp.x(), qp.y(), qp.c());
      else canvas.setPixel(qp.x(), qp.y(), MapPalette.TRANSPARENT);
      n++;
    }
  }
}
