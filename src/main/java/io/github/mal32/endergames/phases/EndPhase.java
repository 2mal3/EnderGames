package io.github.mal32.endergames.phases;

import io.github.mal32.endergames.EnderGames;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;

public class EndPhase extends AbstractPhase {
  public EndPhase(EnderGames plugin) {
    super(plugin);
  }

  @Override
  public void start() { // TODO: bossbar?
    Bukkit.getServer()
        .sendMessage(
            Component.text("Game will restart in 30 seconds")
                .color(NamedTextColor.YELLOW)); // TODO: Lobby not new Game
    Bukkit.getScheduler().runTaskLater(plugin, plugin::nextPhase, 20 * 30);
  }

  @Override
  public void stop() {}
}
