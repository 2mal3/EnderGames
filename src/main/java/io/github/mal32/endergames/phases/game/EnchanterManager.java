package io.github.mal32.endergames.phases.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.EnchantingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

public class EnchanterManager extends AbstractModule {
  private final ArrayList<Location> enchanterLocations = new ArrayList<>();
  protected BukkitTask task;

  public EnchanterManager(JavaPlugin plugin, Location spwanLocation) {
    super(plugin);

    for (int i = 0; i < 4; i++) {
      enchanterLocations.add(spwanLocation.clone().add(0, 0, 0));
    }
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

  private void task() {
    Location randomEnchanter =
        enchanterLocations.get(new Random().nextInt(enchanterLocations.size()));

    destroy(randomEnchanter);
    randomEnchanter = getRandomLocationNearPlayer();
    place(randomEnchanter);
  }

  private Location getRandomLocationNearPlayer() {
    List<Player> players =
        Bukkit.getOnlinePlayers().stream()
            .filter(player -> player.getGameMode() == GameMode.SURVIVAL)
            .collect(Collectors.toList());
    if (players.isEmpty()) {
      return null;
    }
    Player player = players.get(new Random().nextInt(players.size()));

    Location location = player.getLocation().getBlock().getLocation().clone();
    // get a random location near the player
    final int range = 32;
    int xOffset = new Random().nextInt(range * 2) - range;
    int zOffset = new Random().nextInt(range * 2) - range;
    location.add(xOffset, 0, zOffset);
    location.setY(location.getWorld().getHighestBlockYAt(location));
    location.add(0, 1, 0);

    return location;
  }

  private void destroy(Location location) {
    location.getWorld().getBlockAt(location).setType(Material.AIR);
    playEffects(location);
  }

  private void place(Location location) {
    World world = location.getWorld();

    world.getBlockAt(location).setType(Material.ENCHANTING_TABLE);
    playEffects(location);

    Location blockSpawnLocation = location.getBlock().getLocation().clone();
    blockSpawnLocation.setY(256);
    FallingBlock fallingBlock =
        (FallingBlock) world.spawnEntity(blockSpawnLocation, EntityType.FALLING_BLOCK);
    fallingBlock.setCancelDrop(true);
    fallingBlock.setBlockData(Bukkit.createBlockData(Material.OBSIDIAN));
  }

  private static void playEffects(Location location) {
    location.getWorld().playSound(location, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 0.5f);
    location.getWorld().spawnParticle(Particle.PORTAL, location, 50, 0, 0, 0);
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
