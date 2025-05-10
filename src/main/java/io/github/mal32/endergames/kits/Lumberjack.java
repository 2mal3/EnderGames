package io.github.mal32.endergames.kits;

import java.util.Arrays;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class Lumberjack extends AbstractKit {
  public Lumberjack(JavaPlugin plugin) {
    super(plugin);
  }

  @EventHandler
  private void onBlockBreak(BlockBreakEvent event) {
    if (!Tag.LOGS.isTagged(event.getBlock().getType())) {
      return;
    }
    if (!playerHasKit(event.getPlayer())) {
      return;
    }

    Location location = event.getBlock().getLocation().add(0, 1, 0);
    breakTree(location);
  }

  private void breakTree(Location location) {
    Block block = location.getBlock();
    if (!Tag.LOGS.isTagged(block.getType()) && !Tag.LEAVES.isTagged(block.getType())) {
      return;
    }

    block.breakNaturally();
    breakTree(location.clone().add(1, 0, 0));
    breakTree(location.clone().add(-1, 0, 0));
    breakTree(location.clone().add(0, 1, 0));
    breakTree(location.clone().add(0, 0, 1));
    breakTree(location.clone().add(0, 0, -1));
  }

  @EventHandler
  private void onCraftItem(CraftItemEvent event) {
    if (!playerHasKit((Player) event.getWhoClicked())) {
      return;
    }

    ItemStack result = event.getRecipe().getResult();
    if (!Tag.ITEMS_AXES.isTagged(result.getType())) {
      return;
    }
    result.addEnchantment(Enchantment.SHARPNESS, 1);
    event.getInventory().setResult(result);
  }

  @Override
  public ItemStack getDescriptionItem() {
    ItemStack item = new ItemStack(Material.LEATHER_CHESTPLATE, 1);
    ItemMeta meta = item.getItemMeta();
    meta.displayName(Component.text("Barbarian"));
    meta.lore(
        Arrays.asList(
            Component.text("deals 5% more damage per empty food level with swords")
                .color(NamedTextColor.GOLD),
            Component.text("Equipment: Wooden Sword and full Leather armor")
                .color(NamedTextColor.GOLD)));
    item.setItemMeta(meta);
    return item;
  }
}
