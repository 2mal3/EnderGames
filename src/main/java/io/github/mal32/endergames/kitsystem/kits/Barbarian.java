package io.github.mal32.endergames.kitsystem.kits;

import static io.github.mal32.endergames.kitsystem.util.KitUtils.enchantItem;

import io.github.mal32.endergames.kitsystem.api.*;
import io.github.mal32.endergames.kitsystem.api.Difficulty;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class Barbarian extends AbstractKit {
  public Barbarian(KitService kitService, JavaPlugin plugin) {
    super(
        new KitDescription(
            "Barbarian",
            Material.LEATHER_CHESTPLATE,
            "Deals more attack damage with SWORDS the hungrier he is (+2.5% attack damage per half hunger bar missing)",
            "A wooden sword and a full set of leather armor with Unbreaking I",
            Difficulty.HARD),
        kitService,
        plugin);
  }

  @Override
  public void initPlayer(Player player) {
    player
        .getInventory()
        .setHelmet(enchantItem(new ItemStack(Material.LEATHER_HELMET), Enchantment.UNBREAKING, 1));
    player
        .getInventory()
        .setChestplate(
            enchantItem(new ItemStack(Material.LEATHER_CHESTPLATE), Enchantment.UNBREAKING, 1));
    player
        .getInventory()
        .setLeggings(
            enchantItem(new ItemStack(Material.LEATHER_LEGGINGS), Enchantment.UNBREAKING, 1));
    player
        .getInventory()
        .setBoots(enchantItem(new ItemStack(Material.LEATHER_BOOTS), Enchantment.UNBREAKING, 1));
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
}
