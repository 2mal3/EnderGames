package io.github.mal32.endergames.kits;

import org.bukkit.Tag;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class Barbarian extends AbstractKit {
  public Barbarian(JavaPlugin plugin) {
    super(plugin);
  }

  public String getName() {
    return "barbarian";
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
}
