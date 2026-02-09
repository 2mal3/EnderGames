package io.github.mal32.endergames;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.github.lambdaphoenix.advancementLib.AdvancementAPI;
import io.github.mal32.endergames.kits.AbstractKit;
import io.github.mal32.endergames.worlds.game.GameWorld;
import io.github.mal32.endergames.worlds.lobby.LobbyWorld;
import io.github.mal32.endergames.worlds.lobby.MapManager;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.util.ArrayList;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class EnderGames extends JavaPlugin implements Listener {
  public static final NamespacedKey playerWorldKey = new NamespacedKey("endergames", "world");
  private GameWorld gameWorld;
  private LobbyWorld lobbyWorld;
  private final MapManager mapManager = new MapManager();

  public void sendNewMapPixelsToLobby(ArrayList<MapPixel> pixelBatch) {
    mapManager.addToMapWall(pixelBatch);
  }

  @Override
  public void onEnable() {
    final int PLUGIN_ID = 25844;
    var metrics = new Metrics(this, PLUGIN_ID);

    if (isInDebugMode()) {
      this.getComponentLogger().warn("Debug mode is enabled.");
    }

    gameWorld = new GameWorld(this);
    lobbyWorld = new LobbyWorld(this);

    this.getLifecycleManager()
        .registerEventHandler(
            LifecycleEvents.COMMANDS,
            commands -> commands.registrar().register(endergamesCommand()));

    Bukkit.getPluginManager().registerEvents(this, this);

    this.registerKitAdvancements();
  }

  private void registerKitAdvancements() {
    AdvancementAPI advancementAPI = new AdvancementAPI(this);
    for (var kit : AbstractKit.getKits(this)) {
      kit.registerAdvancement(advancementAPI);
    }
  }

  public GameWorld getGameWorld() {
    return gameWorld;
  }

  public LobbyWorld getLobbyWorld() {
    return lobbyWorld;
  }

  private LiteralCommandNode<CommandSourceStack> endergamesCommand() {
    return Commands.literal("endergames")
        .then(
            Commands.literal("start")
                .requires(sender -> sender.getSender().isOp())
                .executes(
                    ctx -> {
                      gameWorld.startGame();
                      return Command.SINGLE_SUCCESS;
                    }))
        .build();
  }

  public static boolean isInDebugMode() {
    String debugEnv = System.getenv("EG_DEBUG");
    return debugEnv != null
        && (debugEnv.equalsIgnoreCase("true") || debugEnv.equalsIgnoreCase("1"));
  }
}
