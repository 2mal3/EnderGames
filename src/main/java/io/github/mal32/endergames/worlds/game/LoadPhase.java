package io.github.mal32.endergames.worlds.game;

import io.github.mal32.endergames.EnderGames;
import org.bukkit.Bukkit;
import org.bukkit.Location;

public class LoadPhase extends AbstractPhase {
  public LoadPhase(EnderGames plugin, GameManager manager, Location spawnLocation) {
    super(plugin, manager, spawnLocation);

    Bukkit.getScheduler().runTaskLater(this.plugin, this::loadSpawnChunks, 20 * 5);
  }

  private void loadSpawnChunks() {
    final int loadRadius = 4;
    int loadDelayTicks = 0;
    final int loadDelayIncrease = 5;
    var world = spawnLocation.getWorld();

    for (int x = (int) (spawnLocation.getX() - (loadRadius * 16));
        x < spawnLocation.getX() + (loadRadius * 16);
        x += 16) {
      for (int z = (int) (spawnLocation.getZ() - (loadRadius * 16));
          z < spawnLocation.getZ() + (loadRadius * 16);
          z += 16) {
        final Location location = new Location(world, x, spawnLocation.getY(), z);
        final int currentLoadDelayTicks = loadDelayTicks;
        Bukkit.getScheduler()
            .runTaskLater(
                plugin, () -> world.getChunkAt(location).load(true), currentLoadDelayTicks);
        loadDelayTicks += loadDelayIncrease;
      }
    }
  }

  @Override
  public void disable() {
    super.disable();
  }
}
