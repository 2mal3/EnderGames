package io.github.mal32.endergames.worlds.game;

import io.github.mal32.endergames.EnderGames;
import java.util.Objects;
import java.util.Random;

import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.structure.Structure;
import org.bukkit.structure.StructureManager;
import org.bukkit.util.BlockVector;

public class WorldManager {
  private final World world = Objects.requireNonNull(Bukkit.getWorld("world"));
  private final Location spawnLocation = new Location(world, 0, 200, 0);
  private final NamespacedKey spawnLocationKey;

  private final EnderGames plugin;

  public WorldManager(EnderGames plugin) {
    this.spawnLocationKey = new NamespacedKey(plugin, "spawnLocation");

    WorldBorder border = this.world.getWorldBorder();
    border.setWarningDistance(32);
    border.setWarningTime(60);
    border.setDamageBuffer(1);

    this.plugin = plugin;
  }

  // Why doesn't BiomeTagKeys.IS_OCEAN work?
  // using directly:
  // https://github.com/misode/mcmeta/blob/data/data/minecraft/tags/worldgen/biome/is_ocean.json
  private static boolean isOcean(Biome biome) {
    return biome.equals(Biome.DEEP_FROZEN_OCEAN)
            || biome.equals(Biome.DEEP_COLD_OCEAN)
            || biome.equals(Biome.DEEP_OCEAN)
            || biome.equals(Biome.DEEP_LUKEWARM_OCEAN)
            || biome.equals(Biome.FROZEN_OCEAN)
            || biome.equals(Biome.OCEAN)
            || biome.equals(Biome.COLD_OCEAN)
            || biome.equals(Biome.LUKEWARM_OCEAN)
            || biome.equals(Biome.WARM_OCEAN);
  }

  public Location getSpawnLocation() {
    return spawnLocation;
  }

  public void loadSpawnPosition() {
    this.loadSavedSpawnLocation();
    this.updateSpawnPosition();
  }

  public void updateSpawnPosition() {
    this.findNewSpawnLocation();
    this.updateSpawnLocation();
    this.placeSpawnPlatform();

    Bukkit.getScheduler().runTaskLater(this.plugin, this::loadSpawnChunks, 20 * 5);
  }

  public void prepareWorldForGame() {
    this.world.setTime(0);
    this.world.setStorm(false);
    this.world.setThundering(false);
    this.world.setWeatherDuration(20 * 60 * 10);

    this.world.getWorldBorder().setSize(600);
  }

  public void startGame() {
    world.getWorldBorder().setSize(50, 20 * 60);

    plugin.getServer().getScheduler().runTaskLater(plugin, this::removeSpawnPlatform, 30 * 20);
  }

  private void removeSpawnPlatform() {
    for (int x = spawnLocation.blockX() - 20; x <= spawnLocation.blockX() + 20; x++) {
      for (int z = spawnLocation.blockZ() - 20; z <= spawnLocation.blockZ() + 20; z++) {
        for (int y = spawnLocation.blockY() - 5; y <= spawnLocation.blockY() + 5; y++) {
          world.getBlockAt(x, y, z).setType(Material.AIR);
        }
      }
    }
  }

  private void loadSavedSpawnLocation() {
    if (!this.world.getPersistentDataContainer().has(this.spawnLocationKey)) {
      this.plugin.getComponentLogger().info("Creating spawn location");
      this.spawnLocation.setX(-1000);
      return;
    }

    double rawSpawnX = this.world.getPersistentDataContainer().get(this.spawnLocationKey, PersistentDataType.DOUBLE);
    this.spawnLocation.setX(rawSpawnX);
  }

  private void updateSpawnLocation() {
    this.world.getPersistentDataContainer().set(this.spawnLocationKey,
            PersistentDataType.DOUBLE,
            this.spawnLocation.getX());

    world.getWorldBorder().setCenter(spawnLocation);
  }

  private void findNewSpawnLocation() {
    do {
      this.spawnLocation.getChunk().load(false);
      this.spawnLocation.add(1000, 0, 0);
      this.spawnLocation.getChunk().load(true);
    } while (WorldManager.isOcean(this.spawnLocation.getBlock().getBiome()));
  }

  private void placeSpawnPlatform() {
    StructureManager manager = Bukkit.getServer().getStructureManager();
    Structure structure = manager.loadStructure(new NamespacedKey("enga", "spawn_platform"));

    BlockVector structureSize = structure.getSize();
    double posX = this.spawnLocation.getX() - (structureSize.getBlockX() / 2.0);
    double posZ = this.spawnLocation.getZ() - (structureSize.getBlockZ() / 2.0);
    Location location = new Location(this.world, posX, this.spawnLocation.getY(), posZ);
    structure.place(location, true, StructureRotation.NONE, Mirror.NONE, 0, 1.0f, new Random());
  }

  private void loadSpawnChunks() {
    final int loadRadius = 4;
    int loadDelayTicks = 0;
    final int loadDelayIncrease = 5;
    for (int x = (int) (spawnLocation.getX() - (loadRadius * 16));
         x < spawnLocation.getX() + (loadRadius * 16);
         x += 16) {
      for (int z = (int) (spawnLocation.getZ() - (loadRadius * 16));
           z < spawnLocation.getZ() + (loadRadius * 16);
           z += 16) {
        final Location location = new Location(world, x, spawnLocation.getY(), z);
        final int currentLoadDelayTicks = loadDelayTicks;
        Bukkit.getScheduler()
                .runTaskLater(
                        plugin, () -> world.getChunkAt(location).load(true), currentLoadDelayTicks);
        loadDelayTicks += loadDelayIncrease;
      }
    }
  }
}
