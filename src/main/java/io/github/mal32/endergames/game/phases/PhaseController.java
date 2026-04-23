package io.github.mal32.endergames.game.phases;

import io.github.mal32.endergames.EnderGames;
import io.github.mal32.endergames.game.GameWorld;
import io.github.mal32.endergames.services.PlayerInWorld;
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
    if (!(current instanceof LoadPhase) || Bukkit.getOnlinePlayers().isEmpty()) {
      Bukkit.getPluginManager().callEvent(new GameStartAbortEvent());
      return;
    }
    next();
  }

  public void next() {
    current.disable();
    current = current.nextPhase();
    if (current instanceof LoadPhase) {
      for (Player p : PlayerInWorld.GAME.all()) {
        plugin.sendToLobby(p);
      }
    }
  }

  public GameWorld getGameWorld() {
    return gameWorld;
  }

  public void initPlayer(Player player) {
    if (current instanceof StartPhase && PlayerInWorld.GAME.is(player)) {
      player.setGameMode(GameMode.ADVENTURE);
    }
  }

  public boolean isLoading() {
    return (current instanceof LoadPhase);
  }
}
