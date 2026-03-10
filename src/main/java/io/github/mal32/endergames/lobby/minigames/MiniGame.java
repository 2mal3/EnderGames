package io.github.mal32.endergames.lobby.minigames;

import org.bukkit.entity.Player;

public interface MiniGame {
  void enable();

  void disable();

  boolean isActive(Player player);
}
