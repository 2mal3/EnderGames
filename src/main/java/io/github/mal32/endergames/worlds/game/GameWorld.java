package io.github.mal32.endergames.worlds.game;

import io.github.mal32.endergames.EnderGames;
import io.github.mal32.endergames.services.PlayerInWorld;
import io.github.mal32.endergames.services.PlayerState;
import io.github.mal32.endergames.worlds.AbstractWorld;
import io.github.mal32.endergames.worlds.game.game.GamePhase;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;

public class GameWorld extends AbstractWorld {
  private final WorldManager worldManager;
  private AbstractPhase currentPhase;

  public GameWorld(EnderGames plugin) {
    super(plugin);
    this.worldManager = new WorldManager(plugin);

    currentPhase = new LoadPhase(plugin, this, this.worldManager.getSpawnLocation());
  }

  public static boolean playerIsInGame(Player player) {
    return PlayerInWorld.GAME.is(player) && player.getGameMode() != GameMode.SPECTATOR;
  }

  public static Player[] getPlayersInGame() {
    return Bukkit.getOnlinePlayers().stream()
        .filter(GameWorld::playerIsInGame)
        .toArray(Player[]::new);
  }

  public void teleportPlayerToGame(Player player) {
    PlayerInWorld.GAME.set(player);
    initPlayer(player);
  }

  public void startGame() {
    if (!(currentPhase instanceof LoadPhase)) return;

    if (PlayerState.PLAYING.all().length < 1) { // TODO: < 1 only in DEBUG?
      plugin.getLobbyWorld().getMenuManager().onGameStartAbort();
      return;
    }
    plugin.getLobbyWorld().getMenuManager().onGameStart();
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
      for (Player p : PlayerInWorld.GAME.all()) {
        plugin.getLobbyWorld().teleportPlayerToLobby(p);
      }
      plugin.getLobbyWorld().getMenuManager().onGameEnd();

      worldManager.findAndSaveNewSpawnLocation();
      plugin.getComponentLogger().info("Spawn location: {}", spawnLocation);

      currentPhase = new LoadPhase(plugin, this, spawnLocation);
    }
  }

  @Override
  public void initPlayer(Player player) {
    player.teleport(this.worldManager.getSpawnLocation().clone().add(0, 5, 0));

    if (currentPhase instanceof StartPhase && PlayerState.PLAYING.is(player)) {
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
    if (!PlayerInWorld.GAME.is(player)) return;
    if (playerIsInGame(player)) return;

    event.setCancelled(true);
  }
}
