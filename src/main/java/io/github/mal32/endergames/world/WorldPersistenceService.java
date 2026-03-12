package io.github.mal32.endergames.world;

import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public class WorldPersistenceService {
  private final NamespacedKey spawnKey;

  public WorldPersistenceService(Plugin plugin) {
    this.spawnKey = new NamespacedKey(plugin, "spawnLocation");
  }

  public void saveSpawn(World world, int x) {
    world.getPersistentDataContainer().set(spawnKey, PersistentDataType.INTEGER, x);
  }

  public Integer loadSpawn(World world) {
    return world.getPersistentDataContainer().get(spawnKey, PersistentDataType.INTEGER);
  }
}
