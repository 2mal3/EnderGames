package io.github.mal32.endergames;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.github.mal32.endergames.worlds.game.GameWorld;
import io.github.mal32.endergames.worlds.lobby.LobbyWorld;
import io.github.mal32.endergames.worlds.lobby.MapManager;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.util.ArrayList;
import java.util.Objects;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class EnderGames extends JavaPlugin implements Listener {
  private static final NamespacedKey playerWorldKey = new NamespacedKey("endergames", "world");
  private GameWorld gameWorld;
  private LobbyWorld lobbyWorld;
  private final MapManager mapManager = new MapManager();

  public static boolean playerIsInLobbyWorld(Player player) {
    var world = player.getPersistentDataContainer().get(playerWorldKey, PersistentDataType.STRING);
    return Objects.equals(world, "lobby");
  }

  public static boolean playerIsInGameWorld(Player player) {
    var world = player.getPersistentDataContainer().get(playerWorldKey, PersistentDataType.STRING);
    return Objects.equals(world, "game");
  }

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
  }

  public GameWorld getGameWorld() {
    return gameWorld;
  }

  public LobbyWorld getLobbyWorld() {
    return lobbyWorld;
  }

  public void teleportPlayerToGame(Player player) {
    player.getPersistentDataContainer().set(playerWorldKey, PersistentDataType.STRING, "game");
    gameWorld.initPlayer(player);
  }

  public void teleportPlayerToLobby(Player player) {
    player.getPersistentDataContainer().set(playerWorldKey, PersistentDataType.STRING, "lobby");
    lobbyWorld.initPlayer(player);
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

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    Bukkit.getScheduler().runTaskLater(this, () -> teleportPlayerToLobby(player), 10);
  }

  public static boolean isInDebugMode() {
    String debugEnv = System.getenv("EG_DEBUG");
    return debugEnv != null
        && (debugEnv.equalsIgnoreCase("true") || debugEnv.equalsIgnoreCase("1"));
  }
}
