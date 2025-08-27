package io.github.mal32.endergames.worlds.game;

import io.github.mal32.endergames.EnderGames;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;

public class EndPhase extends AbstractPhase {
  public EndPhase(EnderGames plugin, GameWorld manager, Location spawnLocation) {
    super(plugin, manager, spawnLocation);

    Bukkit.getServer()
        .sendMessage(
            Component.text("Game will reset in 30 seconds")
                .color(NamedTextColor.YELLOW)); // TODO: Lobby not new Game

    int endTimeSeconds = EnderGames.isInDebugMode() ? 5 : 30;
    Bukkit.getScheduler().runTaskLater(plugin, manager::nextPhase, 20 * endTimeSeconds);
  }
}
