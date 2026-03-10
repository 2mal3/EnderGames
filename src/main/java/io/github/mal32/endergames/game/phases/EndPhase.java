package io.github.mal32.endergames.game.phases;

import io.github.mal32.endergames.EnderGames;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;

public class EndPhase extends AbstractPhase {
  public EndPhase(EnderGames plugin, PhaseController controller) {
    super(plugin, controller);

    Bukkit.getServer()
        .sendMessage(
            Component.text("Game will reset in 30 seconds")
                .color(NamedTextColor.YELLOW)); // TODO: Lobby not new Game

    int endTimeSeconds = EnderGames.isInDebugMode() ? 5 : 30;
    Bukkit.getScheduler().runTaskLater(plugin, controller::next, 20 * endTimeSeconds);
  }

  @Override
  public AbstractPhase nextPhase() {
    return new LoadPhase(plugin, controller);
  }
}
