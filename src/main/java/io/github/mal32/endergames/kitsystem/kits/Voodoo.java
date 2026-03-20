package io.github.mal32.endergames.kitsystem.kits;

import io.github.mal32.endergames.game.game.PotionEffectsStacking;
import io.github.mal32.endergames.game.phases.PhaseController;
import io.github.mal32.endergames.kitsystem.api.AbstractKit;
import io.github.mal32.endergames.kitsystem.api.Difficulty;
import io.github.mal32.endergames.kitsystem.api.KitDescription;
import io.github.mal32.endergames.kitsystem.api.KitService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

public class Voodoo extends AbstractKit {
  private BukkitTask voodooTask;

  public Voodoo(KitService kitService, JavaPlugin plugin) {
    super(
        new KitDescription(
            "Voodoo",
            Material.SCULK,
            "Hitting entities with arrows regenerates health. Critically damaging players gives a short health boost. Nearby players get black hearts.",
            "Bow with Piercing, 10 Arrows, Leather Boots",
            Difficulty.MEDIUM,
            "enga:voodoo"),
        kitService,
        plugin);
  }

  @Override
  public void onEnable() {
    voodooTask = Bukkit.getScheduler().runTaskTimer(plugin, this::witherTask, 20L, 20L);
  }

  @Override
  public void onDisable() {
    voodooTask.cancel();
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onPlayerDeath(PlayerDeathEvent event) {
    if (!playerCanUseThisKit(event.getPlayer())) return;

    event.getDrops().clear();
    event.setShouldDropExperience(false);

    Location location = event.getEntity().getLocation();
    World world = location.getWorld();
    world.spawnParticle(
        Particle.SMOKE,
        location.getX(),
        location.getY() + 0.5,
        location.getZ(),
        20,
        0.5,
        0.5,
        0.5,
        0);
    world.playSound(location, Sound.ENTITY_GENERIC_BURN, SoundCategory.PLAYERS, 0.5f, 0.6f);
  }

  @EventHandler
  public void regenerateOnBowHit(EntityDamageByEntityEvent event) {
    if (!(event.getDamager() instanceof Arrow arrow)) return;
    if (!(arrow.getShooter() instanceof Player shooter)) return;
    if (!playerCanUseThisKit(shooter)) return;

    final int regeneratedHealt = 2;
    PotionEffectsStacking.addPotionEffect(
        shooter,
        new PotionEffect(PotionEffectType.REGENERATION, regeneratedHealt, 5, true, false, false));
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
    for (Player player : PhaseController.getPlayersInGame()) {
      if (!playerCanUseThisKit(player)) continue;

      for (Entity entity : player.getNearbyEntities(10, 10, 10)) {
        if (entity instanceof Player target) {
          target.addPotionEffect(
              new PotionEffect(PotionEffectType.WITHER, 20 * 2, 0, true, false, false));
        }
      }
    }
  }

  @EventHandler
  private void onPlayerMove(PlayerMoveEvent event) {
    if (!event.hasChangedBlock()) return;
    Player player = event.getPlayer();
    if (!playerCanUseThisKit(player)) return;

    Location targetLocation = event.getTo();
    Block targetBlock = targetLocation.getBlock();
    if (!Tag.FLOWERS.isTagged(targetBlock.getType())) return;
    if (targetBlock.getType() == Material.WITHER_ROSE) return;

    targetBlock.setType(Material.WITHER_ROSE);
    targetLocation
        .getWorld()
        .playSound(
            targetLocation, Sound.BLOCK_CACTUS_FLOWER_PLACE, SoundCategory.PLAYERS, 0.5f, 0.8f);
  }

  @EventHandler
  private void cancelWitherRoseInfection(EntityPotionEffectEvent event) {
    if (!(event.getEntity() instanceof Player player)) return;
    if (!playerCanUseThisKit(player)) return;
    if (event.getCause() != EntityPotionEffectEvent.Cause.WITHER_ROSE) return;

    event.setCancelled(true);
  }

  @Override
  public void initPlayer(Player player) {
    var playerInventory = player.getInventory();

    var bow = new ItemStack(Material.BOW);
    bow.addEnchantment(Enchantment.PIERCING, 1);
    playerInventory.addItem(bow);

    playerInventory.addItem(new ItemStack(Material.ARROW, 10));
    playerInventory.setBoots(new ItemStack(Material.LEATHER_BOOTS));
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
