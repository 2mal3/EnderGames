package io.github.mal32.endergames.worlds.game;

import io.github.mal32.endergames.EnderGames;
import io.github.mal32.endergames.worlds.AbstractWorld;
import io.github.mal32.endergames.worlds.game.game.GamePhase;
import org.bukkit.*;
import org.bukkit.entity.Player;

public class GameManager extends AbstractWorld {
  private AbstractPhase currentPhase;
  private final WorldManager worldManager;

  public GameManager(EnderGames plugin) {
    super(plugin);
    this.worldManager = new WorldManager(plugin);

    this.worldManager.loadSpawnPosition();

    currentPhase = new LoadPhase(plugin, this, this.worldManager.getSpawnLocation());
  }

  public void startGame() {
    if (!(currentPhase instanceof LoadPhase)) return;

    nextPhase();
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
      for (Player p : GameManager.getPlayersInGameWorld()) {
        plugin.teleportPlayerToLobby(p);
      }

      this.worldManager.updateSpawnPosition();

      currentPhase = new LoadPhase(plugin, this, spawnLocation);
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

  public static boolean playerIsInGame(Player player) {
    return EnderGames.playerIsInGameWorld(player) && player.getGameMode() != GameMode.SPECTATOR;
  }

  public static Player[] getPlayersInGame() {
    return Bukkit.getOnlinePlayers().stream()
        .filter(GameManager::playerIsInGame)
        .toArray(Player[]::new);
  }

  public static Player[] getPlayersInGameWorld() {
    return Bukkit.getOnlinePlayers().stream()
        .filter(EnderGames::playerIsInGameWorld)
        .toArray(Player[]::new);
  }
}
