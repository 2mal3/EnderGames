package io.github.mal32.endergames.lobby;

import io.github.mal32.endergames.EnderGames;
import io.github.mal32.endergames.lobby.items.MenuModule;
import io.github.mal32.endergames.lobby.minigames.EndlessParkour;
import org.bukkit.World;

public final class LobbyModules {
  public static void registerAll(EnderGames plugin) {
    final LobbyManager lobbyManager = plugin.getLobbyManager();
    lobbyManager.registerModule(new MenuModule(plugin));
    lobbyManager.registerModule(new EndlessParkour(plugin));
    final World lobby = plugin.getWorldManager().getLobbyWorld().getWorld();
    lobbyManager.registerModule(new PlayerDifficulty(plugin, lobby));
  }
}
