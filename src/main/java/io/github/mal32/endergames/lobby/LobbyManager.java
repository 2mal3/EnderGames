package io.github.mal32.endergames.lobby;

import io.github.mal32.endergames.EnderGames;
import io.github.mal32.endergames.lobby.items.MenuModule;
import io.github.mal32.endergames.lobby.minigames.EndlessParkour;
import io.github.mal32.endergames.lobby.minigames.parkour.ParkourGame;
import io.github.mal32.endergames.world.PlayerEnteredLobbyEvent;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class LobbyManager implements Listener {
  private final List<LobbyModule> modules = new ArrayList<>();

  public LobbyManager(JavaPlugin plugin) {
    Bukkit.getPluginManager().registerEvents(this, plugin);
  }

  public void registerModule(LobbyModule module) {
    modules.add(module);
    module.onRegister();
  }

  public void registerAllModules(EnderGames plugin) {
    registerModule(new MenuModule(plugin));
    registerModule(new EndlessParkour(plugin));
    registerModule(new PlayerDifficulty(plugin, plugin.getLobbyWorld().getWorld()));
    registerModule(new ParkourGame(plugin));
  }

  @EventHandler
  public void onLobbyEnter(PlayerEnteredLobbyEvent event) {
    Player player = event.getPlayer();

    for (LobbyModule module : modules) {
      module.onPlayerJoinLobby(player);
    }
  }

  public void disable() {
    HandlerList.unregisterAll(this);
    for (LobbyModule module : modules) {
      module.onDisable();
    }
  }
}
