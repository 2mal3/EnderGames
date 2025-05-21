package io.github.mal32.endergames.worlds.game;

import io.github.mal32.endergames.EnderGames;
import org.bukkit.Location;

public class LoadPhase extends AbstractPhase {
  public LoadPhase(EnderGames plugin, GameManager manager, Location spawnLocation) {
    super(plugin, manager, spawnLocation);
  }
}
