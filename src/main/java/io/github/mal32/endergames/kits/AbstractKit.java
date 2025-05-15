package io.github.mal32.endergames.kits;

import io.github.mal32.endergames.phases.game.AbstractModule;
import java.util.Objects;

import org.bukkit.Color;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public abstract class AbstractKit extends AbstractModule {
  public AbstractKit(JavaPlugin plugin) {
    super(plugin);
  }

  protected boolean playerHasKit(Player player) {
    return Objects.equals(
        player
            .getPersistentDataContainer()
            .get(new NamespacedKey(plugin, "kit"), PersistentDataType.STRING),
        getName());
  }

  public abstract void start(Player player);

  public String getName() {
    return this.getClass().getSimpleName().toLowerCase();
  }

  public abstract ItemStack getDescriptionItem();

  protected static ItemStack enchantItem(ItemStack item, Enchantment enchantment, int level) {
    ItemMeta meta = item.getItemMeta();
    meta.addEnchant(enchantment, level, true);
    item.setItemMeta(meta);
    return item;
  }

  protected static ItemStack enchantItem(ItemStack item, Enchantment enchantment) {
    return enchantItem(item, enchantment, 1);
  }

  protected static ItemStack colorLeatherArmor(ItemStack item, Color color) {
    LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
    meta.setColor(color);
    item.setItemMeta(meta);
    return item;
  }
}
