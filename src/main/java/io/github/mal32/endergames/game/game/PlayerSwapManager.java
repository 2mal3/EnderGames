package io.github.mal32.endergames.game.game;

import io.github.mal32.endergames.EnderGames;
import io.github.mal32.endergames.game.phases.PhaseController;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PlayerSwapManager extends AbstractTask {
  private final int SWAP_COOLDOWN = 60 * 3;
  private final int CLOCK_SPEED_SECONDS = 5;
  private int swapCooldown = SWAP_COOLDOWN;

  public PlayerSwapManager(EnderGames plugin) {
    super(plugin);
  }

  @Override
  public int getDelayTicks() {
    return 20 * CLOCK_SPEED_SECONDS;
  }

  @Override
  public void task() {
    swapCooldown -= 5 * PhaseController.getPlayersInGame().length;
    if (swapCooldown <= 0) {
      swapCooldown = SWAP_COOLDOWN;
      swap();
    }
  }

  private void swap() {
    // get two distinct players
    List<Player> players =
        new ArrayList<>(Arrays.stream(PhaseController.getPlayersInGame()).toList());
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

    Entity vehicle = player.getVehicle();
    if (vehicle != null) {
      vehicle.removePassenger(player);
      vehicle.teleport(location);
      player.teleport(location);
      vehicle.addPassenger(player);
    } else {
      player.teleport(location);
    }

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
