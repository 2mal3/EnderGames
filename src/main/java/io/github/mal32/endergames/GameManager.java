package io.github.mal32.endergames;

import io.github.mal32.endergames.phases.*;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

public class GameManager {
  private final JavaPlugin plugin;
  private final Location spawn;
  private AbstractPhase phase;

  public GameManager(JavaPlugin plugin, Location spawn) {
    this.plugin = plugin;
    this.spawn = spawn;

    phase = new LobbyPhase(plugin, this, spawn);
    //        phase = new GamePhase(plugin, this, spawn);
  }

  public void nextPhase() {
    phase.stop();
    if (phase instanceof LobbyPhase) {
      phase = new StartPhase(plugin, this, spawn);
    } else if (phase instanceof StartPhase) {
      phase = new GamePhase(plugin, this, spawn);
    } else if (phase instanceof GamePhase) {
      phase = new EndPhase(plugin, this, spawn);
    } else if (phase instanceof EndPhase) {
      phase = new LobbyPhase(plugin, this, spawn);
    }
  }
}
