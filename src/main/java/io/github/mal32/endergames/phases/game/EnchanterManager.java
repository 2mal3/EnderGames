package io.github.mal32.endergames.phases.game;

import java.util.ArrayList;
import java.util.Random;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.EnchantingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class EnchanterManager extends AbstractTeleportingBlockManager {
  private final ArrayList<Location> enchanterLocations = new ArrayList<>();

  public EnchanterManager(JavaPlugin plugin, Location spwanLocation) {
    super(plugin);

    for (int i = 0; i < 4; i++) {
      enchanterLocations.add(spwanLocation.clone().add(0, 0, 0));
    }
  }

  @Override
  public int getDelay() {
    return 20 * 10;
  }

  public void task() {
    Location randomEnchanter =
        enchanterLocations.get(new Random().nextInt(enchanterLocations.size()));

    destroy(randomEnchanter);
    randomEnchanter = getRandomLocationNearPlayer();
    place(randomEnchanter);
  }

  private void destroy(Location location) {
    location.getWorld().getBlockAt(location).setType(Material.AIR);
    playTeleportEffects(location);
  }

  private void place(Location location) {
    World world = location.getWorld();

    world.getBlockAt(location).setType(Material.ENCHANTING_TABLE);
    playTeleportEffects(location);

    Location blockSpawnLocation = location.getBlock().getLocation().clone();
    blockSpawnLocation.setY(256);
    FallingBlock fallingBlock =
        (FallingBlock) world.spawnEntity(blockSpawnLocation, EntityType.FALLING_BLOCK);
    fallingBlock.setCancelDrop(true);
    fallingBlock.setBlockData(Bukkit.createBlockData(Material.OBSIDIAN));
  }

  @EventHandler
  public void onEnchanterOpen(InventoryOpenEvent event) {
    if (!(event.getInventory() instanceof EnchantingInventory inventory)) return;

    inventory.setSecondary(new ItemStack(Material.LAPIS_LAZULI, 64));
  }

  @EventHandler
  public void onEnchanterClickLapis(InventoryClickEvent event) {
    if (!(event.getInventory() instanceof EnchantingInventory inventory)) return;
    ItemStack item = event.getCurrentItem();
    if (item == null || item.getType() != Material.LAPIS_LAZULI) return;

    item.setAmount(0);
  }
}
