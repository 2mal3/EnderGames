package io.github.mal32.endergames.worlds.game;

import io.github.mal32.endergames.EnderGames;
import java.util.ArrayList;
import java.util.Random;
import org.bukkit.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.structure.Structure;
import org.bukkit.structure.StructureManager;
import org.bukkit.util.BlockVector;

public class LoadPhase extends AbstractPhase {
  private final ArrayList<BukkitTask> loadTasks = new ArrayList<>();

  public LoadPhase(EnderGames plugin, GameWorld manager, Location spawnLocation) {
    super(plugin, manager, spawnLocation);

    placeSpawnPlatform();

    var startLoadTask =
        Bukkit.getScheduler().runTaskLater(this.plugin, this::loadSpawnChunks, 20 * 5);
    loadTasks.add(startLoadTask);
  }

  public void placeSpawnPlatform() {
    StructureManager manager = Bukkit.getServer().getStructureManager();
    Structure structure = manager.loadStructure(new NamespacedKey("enga", "spawn_platform"));

    BlockVector structureSize = structure.getSize();
    double posX = this.spawnLocation.getBlockX() - (structureSize.getBlockX() / 2.0) + 1;
    double posZ = this.spawnLocation.getBlockZ() - (structureSize.getBlockZ() / 2.0);
    Location location =
        new Location(this.spawnLocation.getWorld(), posX, this.spawnLocation.getY(), posZ);
    structure.place(location, true, StructureRotation.NONE, Mirror.NONE, 0, 1.0f, new Random());
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
