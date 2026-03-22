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

/**
 * The Barbarian kit.
 *
 * <p>Players using this kit deal increased melee damage with swords depending on how hungry they
 * are. For every missing half hunger bar, the player gains +2.5% attack damage.
 *
 * <p>At game start, the player receives:
 *
 * <ul>
 *   <li>A full set of leather armor enchanted with Unbreaking I
 *   <li>A wooden sword
 * </ul>
 *
 * <h2>Ability: Hunger Rage</h2>
 *
 * When the player hits another entity with a sword:
 *
 * <ul>
 *   <li>Damage is multiplied by {@code 1 + (missingFood * 0.025)}
 *   <li>If the multiplier exceeds 1.30, a sound and heart particles are played
 * </ul>
 *
 * <p>This kit is classified as {@link Difficulty#HARD}.
 */
public class Barbarian extends AbstractKit {
  public Barbarian(KitService kitService, JavaPlugin plugin) {
    super(
        new KitDescription(
            "Barbarian",
            Material.LEATHER_CHESTPLATE,
            "Deals more attack damage with SWORDS the hungrier he is (+2.5% attack damage per half"
                + " hunger bar missing)",
            "A wooden sword and a full set of leather armor with Unbreaking I",
            Difficulty.HARD),
        kitService,
        plugin);
  }

  @Override
  public void initPlayer(Player player) {
    player
        .getInventory()
        .setHelmet(enchantItem(ItemStack.of(Material.LEATHER_HELMET), Enchantment.UNBREAKING, 1));
    player
        .getInventory()
        .setChestplate(
            enchantItem(ItemStack.of(Material.LEATHER_CHESTPLATE), Enchantment.UNBREAKING, 1));
    player
        .getInventory()
        .setLeggings(
            enchantItem(ItemStack.of(Material.LEATHER_LEGGINGS), Enchantment.UNBREAKING, 1));
    player
        .getInventory()
        .setBoots(enchantItem(ItemStack.of(Material.LEATHER_BOOTS), Enchantment.UNBREAKING, 1));
    player.getInventory().addItem(ItemStack.of(Material.WOODEN_SWORD));
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
