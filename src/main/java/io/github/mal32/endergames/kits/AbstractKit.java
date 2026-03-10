package io.github.mal32.endergames.kits;

import io.github.lambdaphoenix.advancementLib.AdvancementAPI;
import io.github.mal32.endergames.AbstractModule;
import io.github.mal32.endergames.EnderGames;
import io.github.mal32.endergames.services.KitType;
import io.github.mal32.endergames.worlds.game.GameWorld;
import org.bukkit.Color;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

public abstract class AbstractKit extends AbstractModule {
  public static final NamespacedKey kitStorageKey = new NamespacedKey("endergames", "kit");
  private final KitType type;

  public AbstractKit(EnderGames plugin, KitType type) {
    super(plugin);
    this.type = type;
  }

  protected static ItemStack enchantItem(ItemStack item, Enchantment enchantment, int level) {
    ItemMeta meta = item.getItemMeta();
    meta.addEnchant(enchantment, level, true);
    item.setItemMeta(meta);
    return item;
  }

  protected static ItemStack colorLeatherArmor(ItemStack item, Color color) {
    LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
    meta.setColor(color);
    item.setItemMeta(meta);
    return item;
  }

  protected boolean playerCanUseThisKit(Player player) {
    return GameWorld.playerIsInGame(player) && KitType.get(player).equals(this.type);
  }

  public abstract void initPlayer(Player player);

  public String getNameLowercase() {
    String simpleName = this.getClass().getSimpleName();
    String withSpaces = simpleName.replaceAll("([a-z])([A-Z])", "$1 $2"); // "Forest Spirit"
    return withSpaces.toLowerCase();
  }

  public abstract KitDescription getDescription();

  public void registerAdvancement(AdvancementAPI api) {}
}
