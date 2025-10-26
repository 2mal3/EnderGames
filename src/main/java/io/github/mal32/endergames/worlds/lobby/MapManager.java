package io.github.mal32.endergames.worlds.lobby;

import io.github.mal32.endergames.MapPixel;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.plugin.java.JavaPlugin;

record MapTile(int x, int y) {}

public class MapManager {
  private static final int MATRIX_SIZE = 600;
  private static final int TILE = 128;
  private static final int TILES = 5; // 5x5
  private static final int TOTAL_PIXELS = TILES * TILE; // 640
  // center 600 inside 640 -> margin 40 total -> 20 on each side
  private static final int CENTER_OFFSET = (TOTAL_PIXELS - MATRIX_SIZE) / 2;
  private final JavaPlugin plugin;

  public MapManager(JavaPlugin plugin) {
    this.plugin = plugin;
  }

  private final Color[][] matrix = new Color[MATRIX_SIZE][MATRIX_SIZE];

  private void addChunkPixelsToMatrix(ArrayList<MapPixel> chunkPixels) {
    for (MapPixel pixel : chunkPixels) {
      matrix[pixel.y()][pixel.x()] = pixel.color();
    }
  }

  private int getTileIndexOfCoordinate(int coord) {
    int coord_with_border = coord + CENTER_OFFSET;
    return coord_with_border / TILE;
  }

  private HashSet<MapTile> getChangedMapTiles(ArrayList<MapPixel> chunkPixels) {
    HashSet<MapTile> changedMapTiles = new HashSet<>();

    int[] arr = {0, 15, 239, 255}; // index of 4 corner points of chunk
    for (int value : arr) {
      MapTile mapTile =
          new MapTile(
              getTileIndexOfCoordinate(chunkPixels.get(value).x()),
              getTileIndexOfCoordinate(chunkPixels.get(value).y()));
      changedMapTiles.add(mapTile);
    }
    return changedMapTiles;
  }

  private ItemStack generateMapFromMatrix(MapTile mapTile, World world) {
    MapView view = Bukkit.createMap(world);

    final int originX = mapTile.x() * TILE - CENTER_OFFSET;
    final int originY = mapTile.y() * TILE - CENTER_OFFSET;

    // remove default renderers
    for (MapRenderer r : view.getRenderers()) view.removeRenderer(r);

    view.addRenderer(
        new MapRenderer() {
          private boolean rendered = false;

          @Override
          public void render(MapView mapView, MapCanvas canvas, Player player) {
            if (rendered) return;

            for (int py = 0; py < TILE; py++) {
              for (int px = 0; px < TILE; px++) {
                int srcX = originX + px;
                int srcY = originY + py;

                // If inside the 600x600 matrix, use that color; otherwise use null ->
                // transparent
                Color c =
                    (srcX >= 0 && srcX < MATRIX_SIZE && srcY >= 0 && srcY < MATRIX_SIZE)
                        ? matrix[srcY][srcX]
                        : null; // null = show base pixel / transparent for this renderer

                canvas.setPixelColor(px, py, c);
              }
            }
            rendered = true;
          }
        });

    ItemStack filled = new ItemStack(Material.FILLED_MAP);
    MapMeta meta = (MapMeta) filled.getItemMeta();
    meta.setMapView(view);
    filled.setItemMeta(meta);

    return filled;
  }

  private void placeMapInFrame(MapTile mapTile, ItemStack map, World world) {
    final int startX = 3; // left
    final int startY = 75; // top
    final int z = 7;

    int x = startX - mapTile.x(); // left → right: 3,2,1,0,-1
    int y = startY - mapTile.y(); // top → bottom: 75,74,73,72,71

    this.plugin.getComponentLogger().info("Placing map at {}, {}", x, y);

    Location center = new Location(world, x + 0.5, y + 0.5, z + 0.5);

    for (Entity e : world.getNearbyEntities(center, 0.6, 0.6, 0.6)) {
      if (e instanceof GlowItemFrame) { // or ItemFrame if not using glow frames
        ItemFrame frame = (ItemFrame) e;
        frame.setItem(map, false);
        break;
      }
    }
  }

  public void addToMapWall(ArrayList<MapPixel> chunkPixels) {
    World world = Bukkit.getWorld("world_enga_lobby");
    addChunkPixelsToMatrix(chunkPixels);
    HashSet<MapTile> changedMapTiles = getChangedMapTiles(chunkPixels);
    for (MapTile mapTile : changedMapTiles) {
      ItemStack map = generateMapFromMatrix(mapTile, world);
      placeMapInFrame(mapTile, map, world);
      plugin.getComponentLogger().info(mapTile.toString());
    }
  }
}
