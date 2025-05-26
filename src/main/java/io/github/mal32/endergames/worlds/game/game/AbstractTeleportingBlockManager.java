package io.github.mal32.endergames.worlds.game.game;

import io.github.mal32.endergames.EnderGames;
import io.github.mal32.endergames.worlds.game.GameManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.bukkit.*;
import org.bukkit.entity.Player;

/*
 * This class is an abstract representation of a teleporting block manager.
 * It handles the teleportation and switching of moving blocks like ender chests in specific time intervals.
 */
public abstract class AbstractTeleportingBlockManager<B extends AbstractTeleportingBlock>
    extends AbstractTask {
  protected final List<B> blocks = new ArrayList<>();

  public AbstractTeleportingBlockManager(EnderGames plugin) {
    super(plugin);
  }

  public void task() {
    if (blocks.isEmpty()) return;

    B block = blocks.get(new Random().nextInt(blocks.size()));
    Location location = getRandomLocation();
    block.teleport(location);
  }

  protected Location getRandomLocation() {
    Player[] players = GameManager.getPlayersInGame();
    if (players.length == 0) {
      return null;
    }
    Player player = players[new Random().nextInt(players.length)];

    Location location = player.getLocation().getBlock().getLocation().clone();
    // get a random location near the player
    final int range = 64;
    int xOffset = new Random().nextInt(range * 2) - range;
    int zOffset = new Random().nextInt(range * 2) - range;
    location.add(xOffset, 0, zOffset);
    location.setY(location.getWorld().getHighestBlockYAt(location));
    location.add(0, 1, 0);

    return location;
  }
}
