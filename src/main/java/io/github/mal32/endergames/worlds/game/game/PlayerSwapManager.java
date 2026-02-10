package io.github.mal32.endergames.worlds.game.game;

import io.github.mal32.endergames.EnderGames;
import io.github.mal32.endergames.worlds.game.GameWorld;
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
  public int getDelayTicks() {
    int playerCount = GameWorld.getPlayersInGame().length;
    return 20 * 60 * 3 / playerCount;
  }

  @Override
  public void task() {
    // get two distinct players
    List<Player> players = new ArrayList<>(Arrays.stream(GameWorld.getPlayersInGame()).toList());
    if (players.size() < 2) {
      return;
    }

    Player player1 = players.get(new Random().nextInt(players.size()));
    players.remove(player1);
    Player player2 = players.get(new Random().nextInt(players.size()));

    // swap their locations
    Location player1Location = player1.getLocation().clone();
    Location player2Location = player2.getLocation().clone();

    teleportPlayer(player1, player2Location);
    teleportPlayer(player2, player1Location);

    switchIntoFightProtection(player1, player2);
  }

  private void teleportPlayer(Player player, Location location) {
    player.getOpenInventory().close();

    player.teleport(location);

    location.getWorld().playSound(location, Sound.ENTITY_PLAYER_TELEPORT, 1, 0.5f);
    location.getWorld().spawnParticle(Particle.PORTAL, location, 50, 0, 0, 0);

    player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 0, true));
  }

  private void switchIntoFightProtection(Player player1, Player player2) {
    if (FightDetection.playerIsInFight(player1) && !FightDetection.playerIsInFight(player2)) {
      player2.addPotionEffect(
          new PotionEffect(PotionEffectType.RESISTANCE, 20 * 2, 4, true, false, true));
    }
    if (FightDetection.playerIsInFight(player2) && !FightDetection.playerIsInFight(player1)) {
      player1.addPotionEffect(
          new PotionEffect(PotionEffectType.RESISTANCE, 20 * 2, 4, true, false, true));
    }
  }
}
