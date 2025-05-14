package io.github.mal32.endergames.kits;

import java.util.Arrays;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
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

  @Override
  public void start(Player player) {
    player.getInventory().setChestplate(colorLeatherArmor(new ItemStack(Material.LEATHER_CHESTPLATE), Color.fromRGB(10820909)));
    player.getInventory().setLeggings(colorLeatherArmor(new ItemStack(Material.LEATHER_LEGGINGS), Color.fromRGB(1728436)));
    player.getInventory().addItem(enchantItem(new ItemStack(Material.WOODEN_AXE), Enchantment.SHARPNESS));
  }

  @EventHandler
  private void onBlockBreak(BlockBreakEvent event) {
    if (!Tag.LOGS.isTagged(event.getBlock().getType())) return;

    if (!playerHasKit(event.getPlayer())) return;

    Location location = event.getBlock().getLocation().add(0, 1, 0);
    breakTree(location);
  }

  private void breakTree(Location location) {
    Block block = location.getBlock();
    if (!Tag.LOGS.isTagged(block.getType()) && !Tag.LEAVES.isTagged(block.getType())) return;

    block.breakNaturally();
    breakTree(location.clone().add(1, 0, 0));
    breakTree(location.clone().add(-1, 0, 0));
    breakTree(location.clone().add(0, 1, 0));
    breakTree(location.clone().add(0, 0, 1));
    breakTree(location.clone().add(0, 0, -1));
  }

  @EventHandler
  private void onCraftItem(CraftItemEvent event) {
    if (!playerHasKit((Player) event.getWhoClicked())) return;

    ItemStack result = event.getRecipe().getResult();
    if (!Tag.ITEMS_AXES.isTagged(result.getType())) return;

    result.addEnchantment(Enchantment.SHARPNESS, 1);
    event.getInventory().setResult(result);
  }

  @Override
  public ItemStack getDescriptionItem() {
    ItemStack item = new ItemStack(Material.WOODEN_AXE, 1);
    ItemMeta meta = item.getItemMeta();
    meta.displayName(
        Component.text("Lumberjack")
            .color(NamedTextColor.GOLD)
            .decoration(TextDecoration.ITALIC, false));
    meta.lore(
        Arrays.asList(
            Component.text("Abilities:")
                .decorate(TextDecoration.UNDERLINED)
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false),
            Component.text("The lumberjack can fell entire trees")
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false),
            Component.text("with a single axe swing.")
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false),
            Component.text("Every axe he crafts automatically")
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false),
            Component.text("gains Sharpness I.")
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false),
            Component.text(" "),
            Component.text("Equipment:")
                .decorate(TextDecoration.UNDERLINED)
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false),
            Component.text("Wooden axe, red leather chestplate,")
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false),
            Component.text("blue leather pants, black boots.")
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false)));

    item.setItemMeta(meta);

    return item;
  }
}
