package io.github.mal32.endergames.worlds.lobby;

import java.awt.Color;

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

    public static Color[][] generateMatrix() {
        Color[][] matrix = new Color[MATRIX_SIZE][MATRIX_SIZE];

        Color darkPurple = new Color(48, 0, 64);
        Color lightPurple = new Color(88, 0, 74);
        Color lime = new Color(44, 230, 12);

        for (int y = 0; y < MATRIX_SIZE; y++) {
            for (int x = 0; x < MATRIX_SIZE; x++) {
                if (x<150 && y<150) {
                    matrix[y][x] = lime;
                }
                else if (x % 2 == 0) {
                    matrix[y][x] = lightPurple;
                }
                else  {
                    matrix[y][x] = darkPurple;
                }
            }
        }
        return matrix;
    }


    public static ItemStack[][] createMapsFromMatrix(Color[][] matrix, World world) {
        if (matrix == null || matrix.length != MATRIX_SIZE || matrix[0].length != MATRIX_SIZE) {
            throw new IllegalArgumentException("matrix must be " + MATRIX_SIZE + "x" + MATRIX_SIZE);
        }

        ItemStack[][] maps = new ItemStack[TILES][TILES];

        // For each tile (row 0 = top)
        for (int tileRow = 0; tileRow < TILES; tileRow++) {
            for (int tileCol = 0; tileCol < TILES; tileCol++) {

                final int originX = tileCol * TILE - CENTER_OFFSET;
                final int originY = tileRow * TILE - CENTER_OFFSET;

                MapView view = Bukkit.createMap(world);

                // remove default renderers
                for (MapRenderer r : view.getRenderers()) view.removeRenderer(r);

                view.addRenderer(new MapRenderer() {
                    private boolean rendered = false;

                    @Override
                    public void render(MapView mapView, MapCanvas canvas, Player player) {
                        if (rendered) return;

                        for (int py = 0; py < TILE; py++) {
                            for (int px = 0; px < TILE; px++) {
                                int srcX = originX + px;
                                int srcY = originY + py;

                                // If inside the 600x600 matrix, use that color; otherwise use null -> transparent
                                Color c = (srcX >= 0 && srcX < MATRIX_SIZE && srcY >= 0 && srcY < MATRIX_SIZE)
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

                maps[tileRow][tileCol] = filled;
            }
        }

        return maps;
    }

    public static void placeMapsInFrames(ItemStack[][] maps, World world) {
        if (maps == null || maps.length != TILES || maps[0].length != TILES) {
            throw new IllegalArgumentException("maps must be " + TILES + "x" + TILES + " array");
        }

        final int startX = 3;      // left
        final int startY = 75;     // top
        final int z = 7;

        for (int row = 0; row < TILES; row++) {         // 0 = top row
            for (int col = 0; col < TILES; col++) {     // 0 = left column
                int x = startX - col;       // left → right: 3,2,1,0,-1
                int y = startY - row;       // top → bottom: 75,74,73,72,71

                ItemStack mapItem = maps[row][col];
                if (mapItem == null) continue;

                Location center = new Location(world, x + 0.5, y + 0.5, z + 0.5);

                for (Entity e : world.getNearbyEntities(center, 0.6, 0.6, 0.6)) {
                    if (e instanceof GlowItemFrame) {  // or ItemFrame if not using glow frames
                        ItemFrame frame = (ItemFrame) e;
                        frame.setItem(mapItem, false);
                        break;
                    }
                }
            }
        }
    }



    public static void setupMapWall(World world) {
        Color[][] matrix = generateMatrix();
        ItemStack[][] maps = createMapsFromMatrix(matrix, world);
        placeMapsInFrames(maps, world);
        Location location = new Location(world, 0, 71, 0);

//        for (int i = 0; i < maps.length; i++) {
//            for (int j = 0; j < maps[i].length; j++) {
//                ItemStack item = maps[i][j];
//                if (item != null) {
//                    world.dropItem(new Location(world, 0, 71, 0), item);
//                }
//            }
//        }

    }


}
