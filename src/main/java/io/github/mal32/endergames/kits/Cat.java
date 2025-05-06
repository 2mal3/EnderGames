package io.github.mal32.endergames.kits;

import org.bukkit.Tag;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Cat extends AbstractKit {
    public Cat(JavaPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "cat";
    }

    @EventHandler
    private void onFallDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        if (!playerHasKit((Player) event.getEntity())) {
            return;
        }
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) {
            return;
        }

        event.setDamage(event.getDamage() * 0.5);
    }

    @EventHandler
    private void onPlayerEatFish(PlayerItemConsumeEvent event) {
        if (!playerHasKit((Player) event.getPlayer())) {
            return;
        }
        if (!Tag.ITEMS_FISHES.isTagged(event.getItem().getType())) {
            return;
        }

        event.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 30, 2, true));
    }

    @EventHandler
    private void onPlayerHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) {
            return;
        }
        Player damager = (Player) event.getDamager();
        if (!playerHasKit(damager)) {
            return;
        }

        // skip if damage is not with bare hands
        if (!damager.getInventory().getItemInMainHand().getType().isAir()) {
            return;
        }

        event.setDamage(event.getDamage() + 1);
    }
}
