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
  protected Location location;
  private final World world;
  public int ticksToLive;

  public AbstractTeleportingBlock(EnderGames plugin, Location location, int secondsToLive) {
    this.location = location;
    this.ticksToLive = secondsToLive * 20;
    this.world = location.getWorld();

    if (location.getBlock().getType() != getBlockMaterial()) {
      place();
    }
  }

  private void place() {
    loadChunkIfNotLoaded();

    int y = world.getHighestBlockAt(location, HeightMap.OCEAN_FLOOR).getY();
    location.setY(y + 1);

    Location blockSpawnLocation = this.location.getBlock().getLocation().clone();
    blockSpawnLocation.setY(256);
    FallingBlock fallingBlock =
        (FallingBlock) world.spawnEntity(blockSpawnLocation, EntityType.FALLING_BLOCK);
    fallingBlock.setCancelDrop(true);
    fallingBlock.setBlockData(Bukkit.createBlockData(getFallingBlockMaterial()));

    Block block = world.getBlockAt(location);
    block.setType(getBlockMaterial());
  }

  public void destroy() {
    loadChunkIfNotLoaded();

    if (location.getBlock().getType() != getBlockMaterial()) return;

    location.getWorld().getBlockAt(location).setType(Material.AIR);

    playTeleportEffects();
  }

  private void loadChunkIfNotLoaded() {
    int chunkX = location.blockX() >> 4;
    int chunkZ = location.blockZ() >> 4;
    boolean chunkLoaded = world.isChunkLoaded(chunkX, chunkZ);
    if (!chunkLoaded) {
      world.loadChunk(chunkX, chunkZ, true);
    }
  }

  public void open() {
    ticksToLive = 20 * 20;
  }

  private void playTeleportEffects() {
    location.getWorld().playSound(location, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 0.5f);
    location
        .getWorld()
        .spawnParticle(
            Particle.PORTAL,
            location.getBlock().getLocation().clone().add(0.5, 0.5, 0.5),
            50,
            0,
            0,
            0);
  }

  public abstract Material getBlockMaterial();

  public abstract Material getFallingBlockMaterial();

  public Location getLocation() {
    return location;
  }
}
