package io.github.mal32.endergames.kits;

import io.github.mal32.endergames.EnderGames;
import io.github.mal32.endergames.worlds.game.GameWorld;
import io.github.mal32.endergames.worlds.game.game.PotionEffectsStacking;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemEnchantments;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

public class Voodoo extends AbstractKit {
  private BukkitTask voodooTask;

  public Voodoo(EnderGames plugin) {
    super(plugin);
  }

  @Override
  public void enable() {
    super.enable();

    voodooTask = Bukkit.getScheduler().runTaskTimer(plugin, this::witherTask, 20L, 20L);
  }

  @Override
  public void disable() {
    super.disable();

    voodooTask.cancel();
  }

  @EventHandler(priority = EventPriority.LOW)
  public void onPlayerDeath(PlayerDeathEvent event) {
    if (!playerCanUseThisKit(event.getPlayer())) return;

    var player = event.getPlayer();
    player.getInventory().clear();
    player.setLevel(0);
  }

  @EventHandler
  public void regenerateOnBowHit(EntityDamageByEntityEvent event) {
    if (!(event.getDamager() instanceof Arrow arrow)) return;
    if (!(arrow.getShooter() instanceof Player shooter)) return;
    if (!playerCanUseThisKit(shooter)) return;
    if (!(event.getEntity() instanceof Player)) return;

    PotionEffectsStacking.addPotionEffect(
        shooter, new PotionEffect(PotionEffectType.REGENERATION, 1, 5, true, false, false));
  }

  @EventHandler
  public void healthBoostOnEnemyLow(EntityDamageByEntityEvent event) {
    if (!(event.getDamager() instanceof Player attacker)) return;
    if (!playerCanUseThisKit(attacker)) return;
    if (!(event.getEntity() instanceof Player victim)) return;

    var attribute = victim.getAttribute(Attribute.MAX_HEALTH);
    if (attribute == null) return;
    if (victim.getHealth() <= attribute.getValue() * 0.4) {
      PotionEffectsStacking.addPotionEffect(
          attacker, new PotionEffect(PotionEffectType.ABSORPTION, 20 * 60, 0, true));
    }
  }

  private void witherTask() {
    for (Player player : GameWorld.getPlayersInGame()) {
      if (!playerCanUseThisKit(player)) continue;

      for (Entity entity : player.getNearbyEntities(10, 10, 10)) {
        if (entity instanceof Player target) {
          target.addPotionEffect(
              new PotionEffect(PotionEffectType.WITHER, 20 * 2, 0, true, false, false));
        }
      }
    }
  }

  @Override
  public void start(Player player) {
    var playerInventory = player.getInventory();

    var bow = new ItemStack(Material.BOW);
    bow.setData(
        DataComponentTypes.ENCHANTMENTS,
        ItemEnchantments.itemEnchantments().add(Enchantment.PIERCING, 1).build());
    playerInventory.addItem(bow);

    playerInventory.addItem(new ItemStack(Material.ARROW, 10));
    playerInventory.setBoots(new ItemStack(Material.LEATHER_BOOTS));
  }

  @Override
  public KitDescription getDescription() {
    return new KitDescription(
        Material.SCULK,
        "Voodoo",
        "Doesn't drop items on death. Hitting players with arrows regenerates health. Making an"
            + " enemy low on health gives you extra hearts for a short time. Nearby players get"
            + " black hearts.",
        "Bow with Piercing, 10 Arrows, Leather Boots",
        Difficulty.MEDIUM);
  }

  @EventHandler
  public void cancelVoodooWitherDamage(EntityDamageEvent event) {
    if (!(event.getEntity() instanceof Player player)) return;
    if (event.getCause() != DamageCause.WITHER) return;
    PotionEffect effect =
        player.getActivePotionEffects().stream()
            .filter(eff -> eff.getType() == PotionEffectType.WITHER)
            .findFirst()
            .orElse(null);
    if (effect != null && !effect.hasIcon()) {
      event.setCancelled(true);
    }
  }
}
