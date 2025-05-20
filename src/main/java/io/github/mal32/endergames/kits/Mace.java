package io.github.mal32.endergames.kits;

import io.github.mal32.endergames.EnderGames;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class Mace extends AbstractKit {
  public Mace(EnderGames plugin) {
    super(plugin);
  }

  @Override
  public void start(Player player) {
    ItemStack mace = new ItemStack(Material.MACE);
    player.getInventory().setItemInMainHand(mace);

    player
        .getInventory()
        .setBoots(
            enchantItem(new ItemStack(Material.LEATHER_BOOTS), Enchantment.FEATHER_FALLING, 3));
  }

  @Override
  public ItemStack getDescriptionItem() {
    ItemStack item = new ItemStack(Material.MACE, 1);
    ItemMeta meta = item.getItemMeta();
    meta.displayName(
        Component.text("Mace").color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
    meta.lore(
        java.util.Arrays.asList(
            Component.text("Abilities:")
                .decorate(TextDecoration.UNDERLINED)
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false),
            Component.text("Delivers a crushing blow")
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false),
            Component.text("with a mighty mace")
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false),
            Component.text(" "), // Empty line — no styling needed
            Component.text("Equipment:")
                .decorate(TextDecoration.UNDERLINED)
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false),
            Component.text("Mace")
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false),
            Component.text("Feather Falling III boots")
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false)));
    item.setItemMeta(meta);
    return item;
  }
}
