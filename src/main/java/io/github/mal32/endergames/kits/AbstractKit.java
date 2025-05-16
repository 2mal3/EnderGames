package io.github.mal32.endergames.kits;

import io.github.mal32.endergames.worlds.game.game.AbstractModule;
import java.util.List;
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

  public static List<AbstractKit> getKits(JavaPlugin plugin) {
    return List.of(
        new Lumberjack(plugin),
        new Cat(plugin),
        new Cactus(plugin),
        new Barbarian(plugin),
        new Blaze(plugin),
        new Slime(plugin),
        new Dolphin(plugin));
  }

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

  protected boolean playerCanUseThisKit(Player player) {
    var phase =
        player
            .getPersistentDataContainer()
            .get(new NamespacedKey(plugin, "phase"), PersistentDataType.STRING);

    return Objects.equals(phase, "game")
        && Objects.equals(
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
}
