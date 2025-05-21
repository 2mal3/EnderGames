package io.github.mal32.endergames.worlds.game.game;

import io.github.mal32.endergames.EnderGames;
import io.github.mal32.endergames.worlds.game.GameManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PlayerSwapManager extends AbstractTask {
  public PlayerSwapManager(EnderGames plugin) {
    super(plugin);
  }

  @Override
  public int getDelay() {
    return 20 * 60;
  }

  @Override
  public void task() {
    // get two distinct players
    List<Player> players = new ArrayList<>(Arrays.stream(GameManager.getPlayersInGame()).toList());
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
    location.getWorld().playSound(location, Sound.ENTITY_PLAYER_TELEPORT, 1, 0.5f);
    location.getWorld().spawnParticle(Particle.PORTAL, location, 50, 0, 0, 0);

    player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 0, true));
  }
}
