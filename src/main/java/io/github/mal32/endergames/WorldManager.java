package io.github.mal32.endergames;

import io.github.mal32.endergames.world.*;
import io.github.mal32.endergames.world.LobbyWorld;
import org.bukkit.entity.Player;

public class WorldManager {
  private final LobbyWorld lobbyWorld;
  private final GameWorld gameWorld;

  public WorldManager(EnderGames plugin) {

    FindWorldSpawnService spawnService = new FindWorldSpawnService();
    WorldPersistenceService persistenceService = new WorldPersistenceService(plugin);

    this.lobbyWorld = new LobbyWorld(plugin);
    this.gameWorld = new GameWorld(plugin, spawnService, persistenceService);

    lobbyWorld.setupWorld();
    gameWorld.setupWorld();
  }

  public LobbyWorld getLobbyWorld() {
    return lobbyWorld;
  }

  public GameWorld getGameWorld() {
    return gameWorld;
  }

  public void sendToLobby(Player player) {
    lobbyWorld.initPlayer(player);
  }

  public void sendToGame(Player player) {
    gameWorld.initPlayer(player);
  }

  public void disable() {
    gameWorld.disable();
    lobbyWorld.disable();
  }
}
