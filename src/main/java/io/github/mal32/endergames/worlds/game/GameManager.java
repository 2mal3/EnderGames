package io.github.mal32.endergames.worlds.game;

import io.github.mal32.endergames.EnderGames;
import io.github.mal32.endergames.worlds.AbstractWorld;
import io.github.mal32.endergames.worlds.game.game.GamePhase;
import java.util.List;
import java.util.Objects;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

public class GameManager extends AbstractWorld {
  private Location spawnLocation;
  private final World world = Objects.requireNonNull(Bukkit.getWorld("world_enga_world"));
  private AbstractPhase currentPhase;
  private final NamespacedKey spawnLocationKey;

  public GameManager(EnderGames plugin) {
    super(plugin);
    spawnLocationKey = new NamespacedKey(plugin, "spawnLocation");

    loadSpawnLocation();
    updateSpawn();

    WorldBorder border = spawnLocation.getWorld().getWorldBorder();
    border.setWarningDistance(32);
    border.setWarningTime(60);
    border.setDamageBuffer(1);

    currentPhase = new LoadPhase(plugin, this, spawnLocation);
  }

  private void loadSpawnLocation() {
    if (!world.getPersistentDataContainer().has(spawnLocationKey)) {
      plugin.getComponentLogger().info("Creating spawn location");
      spawnLocation = new Location(world, 0, 150, 0);
      return;
    }

    List<Integer> rawSpawn =
        world
            .getPersistentDataContainer()
            .get(
                spawnLocationKey, PersistentDataType.LIST.listTypeFrom(PersistentDataType.INTEGER));
    spawnLocation = new Location(world, rawSpawn.get(0), 150, rawSpawn.get(1));
  }

  private void updateSpawn() {
    world
        .getPersistentDataContainer()
        .set(
            this.spawnLocationKey,
            PersistentDataType.LIST.listTypeFrom(PersistentDataType.INTEGER),
            List.of((int) spawnLocation.getX(), (int) spawnLocation.getZ()));

    world.getWorldBorder().setCenter(spawnLocation);
  }

  public void startGame() {
    if (!(currentPhase instanceof LoadPhase)) return;

    nextPhase();

    for (Player player : Bukkit.getOnlinePlayers()) {
      plugin.teleportPlayerToGame(player);
    }
  }

  public void nextPhase() {
    currentPhase.disable();

    if (currentPhase instanceof LoadPhase) {
      currentPhase = new StartPhase(plugin, this, spawnLocation);
    } else if (currentPhase instanceof StartPhase) {
      currentPhase = new GamePhase(plugin, this, spawnLocation);
    } else if (currentPhase instanceof GamePhase) {
      currentPhase = new EndPhase(plugin, this, spawnLocation);
    } else if (currentPhase instanceof EndPhase) {
      for (Player p : Bukkit.getOnlinePlayers()) {
        if (!EnderGames.playerIsInGameWorld(p)) return;

        plugin.teleportPlayerToLobby(p);
      }

      findNewSpawnLocation();
      updateSpawn();

      for (Player player : Bukkit.getOnlinePlayers()) {
        plugin.teleportPlayerToLobby(player);
      }

      currentPhase = new LoadPhase(plugin, this, spawnLocation);
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

  @Override
  public void initPlayer(Player player) {
    player.teleport(spawnLocation.clone().add(0, 5, 0));

    if (currentPhase instanceof StartPhase) {
      player.setGameMode(GameMode.ADVENTURE);
    } else {
      player.setGameMode(GameMode.SPECTATOR);
    }
  }

  public static boolean playerIsInGame(Player player) {
    return EnderGames.playerIsInGameWorld(player) && player.getGameMode() != GameMode.SPECTATOR;
  }

  public static Player[] getPlayersInGame() {
    return Bukkit.getOnlinePlayers().stream()
        .filter(GameManager::playerIsInGame)
        .toArray(Player[]::new);
  }
}
