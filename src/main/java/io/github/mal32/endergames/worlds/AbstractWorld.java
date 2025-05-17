package io.github.mal32.endergames.worlds;

import io.github.mal32.endergames.EnderGames;
import org.bukkit.entity.Player;

public abstract class AbstractWorld {
  protected final EnderGames plugin;

  public AbstractWorld(EnderGames plugin) {
    this.plugin = plugin;
  }

  protected abstract void initPlayer(Player player);
}
