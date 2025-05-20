package io.github.mal32.endergames.worlds.game.game;

import io.github.mal32.endergames.EnderGames;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.SmithingInventory;

public class SmithingTemplateManager extends AbstractModule {

  public SmithingTemplateManager(EnderGames plugin) {
    super(plugin);
  }

  @EventHandler
  public void onSmithingTableOpen(InventoryOpenEvent event) {
    if (!(event.getInventory() instanceof SmithingInventory inventory)) return;
    inventory.setInputTemplate(new ItemStack(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE));
  }

  @EventHandler
  public void onSmithingTableClick(InventoryClickEvent event) {
    if (!(event.getInventory() instanceof SmithingInventory inventory)) return;
    ItemStack currentItem = event.getCurrentItem();
    if (currentItem == null
        || currentItem.getType() != Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE) return;

    event.setCancelled(true);
  }

  @EventHandler
  public void onSmithingTableClose(InventoryCloseEvent event) {
    if (!(event.getInventory() instanceof SmithingInventory inventory)) return;

    inventory.setInputTemplate(null);
  }
}
