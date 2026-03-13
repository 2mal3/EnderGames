package io.github.mal32.endergames.world;

import io.github.mal32.endergames.services.PlayerInWorld;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class GameWorld extends AbstractWorld {
  private final World world;
  private final FindWorldSpawnService spawnService;
  private final WorldPersistenceService persistenceService;
  private final WorldBorderService borderService;

  private Location spawnLocation;

  public GameWorld(
      JavaPlugin plugin,
      GamePlayerInitService playerInitService,
      FindWorldSpawnService spawnService,
      WorldPersistenceService persistenceService,
      WorldBorderService borderService) {
    super(plugin, playerInitService);

    this.world = Bukkit.getWorld("world"); // TODO: service?
    this.spawnService = spawnService;
    this.persistenceService = persistenceService;
    this.borderService = borderService;

    assert world != null;
    Integer savedX = persistenceService.loadSpawn(world);
    if (savedX == null) {
      plugin.getComponentLogger().info("Creating spawn location");
      spawnLocation = spawnService.findNextValidSpawn(new Location(world, 0, 200, 0));
      persistenceService.saveSpawn(world, spawnLocation.getBlockX());
    } else {
      spawnLocation = new Location(world, savedX, 200, 0);
    }
  }

  @Override
  public void setupWorld() {
    world.setDifficulty(Difficulty.EASY);

    world.setGameRule(GameRules.ADVANCE_WEATHER, false);
    world.setGameRule(GameRules.LOCATOR_BAR, false);
    world.setGameRule(GameRules.SPAWN_PHANTOMS, false);
    world.setGameRule(GameRules.ALLOW_ENTERING_NETHER_USING_PORTALS, false);

    borderService.configureBorder(world);

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
  protected boolean isInThisWorld(Player player) {
    return PlayerInWorld.GAME.is(player);
  }

  public void findNewSpawn() {
    spawnLocation = spawnService.findNextValidSpawn(spawnLocation);
    persistenceService.saveSpawn(world, spawnLocation.getBlockX());
    borderService.centerBorder(world, spawnLocation);
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
