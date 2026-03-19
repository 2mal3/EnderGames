package io.github.mal32.endergames;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

public class BlockLocation {
  private World world;
  private int x;
  private int y;
  private int z;

  public BlockLocation(World world, int x, int y, int z) {
    this.world = world;
    this.x = x;
    this.y = y;
    this.z = z;
  }

  public BlockLocation(Location location) {
    this.world = location.getWorld();
    this.x = location.getBlockX();
    this.y = location.getBlockY();
    this.z = location.getBlockZ();
  }

  public World getWorld() {
    return world;
  }

  public int getX() {
    return x;
  }

  public int getY() {
    return y;
  }

  public int getZ() {
    return z;
  }

  public void setX(int x) {
    this.x = x;
  }

  public void setY(int y) {
    this.y = y;
  }

  public void setZ(int z) {
    this.z = z;
  }

  @Override
  public BlockLocation clone() {
    return new BlockLocation(getWorld(), getX(), getY(), getZ());
  }

  public BlockLocation add(int x, int y, int z) {
    this.x += x;
    this.y += y;
    this.z += z;

    return this;
  }

  public Block getBlock() {
    return world.getBlockAt(x, y, z);
  }

  public Location toLocation() {
    return new Location(world, x, y, z);
  }

  public boolean equals(Location location) {
    return location.blockX() == x
        && location.blockY() == y
        && location.blockZ() == z
        && location.getWorld() != null
        && location.getWorld().equals(world);
  }

  public String toString() {
    return String.format("BlockLocation{x=%d, y=%d, z=%d}", x, y, z);
  }
}
