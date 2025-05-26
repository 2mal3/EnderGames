package io.github.mal32.endergames.worlds.game.game;

import org.bukkit.Bukkit;
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

  public AbstractTeleportingBlock(Location location) {
    this.location = location;
  }

  public void teleport(Location location) {
    destroy();
    this.location = location;
    place();
  }

  public void place() {
    if (!location.getChunk().isLoaded()) {
      location.getChunk().load();
    }

    World world = location.getWorld();

    Location blockSpawnLocation = this.location.getBlock().getLocation().clone();
    blockSpawnLocation.setY(256);
    FallingBlock fallingBlock =
        (FallingBlock) world.spawnEntity(blockSpawnLocation, EntityType.FALLING_BLOCK);
    fallingBlock.setCancelDrop(true);
    fallingBlock.setBlockData(Bukkit.createBlockData(getFallingBlockMaterial()));

    Block block = world.getBlockAt(location);
    block.setType(getBlockMaterial());

    playTeleportEffects();
  }

  public void destroy() {
    if (!location.getChunk().isLoaded()) {
      location.getChunk().load();
    }

    location.getWorld().getBlockAt(location).setType(Material.AIR);

    playTeleportEffects();
  }

  private void playTeleportEffects() {
    location.getWorld().playSound(location, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 0.5f);
    location.getWorld().spawnParticle(Particle.PORTAL, location, 50, 0, 0, 0);
  }

  public abstract Material getBlockMaterial();

  public abstract Material getFallingBlockMaterial();
}
