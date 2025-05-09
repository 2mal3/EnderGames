package io.github.mal32.endergames.phases.game.tasks;

import io.github.mal32.endergames.EnderGames;
import io.github.mal32.endergames.phases.game.EnderChest;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class EnderChestTask extends AbstractTask {
  private final List<EnderChest> enderChests;

  public EnderChestTask(EnderGames plugin, List<EnderChest> enderChests) {
    super(plugin);
    this.enderChests = enderChests;
  }

  @Override
  public int getDelayTicks() {
    return 20 * 10;
  }

  @Override
  public void run() {
    if (enderChests.isEmpty()) return;
    EnderChest enderChest = enderChests.get(new Random().nextInt(enderChests.size()));

    // get a random location near a player unobstructed by blocks
    List<Player> players =
        Bukkit.getOnlinePlayers().stream()
            .filter(player -> player.getGameMode() == GameMode.SURVIVAL)
            .collect(Collectors.toList());
    if (players.isEmpty()) {
      return;
    }
    Player player = players.get(new Random().nextInt(players.size()));

    Location location = player.getLocation().getBlock().getLocation().clone();
    // get a random location near the player
    final int range = 64;
    int xOffset = new Random().nextInt(range * 2) - range;
    int zOffset = new Random().nextInt(range * 2) - range;
    location.add(xOffset, 0, zOffset);
    location.setY(location.getWorld().getHighestBlockYAt(location));
    location.add(0, 1, 0);

    enderChest.teleport(location);
  }
}
