package io.github.mal32.endergames.phases;

import io.github.mal32.endergames.EnderGames;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class EndPhase extends AbstractPhase {
  public EndPhase(EnderGames plugin, Location spawn) {
    super(plugin, spawn);

    Bukkit.getServer()
        .sendMessage(
            Component.text("Game will restart in 30 seconds").color(NamedTextColor.YELLOW));
    Bukkit.getScheduler().runTaskLater(plugin, plugin::nextPhase, 20 * 30);
  }

  @EventHandler
  private void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    player.setGameMode(GameMode.SPECTATOR);
  }
}
