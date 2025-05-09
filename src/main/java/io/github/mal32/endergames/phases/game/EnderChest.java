package io.github.mal32.endergames.phases.game;

import java.util.ArrayList;
import java.util.Random;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class EnderChest implements InventoryHolder {
  private final Inventory inventory;
  private Location location;

  public EnderChest(JavaPlugin plugin, Location location) {
    this.inventory = plugin.getServer().createInventory(this, 27);
    this.location = location;

    fill();
  }

  public void teleport(Location location) {
    new ArrayList<>(inventory.getViewers()).forEach(HumanEntity::closeInventory);

    destroy();
    this.location = location;
    place();
    fill();
  }

  public void destroy() {
    Block block = this.location.getBlock();

    if (!location.getChunk().isLoaded()) {
      location.getChunk().load();
    }

    block.setType(Material.AIR);
    effects();
  }

  private void place() {
    World world = location.getWorld();

    Location blockSpawnLocation = this.location.getBlock().getLocation().clone();
    blockSpawnLocation.setY(256);
    FallingBlock fallingBlock =
        (FallingBlock) world.spawnEntity(blockSpawnLocation, EntityType.FALLING_BLOCK);
    fallingBlock.setCancelDrop(true);
    fallingBlock.setBlockData(Bukkit.createBlockData(Material.OBSIDIAN));

    Block block = world.getBlockAt(location);
    block.setType(Material.ENDER_CHEST);

    effects();
  }

  private void effects() {
    location.getWorld().playSound(location, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 0.5f);
    location.getWorld().spawnParticle(Particle.PORTAL, location, 50, 0, 0, 0);
  }

  private void fill() {
    inventory.clear();

    LootTable lootTable = Bukkit.getLootTable(new NamespacedKey("enga", "ender_chest"));
    LootContext.Builder lootContextBuilder = new LootContext.Builder(location);
    LootContext lootContext = lootContextBuilder.build();
    lootTable.fillInventory(this.inventory, new Random(), lootContext);
  }

  @Override
  @NotNull
  public Inventory getInventory() {
    return inventory;
  }

  public Location getLocation() {
    return location;
  }
}
