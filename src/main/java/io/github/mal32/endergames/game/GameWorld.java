package io.github.mal32.endergames.game;

import io.github.mal32.endergames.AbstractWorld;
import io.github.mal32.endergames.services.PlayerInWorld;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class GameWorld extends AbstractWorld {
  private final World world;
  private final FindWorldSpawnService spawnService;
  private final NamespacedKey spawnKey;

  private Location spawnLocation;

  public GameWorld(JavaPlugin plugin, FindWorldSpawnService spawnService) {
    super(plugin);

    this.world = Bukkit.getWorld("world"); // TODO: service?
    this.spawnService = spawnService;
    this.spawnKey = new NamespacedKey(plugin, "spawnLocation");

    assert world != null;
    Integer savedX = loadSpawn(world);
    if (savedX == null) {
      plugin.getComponentLogger().info("Creating spawn location");
      spawnLocation = spawnService.findNextValidSpawn(new Location(world, 0, 200, 0));
      saveSpawn(world, spawnLocation.getBlockX());
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

    WorldBorder border = world.getWorldBorder();
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
  protected boolean isInThisWorld(Player player) {
    return PlayerInWorld.GAME.is(player);
  }

  public void findNewSpawn() {
    spawnLocation = spawnService.findNextValidSpawn(spawnLocation);
    saveSpawn(world, spawnLocation.getBlockX());
    WorldBorder border = world.getWorldBorder();
    border.setCenter(spawnLocation);
  }

  private void saveSpawn(World world, int x) {
    world.getPersistentDataContainer().set(spawnKey, PersistentDataType.INTEGER, x);
  }

  private Integer loadSpawn(World world) {
    return world.getPersistentDataContainer().get(spawnKey, PersistentDataType.INTEGER);
  }

  @EventHandler
  public void onPlayerDamage(EntityDamageEvent event) {
    if (!(event.getEntity() instanceof Player player)) return;
    if (!isInThisWorld(player)) return;

    if (player.getGameMode() == GameMode.SPECTATOR) {
      event.setCancelled(true);
    }
  }

  @Override
  public void initPlayer(Player player) {
    super.initPlayer(player);

    player.teleportAsync(spawnLocation.clone().add(0, 5, 0));
    PlayerInWorld.GAME.set(player);
    Bukkit.getPluginManager().callEvent(new PlayerEnteredGameEvent(player));
  }
}
