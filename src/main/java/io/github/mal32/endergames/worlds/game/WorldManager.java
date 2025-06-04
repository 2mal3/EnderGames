package io.github.mal32.endergames.worlds.game;

import io.github.mal32.endergames.EnderGames;
import java.util.Objects;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.persistence.PersistentDataType;

public class WorldManager {
  private final World world = Objects.requireNonNull(Bukkit.getWorld("world"));
  private final NamespacedKey spawnLocationKey;
  private Location spawnLocation;

  public WorldManager(EnderGames plugin) {
    this.spawnLocationKey = new NamespacedKey(plugin, "spawnLocation");

    WorldBorder border = this.world.getWorldBorder();
    border.setWarningDistance(32);
    border.setWarningTime(60);
    border.setDamageBuffer(1);

    if (!world.getPersistentDataContainer().has(this.spawnLocationKey)) {
      plugin.getComponentLogger().info("Creating spawn location");
      spawnLocation = new Location(world, 0, 200, 0);
      findAndSaveNewSpawnLocation();
    } else {
      loadSpawnLocation();
    }
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

  private void saveSpawnLocation() {
    this.world
        .getPersistentDataContainer()
        .set(this.spawnLocationKey, PersistentDataType.INTEGER, this.spawnLocation.getBlockX());
  }

  private void loadSpawnLocation() {
    double rawSpawnX =
        this.world
            .getPersistentDataContainer()
            .get(this.spawnLocationKey, PersistentDataType.INTEGER);
    spawnLocation = new Location(world, rawSpawnX, 200, 0);
  }

  public void findAndSaveNewSpawnLocation() {
    do {
      this.spawnLocation.add(1000, 0, 0);
      this.spawnLocation.getChunk().load(true);
    } while (WorldManager.isOcean(this.spawnLocation.getBlock().getBiome()));

    saveSpawnLocation();
    world.getWorldBorder().setCenter(spawnLocation);
  }
}
