package io.github.mal32.endergames.game.phases;

import io.github.mal32.endergames.EnderGames;
import io.github.mal32.endergames.game.GameWorld;
import io.github.mal32.endergames.services.PlayerInWorld;
import io.github.mal32.endergames.services.PlayerState;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

public class PhaseController {
  private final EnderGames plugin;
  private final GameWorld gameWorld;

  private AbstractPhase current;

  public PhaseController(EnderGames plugin, GameWorld gameWorld) {
    this.plugin = plugin;
    this.gameWorld = gameWorld;

    this.current = new LoadPhase(plugin, this);
  }

  // TODO: move
  public static boolean playerIsInGame(Player player) {
    return PlayerInWorld.GAME.is(player) && player.getGameMode() != GameMode.SPECTATOR;
  }

  // TODO: move
  public static Player[] getPlayersInGame() {
    return Bukkit.getOnlinePlayers().stream()
        .filter(PhaseController::playerIsInGame)
        .toArray(Player[]::new);
  }

  public void start() {
    if (!(current instanceof LoadPhase)
        || PlayerState.PLAYING.all().length < 1) { // TODO: < 1 only in DEBUG?
      plugin.getMenuManager().onGameStartAbort();
      return;
    }
    plugin.getMenuManager().onGameStart();
    next();
  }

  public void next() {
    current.disable();
    current = current.nextPhase();
    if (current instanceof LoadPhase) {
      for (Player p : PlayerInWorld.GAME.all()) {
        plugin.getWorldManager().sendToLobby(p);
      }
    }
  }

  public GameWorld getGameWorld() {
    return gameWorld;
  }

  public void initPlayer(Player player) {
    if (current instanceof StartPhase && PlayerState.PLAYING.is(player)) {
      player.setGameMode(GameMode.ADVENTURE);
    } else {
      player.setGameMode(GameMode.SPECTATOR);
    }
  }

  public boolean isLoading() {
    return (current instanceof LoadPhase);
  }
}
