package io.github.mal32.endergames.worlds.lobby;

import io.github.mal32.endergames.MapPixel;
import java.awt.Color;
import java.util.*;
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
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.jspecify.annotations.NonNull;

record MapTile(int x, int y) {}

public class MapManager {
  private static final int MATRIX_SIZE = 600;
  private static final int TILE = 128;
  private static final int TILES = 5; // 5x5
  private static final int GRID_PIXEL_DIMENSION = TILES * TILE; // 640
  private static final int CENTER_OFFSET = (GRID_PIXEL_DIMENSION - MATRIX_SIZE) / 2; // 20

  // wall info (your coordinates)
  private static final int WALL_START_X = 5; // spans x=1..5
  private static final int WALL_START_Y = 75; // spans y=71..75
  private static final int WALL_Z = 6;
  private static final BlockFace WALL_FACING = BlockFace.NORTH;

  // deterministic spiral: center -> outward
  private static final List<MapTile> SPIRAL_ORDER = buildSpiralOrder();

  // cache: create once, reuse forever
  private final Map<MapTile, MatrixMapRenderer> renderers = new HashMap<>();
  private boolean initialized = false;

  private static List<MapTile> buildSpiralOrder() {
    int cx = MapManager.TILES / 2;
    int cy = MapManager.TILES / 2;

    var out = new ArrayList<MapTile>(MapManager.TILES * MapManager.TILES);
    int x = cx, y = cy;
    out.add(new MapTile(x, y));

    int step = 1;
    while (out.size() < MapManager.TILES * MapManager.TILES) {
      // right, down
      for (int i = 0; i < step && out.size() < MapManager.TILES * MapManager.TILES; i++)
        out.add(new MapTile(++x, y));
      for (int i = 0; i < step && out.size() < MapManager.TILES * MapManager.TILES; i++)
        out.add(new MapTile(x, ++y));
      step++;

      // left, up
      for (int i = 0; i < step && out.size() < MapManager.TILES * MapManager.TILES; i++)
        out.add(new MapTile(--x, y));
      for (int i = 0; i < step && out.size() < MapManager.TILES * MapManager.TILES; i++)
        out.add(new MapTile(x, --y));
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

        ItemFrame frame = findTargetFrame(world, worldX, worldY);

        MapView view = Bukkit.createMap(world);
        for (var r : List.copyOf(view.getRenderers())) view.removeRenderer(r);

        view.setTrackingPosition(false);
        view.setUnlimitedTracking(false);
        view.setLocked(true);

        MatrixMapRenderer renderer = new MatrixMapRenderer(tx, ty);
        view.addRenderer(renderer);

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

  private ItemFrame findTargetFrame(World world, int x, int y) {
    Location expected = new Location(world, x + 0.5, y + 0.5, MapManager.WALL_Z + 0.5);

    return world.getNearbyEntities(expected, 1.5, 1.5, 1.5, e -> e instanceof ItemFrame).stream()
        .map(e -> (ItemFrame) e)
        .filter(f -> f.getFacing() == WALL_FACING)
        .filter(f -> f.getLocation().getBlockX() == x && f.getLocation().getBlockY() == y)
        .min(Comparator.comparingDouble(f -> f.getLocation().distanceSquared(expected)))
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "No matching ItemFrame at x="
                        + x
                        + " y="
                        + y
                        + " near z="
                        + MapManager.WALL_Z
                        + " facing "
                        + WALL_FACING));
  }

  private int getTileIndexOfCoordinate(int cord) {
    int cordWithBorder = cord + CENTER_OFFSET;
    return cordWithBorder / TILE;
  }

  /**
   * Main entry: pushes pixels into per-tile queues so updates appear gradually without map reload
   * flicker.
   */
  public void addToMapWall(ArrayList<MapPixel> chunkPixels, boolean forceFullUpdate) {
    World world = Bukkit.getWorld("world_enga_lobby");
    if (world == null) return;

    ensureInitialized(world);

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
        renderer.enqueuePixels(list, forceFullUpdate);
      }
    }
  }

  static class MatrixMapRenderer extends MapRenderer {
    private static final int TILE = 128;
    private static final int TILE_PIXELS = TILE * TILE;
    private static final int MATRIX_SIZE = 600;
    private static final int CENTER_OFFSET = (5 * TILE - MATRIX_SIZE) / 2;

    private static final Color TRANSPARENT = new Color(0, 0, 0, 0);

    private final int originX;
    private final int originY;

    private final Color[] tile = new Color[TILE_PIXELS];
    private final BitSet dirty = new BitSet(TILE_PIXELS);

    private boolean fullRedraw = false;

    public MatrixMapRenderer(int tileX, int tileY) {
      this.originX = tileX * TILE - CENTER_OFFSET;
      this.originY = tileY * TILE - CENTER_OFFSET;
    }

    public void enqueuePixels(List<MapPixel> pixels, boolean forceFullUpdate) {
      if (forceFullUpdate) {
        for (MapPixel p : pixels) {
          int lx = p.x() - originX;
          int ly = p.y() - originY;
          if ((lx | ly) < 0 || lx >= TILE || ly >= TILE) continue;
          tile[ly * TILE + lx] = p.color();
        }

        fullRedraw = true;
        dirty.clear();
      } else {
        for (MapPixel p : pixels) {
          int lx = p.x() - originX;
          int ly = p.y() - originY;
          if ((lx | ly) < 0 || lx >= TILE || ly >= TILE) continue;

          int idx = ly * TILE + lx;
          tile[idx] = p.color();
          dirty.set(idx);
        }
      }
    }

    @Override
    public void render(
        @NonNull MapView mapView, @NonNull MapCanvas canvas, @NonNull Player player) {

      if (fullRedraw) {
        // iterate linear through all pixels
        for (int idx = 0; idx < tile.length; idx++) {
          int x = idx % TILE;
          int y = idx / TILE;

          Color c = tile[idx];
          canvas.setPixelColor(x, y, c != null ? c : TRANSPARENT);
        }
        fullRedraw = false;
      } else {
        // iterate just through newly changed pixels
        int idx = dirty.nextSetBit(0);
        while (idx >= 0) {
          dirty.clear(idx);

          int x = idx % TILE;
          int y = idx / TILE;

          Color c = tile[idx];
          canvas.setPixelColor(x, y, c != null ? c : TRANSPARENT);
          idx = dirty.nextSetBit(idx + 1);
        }
      }
    }
  }
}
