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
    return 20 * 20;
  }

  @Override
  protected int blocksPerPlayer() {
    return 3;
  }

  @Override
  protected Enchanter getNewBlock(Location location) {
    return new Enchanter(location);
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
  public Enchanter(Location location) {
    super(location);
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
