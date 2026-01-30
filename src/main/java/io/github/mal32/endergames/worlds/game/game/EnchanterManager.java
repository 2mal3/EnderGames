package io.github.mal32.endergames.worlds.game.game;

import io.github.mal32.endergames.EnderGames;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.EnchantingInventory;
import org.bukkit.inventory.ItemStack;

public class EnchanterManager extends AbstractTeleportingBlockManager<Enchanter> {
  public EnchanterManager(EnderGames plugin, Location spawnLocation) {
    super(plugin, spawnLocation);
  }

  @Override
  public int getBlockTeleportDelayTicks() {
    return 20 * 50;
  }

  @Override
  protected int blockCount() {
    return 15;
  }

  @Override
  protected Enchanter getNewBlock(Location location) {
    return new Enchanter(plugin, location);
  }

  @EventHandler
  public void onEnchanterOpen(InventoryOpenEvent event) {
    if (!(event.getInventory() instanceof EnchantingInventory inventory)) return;

    inventory.setSecondary(new ItemStack(Material.LAPIS_LAZULI, 64));

    // Mark the enchanter as used when a player opens it
    var location = event.getInventory().getLocation();
    if (location == null) return;
    Enchanter enchanter = getBlockAtLocation(location);
    if (enchanter != null) {
      enchanter.open();
    }
  }

  @EventHandler
  public void onEnchanterClickLapis(InventoryClickEvent event) {
    if (!(event.getInventory() instanceof EnchantingInventory)) return;
    ItemStack item = event.getCurrentItem();
    if (item == null || item.getType() != Material.LAPIS_LAZULI) return;

    event.setCancelled(true);
  }

  @EventHandler
  public void onEnchanterClose(InventoryCloseEvent event) {
    if (!(event.getInventory() instanceof EnchantingInventory inventory)) return;
    ItemStack item = inventory.getSecondary();
    if (item == null || item.getType() != Material.LAPIS_LAZULI) return;

    item.setAmount(0);
  }
}

class Enchanter extends AbstractTeleportingBlock {
  public Enchanter(EnderGames plugin, Location location) {
    super(plugin, location);
  }

  @Override
  public Material getBlockMaterial() {
    return Material.ENCHANTING_TABLE;
  }

  @Override
  public Material getFallingBlockMaterial() {
    return Material.OBSIDIAN;
  }
}
