package io.github.mal32.endergames.lobby.minigames;

import io.github.mal32.endergames.EnderGames;
import java.util.ArrayList;
import java.util.List;

public class MiniGameController {
  public final EnderGames plugin;
  private final List<MiniGame> miniGames = new ArrayList<>();

  public MiniGameController(EnderGames plugin) {
    this.plugin = plugin;
  }

  public void register(MiniGame game) {
    miniGames.add(game);
    game.enable();
  }

  public void disableAll() {
    for (MiniGame game : miniGames) {
      game.disable();
    }
  }
}
