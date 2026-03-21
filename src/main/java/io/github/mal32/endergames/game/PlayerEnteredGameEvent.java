package io.github.mal32.endergames.game;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PlayerEnteredGameEvent extends Event {
  private static final HandlerList HANDLER_LIST = new HandlerList();
  private final Player player;

  public PlayerEnteredGameEvent(@NotNull Player player) {
    this.player = player;
  }

  public static @NotNull HandlerList getHandlerList() {
    return HANDLER_LIST;
  }

  public Player getPlayer() {
    return player;
  }

  @Override
  public @NotNull HandlerList getHandlers() {
    return HANDLER_LIST;
  }
}
