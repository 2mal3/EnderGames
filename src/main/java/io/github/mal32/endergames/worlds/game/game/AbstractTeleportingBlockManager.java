package io.github.mal32.endergames.worlds.game.game;

import io.github.mal32.endergames.EnderGames;
import io.github.mal32.endergames.worlds.game.GameWorld;
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
    Player[] players = GameWorld.getPlayersInGame();
    if (players.length == 0) return null;

    World world = players[0].getWorld();
    WorldBorder border = world.getWorldBorder();

    Location center = border.getCenter();
    double size = border.getSize() / 2; // Half-size from center in each direction

    Random random = new Random();

    Location location;
    int attempts = 0;
    do {
      double x = center.getX() + (random.nextDouble() * size * 2) - size;
      double z = center.getZ() + (random.nextDouble() * size * 2) - size;
      double y = world.getHighestBlockYAt((int) x, (int) z) + 1;

      location = new Location(world, x, y, z);
      attempts++;
    } while (!border.isInside(location) && attempts < 10);

    return location;
  }

}
