package io.github.mal32.endergames;

import io.github.mal32.endergames.game.GameWorld;
import io.github.mal32.endergames.lobby.LobbyWorld;
import org.bukkit.entity.Player;

public class WorldManager {
  private final LobbyWorld lobbyWorld;
  private final GameWorld gameWorld;

  public WorldManager(EnderGames plugin) {
    this.lobbyWorld = new LobbyWorld(plugin);
    this.gameWorld = new GameWorld(plugin);

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

  public void shutdown() {
    gameWorld.shutdown();
    lobbyWorld.shutdown();
  }
}
