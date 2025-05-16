package io.github.mal32.endergames.kits;

import java.util.Arrays;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
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

  @Override
  public void start(Player player) {
    player
        .getInventory()
        .setHelmet(enchantItem(new ItemStack(Material.LEATHER_HELMET), Enchantment.UNBREAKING));
    player
        .getInventory()
        .setChestplate(
            enchantItem(new ItemStack(Material.LEATHER_CHESTPLATE), Enchantment.UNBREAKING));
    player
        .getInventory()
        .setLeggings(enchantItem(new ItemStack(Material.LEATHER_LEGGINGS), Enchantment.UNBREAKING));
    player
        .getInventory()
        .setBoots(enchantItem(new ItemStack(Material.LEATHER_BOOTS), Enchantment.UNBREAKING));
    player.getInventory().addItem(new ItemStack(Material.WOODEN_SWORD));
  }

  @EventHandler
  public void onHit(EntityDamageByEntityEvent event) {
    if (!(event.getDamager() instanceof Player damager) || !playerCanUseThisKit(damager)) return;

    if (!Tag.ITEMS_SWORDS.isTagged(damager.getInventory().getItemInMainHand().getType())) return;

    // +2.5% damage per lost food level
    int foodLevel = damager.getFoodLevel();
    final int maxFoodLevel = 20;
    double damageMultiplier = 1 + ((maxFoodLevel - foodLevel) * 0.025);
    event.setDamage(event.getDamage() * damageMultiplier);

    if (damageMultiplier > 1.30) {
      Location location = event.getEntity().getLocation();
      location.getWorld().playSound(location, Sound.BLOCK_MANGROVE_ROOTS_BREAK, 1, 0.5f);
      location
          .getWorld()
          .spawnParticle(Particle.HEART, location.clone().add(0, 1, 0), 10, 0.2, 0.6, 0.2, 2);
    }
  }

  @Override
  public ItemStack getDescriptionItem() {
    ItemStack item = new ItemStack(Material.LEATHER_CHESTPLATE, 1);
    ItemMeta meta = item.getItemMeta();
    meta.displayName(
        Component.text("Barbarian")
            .color(NamedTextColor.GOLD)
            .decoration(TextDecoration.ITALIC, false));
    meta.lore(
        Arrays.asList(
            Component.text("Abilities:")
                .decorate(TextDecoration.UNDERLINED)
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false),
            Component.text("Deals more attack damage")
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false),
            Component.text("the hungrier he is")
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false),
            Component.text("(+2.5% attack damage per")
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false),
            Component.text("half hunger bar missing)")
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false),
            Component.text(" "), // Empty line — no styling needed
            Component.text("Equipment:")
                .decorate(TextDecoration.UNDERLINED)
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false),
            Component.text("A wooden sword and")
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false),
            Component.text("a full set of leather")
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false),
            Component.text("armor with Unbreaking I")
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false)));
    item.setItemMeta(meta);

    return item;
  }
}
