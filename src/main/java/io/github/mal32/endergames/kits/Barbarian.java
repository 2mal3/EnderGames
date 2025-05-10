package io.github.mal32.endergames.kits;

import java.util.Arrays;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class Barbarian extends AbstractKit {
  public Barbarian(JavaPlugin plugin) {
    super(plugin);
  }

  @EventHandler
  public void onHit(EntityDamageByEntityEvent event) {
    if (!(event.getDamager() instanceof Player damager)) {
      return;
    }
    if (!playerHasKit(damager)) {
      return;
    }
    if (!Tag.ITEMS_SWORDS.isTagged(damager.getInventory().getItemInMainHand().getType())) {
      return;
    }

    // +2.5% damage per lost food level
    int foodLevel = damager.getFoodLevel();
    final int maxFoodLevel = 20;
    double damageMultiplier = 1 + ((maxFoodLevel - foodLevel) * 0.05);
    event.setDamage(event.getDamage() * damageMultiplier);
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
