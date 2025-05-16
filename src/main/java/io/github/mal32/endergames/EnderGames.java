package io.github.mal32.endergames;

import io.github.mal32.endergames.worlds.game.GameManager;
import io.github.mal32.endergames.worlds.lobby.LobbyPhase;
import java.util.Objects;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class EnderGames extends JavaPlugin implements Listener {
  private final GameManager gameWorld = new GameManager(this);
  private final LobbyPhase lobbyWorld = new LobbyPhase(this);

  public static boolean playerIsInLobbyWorld(Player player) {
    var world =
        player
            .getPersistentDataContainer()
            .get(new NamespacedKey("enga", "world"), PersistentDataType.STRING);
    return Objects.equals(world, "lobby");
  }

  public static boolean playerIsInGameWorld(Player player) {
    var world =
        player
            .getPersistentDataContainer()
            .get(new NamespacedKey("enga", "world"), PersistentDataType.STRING);
    return Objects.equals(world, "game");
  }

  @Override
  public void onEnable() {
    final int PLUGIN_ID = 25844;
    Metrics metrics = new Metrics(this, PLUGIN_ID);
  }
}
