package io.github.mal32.endergames.phases.game;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class EnderChestManager extends AbstractTeleportingBlockManager {
  private final List<EnderChest> enderChests = new ArrayList<>();

  public EnderChestManager(JavaPlugin plugin) {
    super(plugin);
  }

  @Override
  public int getDelay() {
    return 20 * 10;
  }

  @EventHandler
  private void onEnderChestInteract(PlayerInteractEvent event) {
    if (event.getAction() != Action.RIGHT_CLICK_BLOCK
        || event.getClickedBlock() == null
        || event.getClickedBlock().getType() != Material.ENDER_CHEST) {
      return;
    }

    Location blockLocation = event.getClickedBlock().getLocation().clone();
    EnderChest enderChest = null;
    for (EnderChest e : enderChests) {
      if (e.getLocation().getX() == blockLocation.getX()
          && e.getLocation().getZ() == blockLocation.getZ()) {
        enderChest = e;
        break;
      }
    }
    if (enderChest == null) {
      enderChest = new EnderChest(plugin, blockLocation);
      enderChests.add(enderChest);
    }

    EnderChest finalEnderChest = enderChest;
    Bukkit.getScheduler()
        .runTask(
            plugin,
            () -> {
              event.getPlayer().openInventory(finalEnderChest.getInventory());
            });
  }

  public void task() {
    if (enderChests.isEmpty()) return;

    EnderChest enderChest = enderChests.get(new Random().nextInt(enderChests.size()));
    Location location = getRandomLocationNearPlayer();
    enderChest.teleport(location);
  }
}

class EnderChest implements InventoryHolder {
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
    AbstractTeleportingBlockManager.playTeleportEffects(location);
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
