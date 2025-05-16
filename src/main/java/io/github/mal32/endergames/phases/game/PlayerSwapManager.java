package io.github.mal32.endergames.phases.game;

import io.github.mal32.endergames.EnderGames;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class PlayerSwapManager extends AbstractTask {
  public PlayerSwapManager(JavaPlugin plugin) {
    super(plugin);
  }

  @Override
  public int getDelay() {
    return 20 * 60;
  }

  @Override
  public void task() {
    // get two distinct players
    List<Player> players =
        Bukkit.getOnlinePlayers().stream()
            .filter(EnderGames::playerIsPlaying)
            .collect(Collectors.toList());
    if (players.size() < 2) {
      return;
    }

    Player player1 = players.get(new Random().nextInt(players.size()));
    players.remove(player1);
    Player player2 = players.get(new Random().nextInt(players.size()));

    // swap their locations
    Location player1Location = player1.getLocation().clone();
    Location player2Location = player2.getLocation().clone();

    player1.teleport(player2Location);
    player2.teleport(player1Location);

    playerSwapEffects(player1);
    playerSwapEffects(player2);
  }

  private void playerSwapEffects(Player player) {
    Location location = player.getLocation();
    AbstractTeleportingBlockManager.playTeleportEffects(location);

    player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 0, true));
  }
}
