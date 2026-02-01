package io.github.mal32.endergames.worlds.game.game;

import io.github.mal32.endergames.EnderGames;
import org.bukkit.Bukkit;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;

public abstract class AbstractTeleportingBlock {
  protected Location currentLocation;
  public boolean hasBeenUsed = false;
  protected boolean hasBeenOpened = false;
  private final EnderGames plugin;

  public AbstractTeleportingBlock(EnderGames plugin, Location location) {
    this.currentLocation = location;
    this.plugin = plugin;
  }

  public void teleport(Location targetLocation) {
    // Every other method that World#isChunkLoaded(x, z) already loads the chunk and leads to
    // performance degradation, so we try load as little as possible here.
    World world = targetLocation.getWorld();
    int targetChunkX = targetLocation.blockX() >> 4;
    int targetChunkZ = targetLocation.blockZ() >> 4;
    int currentChunkX = currentLocation.blockX() >> 4;
    int currentChunkZ = currentLocation.blockZ() >> 4;
    boolean currentChunkLoaded = world.isChunkLoaded(currentChunkX, currentChunkZ);
    boolean targetChunkLoaded = world.isChunkLoaded(targetChunkX, targetChunkZ);

    if (currentChunkLoaded) {
      world.loadChunk(currentChunkX, currentChunkZ, false);
    }
    if (!targetChunkLoaded) {
      world.loadChunk(targetChunkX, targetChunkZ, true);
    }

    destroy();
    this.currentLocation = targetLocation;
    place();

    hasBeenUsed = false;
    hasBeenOpened = false;
  }

  public void place() {
    World world = currentLocation.getWorld();

    int y = world.getHighestBlockAt(currentLocation, HeightMap.OCEAN_FLOOR).getY();
    currentLocation.setY(y + 1);

    Location blockSpawnLocation = this.currentLocation.getBlock().getLocation().clone();
    blockSpawnLocation.setY(256);
    FallingBlock fallingBlock =
        (FallingBlock) world.spawnEntity(blockSpawnLocation, EntityType.FALLING_BLOCK);
    fallingBlock.setCancelDrop(true);
    fallingBlock.setBlockData(Bukkit.createBlockData(getFallingBlockMaterial()));

    Block block = world.getBlockAt(currentLocation);
    block.setType(getBlockMaterial());

    playTeleportEffects();
  }

  public void destroy() {
    if (currentLocation.getBlock().getType() != getBlockMaterial()) return;

    currentLocation.getWorld().getBlockAt(currentLocation).setType(Material.AIR);

    playTeleportEffects();
  }

  public void open() {
    if (hasBeenOpened) return;
    hasBeenOpened = true;

    final int TELEPORT_BLOCK_TIME_SECONDS = 10;
    Bukkit.getScheduler()
        .runTaskLater(plugin, () -> hasBeenUsed = true, 20 * TELEPORT_BLOCK_TIME_SECONDS);
  }

  private void playTeleportEffects() {
    currentLocation.getWorld().playSound(currentLocation, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 0.5f);
    currentLocation
        .getWorld()
        .spawnParticle(
            Particle.PORTAL,
            currentLocation.getBlock().getLocation().clone().add(0.5, 0.5, 0.5),
            50,
            0,
            0,
            0);
  }

  public abstract Material getBlockMaterial();

  public abstract Material getFallingBlockMaterial();

  public Location getLocation() {
    return currentLocation;
  }
}
