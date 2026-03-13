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
    WorldBorderService borderService = new WorldBorderService();
    GamePlayerInitService gamePlayerInitService = new GamePlayerInitService();

    LobbyPlayerInitService lobbyPlayerInitService = new LobbyPlayerInitService();

    this.lobbyWorld = new LobbyWorld(plugin, lobbyPlayerInitService);
    this.gameWorld =
        new GameWorld(
            plugin, gamePlayerInitService, spawnService, persistenceService, borderService);

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
