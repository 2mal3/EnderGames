package io.github.mal32.endergames.game.phases;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class GameStartingEvent extends Event {
  private static final HandlerList HANDLER_LIST = new HandlerList();
  private final Collection<Player> participants;

  public GameStartingEvent(Collection<Player> players) {
    super();
    this.participants = Collections.unmodifiableCollection(Objects.requireNonNull(players));
  }

  public static @NotNull HandlerList getHandlerList() {
    return HANDLER_LIST;
  }

  @Override
  public @NotNull HandlerList getHandlers() {
    return HANDLER_LIST;
  }

  public @NotNull Collection<Player> getParticipants() {
    return participants;
  }
}
