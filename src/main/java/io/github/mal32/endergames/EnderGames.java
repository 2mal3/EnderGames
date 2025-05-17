package io.github.mal32.endergames;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.github.mal32.endergames.worlds.game.GameManager;
import io.github.mal32.endergames.worlds.lobby.LobbyPhase;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class EnderGames extends JavaPlugin implements Listener {
  private GameManager gameWorld;
  private LobbyPhase lobbyWorld;
  private static final NamespacedKey worldKey = new NamespacedKey("endergames", "world");

  @Override
  public void onEnable() {
    final int PLUGIN_ID = 25844;

    gameWorld = new GameManager(this);
    lobbyWorld = new LobbyPhase(this);

    this.getLifecycleManager()
        .registerEventHandler(
            LifecycleEvents.COMMANDS,
            commands -> commands.registrar().register(endergamesCommand()));

    Bukkit.getPluginManager().registerEvents(this, this);
  }

  public static boolean playerIsInLobbyWorld(Player player) {
    var world = player.getPersistentDataContainer().get(worldKey, PersistentDataType.STRING);
    return Objects.equals(world, "lobby");
  }

  public static boolean playerIsInGameWorld(Player player) {
    var world = player.getPersistentDataContainer().get(worldKey, PersistentDataType.STRING);
    return Objects.equals(world, "game");
  }

  public void teleportPlayerToGame(Player player) {
    player.getPersistentDataContainer().set(worldKey, PersistentDataType.STRING, "game");
    gameWorld.initPlayer(player);
  }

  public void teleportPlayerToLobby(Player player) {
    player.getPersistentDataContainer().set(worldKey, PersistentDataType.STRING, "lobby");
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
    teleportPlayerToLobby(event.getPlayer());
  }
}
