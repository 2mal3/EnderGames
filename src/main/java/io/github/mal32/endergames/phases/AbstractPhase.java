package io.github.mal32.endergames.phases;

import io.github.mal32.endergames.EnderGames;
import org.bukkit.event.Listener;

public abstract class AbstractPhase implements Listener {
  protected final EnderGames plugin;

  public AbstractPhase(EnderGames plugin) {
    this.plugin = plugin;
  }

  public abstract void start();

  public abstract void stop();
}
