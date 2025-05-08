package io.github.mal32.endergames.kits;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Blaze extends AbstractKit {
  public Blaze(JavaPlugin plugin) {
    super(plugin);
  }

  @Override
  public String getName() {
    return "blaze";
  }

  @Override
  public void start(Player player) {
    super.start(player);
    player.addPotionEffect(
        new PotionEffect(
            PotionEffectType.FIRE_RESISTANCE, PotionEffect.INFINITE_DURATION, 0, true, false));
  }

  @EventHandler
  public void onPlayerMoveWater(PlayerMoveEvent event) {
    if (!event.hasChangedBlock()) return;

    Player player = event.getPlayer();
    if (!playerHasKit(player)) return;

    boolean playerIsInWater = player.isInWater();
    boolean playerHasWeakness = player.hasPotionEffect(PotionEffectType.WEAKNESS);

    if (playerIsInWater && !playerHasWeakness) {
      player.addPotionEffect(
          new PotionEffect(
              PotionEffectType.WEAKNESS, PotionEffect.INFINITE_DURATION, 0, true, false));
    }

    if (!playerIsInWater && playerHasWeakness) {
      PotionEffect weaknessEffect = player.getPotionEffect(PotionEffectType.WEAKNESS);
      if (weaknessEffect != null
          && weaknessEffect.getDuration() == PotionEffect.INFINITE_DURATION) {
        player.removePotionEffect(PotionEffectType.WEAKNESS);
      }
    }
  }

  @EventHandler
  public void onHit(EntityDamageByEntityEvent event) {
    if (!(event.getDamager() instanceof Player damager)) return;
    if (!playerHasKit(damager)) return;

    if (Math.random() > 0.2) return;

    event.getEntity().setFireTicks(20 * 3);
  }

  @EventHandler
  public void onBowShot(EntityShootBowEvent event) {
    if (!(event.getEntity() instanceof Player player)) return;
    if (!playerHasKit(player)) return;

    if (Math.random() > 0.25) return;

    event.getProjectile().setFireTicks(20 * 3);
  }

  private final HashMap<UUID, LocalTime> burnTime = new HashMap<UUID, LocalTime>();

  @EventHandler
  public void onPowderClick(PlayerInteractEvent event) {
    final int useCooldownSeconds = 30;
    final int burnDurationSeconds = 10;

    Player player = event.getPlayer();

    if (!playerHasKit(player)) return;
    ItemStack item = event.getItem();
    if (item == null || item.getType() != Material.BLAZE_POWDER) return;
    if (player.hasCooldown(Material.BLAZE_POWDER)) return;

    burnTime.put(player.getUniqueId(), LocalTime.now().plusSeconds(burnDurationSeconds));

    player.setCooldown(Material.BLAZE_POWDER, useCooldownSeconds * 20);

    Location location = event.getPlayer().getLocation();
    location.getWorld().playSound(location, Sound.ENTITY_BLAZE_SHOOT, 1, 1);
    location
        .getWorld()
        .spawnParticle(Particle.FLAME, location.clone().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
  }

  @EventHandler
  public void onPlayerMoveFire(PlayerMoveEvent event) {
    if (!event.hasChangedBlock()) return;

    Player player = event.getPlayer();
    if (!playerHasKit(player)) return;

    if (!burnTime.containsKey(player.getUniqueId())) return;
    if (burnTime.get(player.getUniqueId()).isBefore(LocalTime.now())) return;

    player.getLocation().getBlock().setType(Material.FIRE);
  }
}
