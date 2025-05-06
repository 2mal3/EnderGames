package io.github.mal32.endergames.kits;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class Cactus extends AbstractKit {
    public Cactus(JavaPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "cactus";
    }

    @EventHandler
    public void onEntityDamagedByEntity(EntityDamageByEntityEvent event)  {
        if (!(event.getEntity() instanceof Player player) || !(event.getDamager() instanceof Damageable damager)) {
            return;
        }
        if (!playerHasKit(player)) {
            return;
        }

        if (Math.random() < 0.5) {
            return;
        }


        Location location = player.getLocation();
        location.getWorld().playSound(location, Sound.ENCHANT_THORNS_HIT, 1, 1);

        int damage = 1 + (int) (Math.round(Math.random() * 3));
        damager.damage(damage, player);
    }
}
