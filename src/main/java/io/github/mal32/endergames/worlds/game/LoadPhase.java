package io.github.mal32.endergames.worlds.game;

import io.github.mal32.endergames.EnderGames;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;

public class LoadPhase extends AbstractPhase {
  public LoadPhase(EnderGames plugin, GameManager manager, Location spawnLocation) {
    super(plugin, manager, spawnLocation);

    loadSpawnChunks();
  }

  private void loadSpawnChunks() {
    World world = spawnLocation.getWorld();

    for (Chunk chunk : world.getLoadedChunks()) {
      chunk.removePluginChunkTicket(plugin);
    }

    final int loadRadius = 4;
    for (int x = (int) (spawnLocation.getX() - (2 * 16));
        x < spawnLocation.getX() + (loadRadius * 16);
        x += 16) {
      for (int z = (int) (spawnLocation.getZ() - (2 * 16));
          z < spawnLocation.getZ() + (loadRadius * 16);
          z += 16) {
        world
            .getChunkAt(new Location(world, x, spawnLocation.getY(), z))
            .addPluginChunkTicket(plugin);
      }
    }
  }
}
