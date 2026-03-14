package io.github.mal32.endergames;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.github.lambdaphoenix.advancementLib.AdvancementAPI;
import io.github.mal32.endergames.game.phases.PhaseController;
import io.github.mal32.endergames.kits.KitRegistry;
import io.github.mal32.endergames.lobby.LobbyManager;
import io.github.mal32.endergames.lobby.LobbyModules;
import io.github.mal32.endergames.lobby.MapManager;
import io.github.mal32.endergames.world.FindWorldSpawnService;
import io.github.mal32.endergames.world.GameWorld;
import io.github.mal32.endergames.world.LobbyWorld;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.util.ArrayList;
import org.bstats.bukkit.Metrics;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class EnderGames extends JavaPlugin {
  private final MapManager mapManager = new MapManager();
  private PhaseController phaseController;
  private LobbyManager lobbyManager;
  private LobbyWorld lobbyWorld;
  private GameWorld gameWorld;
  FindWorldSpawnService spawnService = new FindWorldSpawnService();

  public static boolean isInDebugMode() {
    String debugEnv = System.getenv("EG_DEBUG");
    return debugEnv != null
        && (debugEnv.equalsIgnoreCase("true") || debugEnv.equalsIgnoreCase("1"));
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
  public void onEnable() {
    saveDefaultConfig();

    if (isInDebugMode()) {
      this.getComponentLogger().warn("Debug mode is enabled.");
    } else {
      final int PLUGIN_ID = 25844;
      var metrics = new Metrics(this, PLUGIN_ID);
    }

    this.lobbyWorld = new LobbyWorld(this);
    this.gameWorld = new GameWorld(this, spawnService);
    lobbyWorld.setupWorld();
    gameWorld.setupWorld();
    this.phaseController = new PhaseController(this, gameWorld);

    this.lobbyManager = new LobbyManager(this);
    LobbyModules.registerAll(this);

    KitRegistry.registerKits(this);

    KDScoreboard kdScoreboard = new KDScoreboard(this);

    this.getLifecycleManager()
        .registerEventHandler(
            LifecycleEvents.COMMANDS,
            commands -> commands.registrar().register(endergamesCommand()));

    this.registerKitAdvancements();
  }

  private void registerKitAdvancements() {
    AdvancementAPI advancementAPI = new AdvancementAPI(this);
    for (var kit : KitRegistry.getKits()) {
      kit.registerAdvancement(advancementAPI);
    }
  }

  public void sendToLobby(Player player) {
    lobbyWorld.initPlayer(player);
  }

  public void sendToGame(Player player) {
    gameWorld.initPlayer(player);
  }

  private LiteralCommandNode<CommandSourceStack> endergamesCommand() {
    return Commands.literal("endergames")
        .then(
            Commands.literal("start")
                .requires(sender -> sender.getSender().isOp())
                .executes(
                    ctx -> {
                      phaseController.start();
                      return Command.SINGLE_SUCCESS;
                    }))
        .build();
  }

  @Override
  public void onDisable() {
    lobbyManager.disable();
    gameWorld.disable();
    lobbyWorld.disable();
  }
}
