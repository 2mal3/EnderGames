package io.github.mal32.endergames.worlds.game;

import io.github.mal32.endergames.EnderGames;
import io.github.mal32.endergames.worlds.AbstractWorld;
import io.github.mal32.endergames.worlds.game.game.GamePhase;
import java.util.Objects;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.persistence.PersistentDataType;

public class GameWorld extends AbstractWorld {
  private final WorldManager worldManager;
  private AbstractPhase currentPhase;

  public GameWorld(EnderGames plugin) {
    super(plugin);
    this.worldManager = new WorldManager(plugin);

    currentPhase = new LoadPhase(plugin, this, this.worldManager.getSpawnLocation());
  }

  public static boolean playerIsInGame(Player player) {
    return playerIsInGameWorld(player) && player.getGameMode() != GameMode.SPECTATOR;
  }

  public static boolean playerIsInGameWorld(Player player) {
    var world =
        player
            .getPersistentDataContainer()
            .get(EnderGames.playerWorldKey, PersistentDataType.STRING);
    return Objects.equals(world, "game");
  }

  public void teleportPlayerToGame(Player player) {
    player
        .getPersistentDataContainer()
        .set(EnderGames.playerWorldKey, PersistentDataType.STRING, "game");
    initPlayer(player);
  }

  public static Player[] getPlayersInGame() {
    return Bukkit.getOnlinePlayers().stream()
        .filter(GameWorld::playerIsInGame)
        .toArray(Player[]::new);
  }

  public static Player[] getPlayersInGameWorld() {
    return Bukkit.getOnlinePlayers().stream()
        .filter(GameWorld::playerIsInGameWorld)
        .toArray(Player[]::new);
  }

  public void startGame() {
    if (!(currentPhase instanceof LoadPhase)) return;

    nextPhase();
  }

  public boolean isGameRunning() {
    return currentPhase instanceof GamePhase;
  }

  public void nextPhase() {
    currentPhase.disable();

    Location spawnLocation = this.worldManager.getSpawnLocation();

    if (currentPhase instanceof LoadPhase) {
      currentPhase = new StartPhase(plugin, this, spawnLocation);
    } else if (currentPhase instanceof StartPhase) {
      currentPhase = new GamePhase(plugin, this, spawnLocation);
    } else if (currentPhase instanceof GamePhase) {
      currentPhase = new EndPhase(plugin, this, spawnLocation);
    } else if (currentPhase instanceof EndPhase) {
      for (Player p : GameWorld.getPlayersInGameWorld()) {
        plugin.getLobbyWorld().teleportPlayerToLobby(p);
      }

      worldManager.findAndSaveNewSpawnLocation();
      plugin.getComponentLogger().info("Spawn location: " + spawnLocation);

      currentPhase = new LoadPhase(plugin, this, spawnLocation);
      plugin.getLobbyWorld().onGameEnd();
    }
  }

  @Override
  public void initPlayer(Player player) {
    player.teleport(this.worldManager.getSpawnLocation().clone().add(0, 5, 0));

    if (currentPhase instanceof StartPhase) {
      player.setGameMode(GameMode.ADVENTURE);
    } else {
      player.setGameMode(GameMode.SPECTATOR);
    }
  }

  public WorldManager getWorldManager() {
    return worldManager;
  }

  @EventHandler
  public void onPlayerDamage(EntityDamageEvent event) {
    if (!(event.getEntity() instanceof Player player)) return;
    if (!playerIsInGameWorld(player)) return;
    if (playerIsInGame(player)) return;

    event.setCancelled(true);
  }
}
