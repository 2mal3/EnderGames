package io.github.mal32.endergames;

import io.github.lambdaphoenix.advancementLib.AdvancementAPI;
import io.github.mal32.endergames.game.FindWorldSpawnService;
import io.github.mal32.endergames.game.GameWorld;
import io.github.mal32.endergames.game.phases.PhaseController;
import io.github.mal32.endergames.kitsystem.api.*;
import io.github.mal32.endergames.kitsystem.registry.KitRegistry;
import io.github.mal32.endergames.lobby.LobbyManager;
import io.github.mal32.endergames.lobby.LobbyWorld;
import io.github.mal32.endergames.lobby.MapManager;
import java.util.ArrayList;
import org.bstats.bukkit.Metrics;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class EnderGames extends JavaPlugin {
  private final MapManager mapManager = new MapManager();
  private final FindWorldSpawnService spawnService = new FindWorldSpawnService();
  private PhaseController phaseController;
  private LobbyManager lobbyManager;
  private LobbyWorld lobbyWorld;
  private GameWorld gameWorld;

  private KitSystem kitSystem;

  public static boolean isInDebugMode() {
    String debugEnv = System.getenv("EG_DEBUG");
    return debugEnv != null
        && (debugEnv.equalsIgnoreCase("true") || debugEnv.equalsIgnoreCase("1"));
  }

  public KitSystem getKitSystem() {
    return kitSystem;
  }

  public LobbyWorld getLobbyWorld() {
    return lobbyWorld;
  }

  public void changeMapPixelsInLobby(
      ArrayList<MapPixel> changedMapPixels, boolean forceFullUpdate) {
    mapManager.addToMapWall(changedMapPixels, forceFullUpdate);
  }

  public PhaseController getPhaseController() {
    return phaseController;
  }

  public LobbyManager getLobbyManager() {
    return lobbyManager;
  }

  @Override
  public void onLoad() {
    this.kitSystem = new KitSystem(this);
    KitRegistry.registerAll(this);
  }

  @Override
  public void onEnable() {
    saveDefaultConfig();

    if (isInDebugMode()) {
      this.getComponentLogger().warn("Debug mode is enabled.");
    } else {
      final int PLUGIN_ID = 25844;
      var metrics = new Metrics(this, PLUGIN_ID);
    }

    this.lobbyManager = new LobbyManager(this);
    this.lobbyWorld = new LobbyWorld(this);
    this.gameWorld = new GameWorld(this, spawnService);
    lobbyWorld.setupWorld();
    gameWorld.setupWorld();
    this.phaseController = new PhaseController(this, gameWorld);

    kitSystem.enable();

    LobbyManager.registerDefaultModules(this);

    KDScoreboard kdScoreboard = new KDScoreboard(this);

    this.registerKitAdvancements();
  }

  private void registerKitAdvancements() {
    AdvancementAPI advancementAPI = new AdvancementAPI(this);
    for (AbstractKit kit : kitSystem.manager().all()) {
      if (kit instanceof CustomKitUnlockAdvancement) {
        ((CustomKitUnlockAdvancement) kit).registerAdvancement(advancementAPI);
      }
    }
  }

  public void sendToLobby(Player player) {
    lobbyWorld.initPlayer(player);
  }

  public void sendToGame(Player player) {
    gameWorld.initPlayer(player);
  }

  @Override
  public void onDisable() {
    if (kitSystem != null) kitSystem.disable();
    if (lobbyManager != null) lobbyManager.disable();
    if (gameWorld != null) gameWorld.disable();
    if (lobbyWorld != null) lobbyWorld.disable();
  }
}
