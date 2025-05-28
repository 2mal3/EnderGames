package io.github.mal32.endergames.worlds.game;

import io.github.mal32.endergames.EnderGames;
import java.util.ArrayList;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitTask;

public class LoadPhase extends AbstractPhase {
  private final ArrayList<BukkitTask> loadTasks = new ArrayList<>();

  public LoadPhase(EnderGames plugin, GameManager manager, Location spawnLocation) {
    super(plugin, manager, spawnLocation);

    var startLoadTask =
        Bukkit.getScheduler().runTaskLater(this.plugin, this::loadSpawnChunks, 20 * 5);
    loadTasks.add(startLoadTask);
  }

  private void loadSpawnChunks() {
    final int loadRadius = 8;
    var location = spawnLocation.clone();

    int invert = 1;
    for (int i = 0; i < loadRadius; i++) {
      for (int k = 0; k < i; k++) {
        location.add(invert * 16, 0, 0);
        scheduleChunkLoad(location.clone());
      }
      for (int k = 0; k < i; k++) {
        location.add(0, 0, invert * 16);
        scheduleChunkLoad(location.clone());
      }

      invert *= -1;
    }
  }

  int loadDelayTicks = 0;
  final int loadDelayIncrease = 5;

  private void scheduleChunkLoad(Location location) {
    var chunkLoadTask =
        Bukkit.getScheduler()
            .runTaskLater(
                plugin, () -> location.getWorld().getChunkAt(location).load(true), loadDelayTicks);
    loadTasks.add(chunkLoadTask);

    loadDelayTicks += loadDelayIncrease;
  }

  @Override
  public void disable() {
    super.disable();

    for (BukkitTask task : loadTasks) {
      if (task == null || task.isCancelled()) continue;

      task.cancel();
    }
  }
}
