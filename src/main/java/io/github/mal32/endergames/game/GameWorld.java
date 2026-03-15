package io.github.mal32.endergames.game;

import io.github.mal32.endergames.AbstractWorld;
import io.github.mal32.endergames.EnderGames;
import io.github.mal32.endergames.services.PlayerInWorld;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.persistence.PersistentDataType;

public class GameWorld extends AbstractWorld {
  private final World world;
  private final NamespacedKey spawnLocationKey;
  private Location spawnLocation;

  public GameWorld(EnderGames plugin) {
    super(plugin);
    this.world = Bukkit.getWorld("world");
    this.spawnLocationKey = new NamespacedKey(plugin, "spawnLocation");

    assert world != null;
    if (world.getPersistentDataContainer().has(this.spawnLocationKey)) {
      loadSpawnLocation();
    } else {
      plugin.getComponentLogger().info("Creating spawn location");
      spawnLocation = new Location(world, 0, 200, 0);
      findAndSaveNewSpawnLocation();
    }
  }

  // Why doesn't BiomeTagKeys.IS_OCEAN work?
  // using directly:
  // https://github.com/misode/mcmeta/blob/data/data/minecraft/tags/worldgen/biome/is_ocean.json
  private static boolean isInvalidBiome(Biome biome) {
    return biome.equals(Biome.DEEP_FROZEN_OCEAN)
        || biome.equals(Biome.DEEP_COLD_OCEAN)
        || biome.equals(Biome.DEEP_OCEAN)
        || biome.equals(Biome.DEEP_LUKEWARM_OCEAN)
        || biome.equals(Biome.FROZEN_OCEAN)
        || biome.equals(Biome.OCEAN)
        || biome.equals(Biome.COLD_OCEAN)
        || biome.equals(Biome.LUKEWARM_OCEAN)
        || biome.equals(Biome.WARM_OCEAN)
        || biome.equals(Biome.JUNGLE)
        || biome.equals(Biome.SPARSE_JUNGLE)
        || biome.equals(Biome.BAMBOO_JUNGLE)
        || biome.equals(Biome.RIVER)
        || biome.equals(Biome.STONY_SHORE);
  }

  private void loadSpawnLocation() {
    double rawSpawnX =
        world.getPersistentDataContainer().get(spawnLocationKey, PersistentDataType.INTEGER);
    spawnLocation = new Location(world, rawSpawnX, 200, 0);
  }

  public void findAndSaveNewSpawnLocation() {
    do {
      this.spawnLocation.add(1000, 0, 0);
      this.spawnLocation.getChunk().load(true);
    } while (GameWorld.isInvalidBiome(this.spawnLocation.getBlock().getBiome()));

    this.world
        .getPersistentDataContainer()
        .set(this.spawnLocationKey, PersistentDataType.INTEGER, this.spawnLocation.getBlockX());
    world.getWorldBorder().setCenter(spawnLocation);
  }

  @Override
  public void setupWorld() {
    world.setDifficulty(Difficulty.EASY);

    world.setGameRule(GameRules.ADVANCE_WEATHER, false);
    world.setGameRule(GameRules.LOCATOR_BAR, false);
    world.setGameRule(GameRules.SPAWN_PHANTOMS, false);
    world.setGameRule(GameRules.ALLOW_ENTERING_NETHER_USING_PORTALS, false);
    world.setGameRule(GameRules.SPECTATORS_GENERATE_CHUNKS, false);

    WorldBorder border = this.world.getWorldBorder();
    border.setWarningDistance(32);
    border.setWarningTimeTicks(60 * 20);
    border.setDamageBuffer(1);

    world.setStorm(false);
    world.setThundering(false);
  }

  @Override
  public void resetWorld() {}

  @Override
  public World getWorld() {
    return world;
  }

  @Override
  public Location getSpawnLocation() {
    return spawnLocation.clone();
  }

  @Override
  public void initPlayer(Player player) {
    player.teleportAsync(getSpawnLocation().add(0, 5, 0));

    PlayerInWorld.GAME.set(player);
    plugin.getPhaseController().initPlayer(player);
  }

  @Override
  protected boolean isInThisWorld(Player player) {
    return PlayerInWorld.GAME.is(player);
  }

  @EventHandler
  public void onPlayerDamage(EntityDamageEvent event) {
    if (!(event.getEntity() instanceof Player player)) return;
    if (!isInThisWorld(player)) return;

    if (player.getGameMode() == GameMode.SPECTATOR) {
      event.setCancelled(true);
    }
  }
}
