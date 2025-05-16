package io.github.mal32.endergames.worlds.game;

import io.github.mal32.endergames.EnderGames;
import io.github.mal32.endergames.worlds.game.game.GamePhase;
import java.util.List;
import java.util.Objects;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.persistence.PersistentDataType;

public class GameManager {
  private Location spawnLocation;
  private final World world = Objects.requireNonNull(Bukkit.getWorld("world_enga_world"));
  private final EnderGames plugin;
  private AbstractPhase currentPhase;
  private final NamespacedKey spawnLocationKey;

  public GameManager(EnderGames plugin) {
    this.plugin = plugin;
    spawnLocationKey = new NamespacedKey(plugin, "spawnLocation");

    loadSpawnLocation();

    WorldBorder border = spawnLocation.getWorld().getWorldBorder();
    border.setWarningDistance(32);
    border.setWarningTime(60);
    border.setDamageBuffer(1);

    currentPhase = new EmptyPhase(plugin, this, spawnLocation);
  }

  private void loadSpawnLocation() {
    if (!world.getPersistentDataContainer().has(spawnLocationKey)) {
      plugin.getComponentLogger().info("Creating spawn location");
      spawnLocation = new Location(world, 0, 150, 0);
      updateSpawn();
    }

    List<Integer> rawSpawn =
        world
            .getPersistentDataContainer()
            .get(
                spawnLocationKey, PersistentDataType.LIST.listTypeFrom(PersistentDataType.INTEGER));
    spawnLocation = new Location(world, rawSpawn.get(0), 150, rawSpawn.get(1));
  }

  private void updateSpawn() {
    World world = spawnLocation.getWorld();

    world
        .getPersistentDataContainer()
        .set(
            this.spawnLocationKey,
            PersistentDataType.LIST.listTypeFrom(PersistentDataType.INTEGER),
            List.of((int) spawnLocation.getX(), (int) spawnLocation.getZ()));

    world.getWorldBorder().setCenter(spawnLocation);
  }

  protected void nextPhase() {
    currentPhase.disable();

    if (currentPhase instanceof EmptyPhase) {
      currentPhase = new StartPhase(plugin, this, spawnLocation);
    } else if (currentPhase instanceof StartPhase) {
      currentPhase = new GamePhase(plugin, this, spawnLocation);
    } else if (currentPhase instanceof GamePhase) {
      currentPhase = new EndPhase(plugin, this, spawnLocation);
    } else if (currentPhase instanceof EndPhase) {
      findNewSpawnLocation();

      currentPhase = new EmptyPhase(plugin, this, spawnLocation);
    }
  }

  private void findNewSpawnLocation() {
    Location spawnLocationCandidate = spawnLocation.clone();

    do {
      spawnLocationCandidate.add(1000, 0, 0);
      spawnLocationCandidate.getChunk().load(true);
    } while (isOcean(spawnLocationCandidate.getBlock().getBiome()));

    spawnLocation.setX(spawnLocationCandidate.getX());
  }

  // Why doesn't BiomeTagKeys.IS_OCEAN work?
  // using directly:
  // https://github.com/misode/mcmeta/blob/data/data/minecraft/tags/worldgen/biome/is_ocean.json
  private boolean isOcean(Biome biome) {
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
}
