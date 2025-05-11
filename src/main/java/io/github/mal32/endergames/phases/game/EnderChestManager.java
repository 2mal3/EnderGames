package io.github.mal32.endergames.phases.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

public class EnderChestManager extends AbstractModule {
  private List<EnderChest> enderChests = new ArrayList<>();
  private BukkitTask task;

  public EnderChestManager(JavaPlugin plugin) {
    super(plugin);
  }

  @Override
  public void enable() {
    super.enable();

    BukkitScheduler scheduler = plugin.getServer().getScheduler();
    task = scheduler.runTaskTimer(plugin, this::task, 20 * 10, 20 * 10);
  }

  @Override
  public void disable() {
    super.disable();

    task.cancel();
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

  private void task() {
    if (enderChests.isEmpty()) return;
    EnderChest enderChest = enderChests.get(new Random().nextInt(enderChests.size()));

    // get a random location near a player unobstructed by blocks
    List<Player> players =
        Bukkit.getOnlinePlayers().stream()
            .filter(player -> player.getGameMode() == GameMode.SURVIVAL)
            .collect(Collectors.toList());
    if (players.isEmpty()) {
      return;
    }
    Player player = players.get(new Random().nextInt(players.size()));

    Location location = player.getLocation().getBlock().getLocation().clone();
    // get a random location near the player
    final int range = 64;
    int xOffset = new Random().nextInt(range * 2) - range;
    int zOffset = new Random().nextInt(range * 2) - range;
    location.add(xOffset, 0, zOffset);
    location.setY(location.getWorld().getHighestBlockYAt(location));
    location.add(0, 1, 0);

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
