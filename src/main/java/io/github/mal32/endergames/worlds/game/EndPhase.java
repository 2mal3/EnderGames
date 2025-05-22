package io.github.mal32.endergames.worlds.game;

import io.github.mal32.endergames.EnderGames;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

public class EndPhase extends AbstractPhase {
  public EndPhase(EnderGames plugin, GameManager manager, Location spawnLocation) {
    super(plugin, manager, spawnLocation);

    Bukkit.getServer()
        .sendMessage(
            Component.text("Game will reset in 30 seconds")
                .color(NamedTextColor.YELLOW)); // TODO: Lobby not new Game

    for (Player player : Bukkit.getServer().getOnlinePlayers()) {
      if (!EnderGames.playerIsInGameWorld(player)) continue;

      player.getInventory().clear();
      player.setGameMode(GameMode.SPECTATOR);
      player.setExp(0);

      for (PotionEffect effect : player.getActivePotionEffects()) {
        player.removePotionEffect(effect.getType());
      }
    }

    Bukkit.getScheduler().runTaskLater(plugin, manager::nextPhase, 20 * 30);
  }
}
