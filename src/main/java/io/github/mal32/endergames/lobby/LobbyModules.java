package io.github.mal32.endergames.lobby;

import io.github.mal32.endergames.EnderGames;
import io.github.mal32.endergames.lobby.items.MenuModule;
import io.github.mal32.endergames.lobby.minigames.EndlessParkour;

public final class LobbyModules {
  public static void registerAll(EnderGames plugin) {
    LobbyManager lobbyManager = plugin.getLobbyManager();
    lobbyManager.registerModule(new MenuModule(plugin));
    lobbyManager.registerModule(new EndlessParkour(plugin));
  }
}
