package io.github.mal32.endergames.game.phases;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class GameStartAbortEvent extends Event {
  private static final HandlerList HANDLER_LIST = new HandlerList();

  public static @NotNull HandlerList getHandlerList() {
    return HANDLER_LIST;
  }

  @Override
  public @NotNull HandlerList getHandlers() {
    return HANDLER_LIST;
  }
}
