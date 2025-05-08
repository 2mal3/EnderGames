package io.github.mal32.endergames.kits;

import java.util.Random;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootTables;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Slime extends AbstractKit {
  public Slime(JavaPlugin plugin) {
    super(plugin);
  }

  @Override
  public void start(Player player) {
    super.start(player);
    player.addPotionEffect(
        new PotionEffect(
            PotionEffectType.JUMP_BOOST, PotionEffect.INFINITE_DURATION, 1, true, false));
  }

  @EventHandler
  public void onEntityDamage(EntityDamageByEntityEvent event) {
    if (!(event.getEntity() instanceof Player player)) {
      return;
    }
    if (!playerHasKit(player)) {
      return;
    }
    if (event.getDamager() instanceof EnderPearl) {
      return;
    }
    Location location = player.getLocation();
    World world = player.getWorld();
    Random random = new Random();
    int loops = random.nextInt(3) + 1; // 1 bis 3
    for (int x = 0; x < loops; x++) {
      Location editLoc = location.clone();
      editLoc.add(1 - Math.random() * 2, 0.1, 1 - Math.random() * 2);
      if (world.getBlockAt(editLoc).getType() == Material.AIR
          || world.getBlockAt(editLoc).getType() == Material.WATER) {
        org.bukkit.entity.Slime slime =
            (org.bukkit.entity.Slime) world.spawnEntity(editLoc, EntityType.SLIME);
        Bukkit.getScheduler()
            .runTaskLater(
                plugin,
                () -> {
                  if (slime.isValid()) {
                    slime.remove();
                  }
                },
                20 * 4);
        slime.setLootTable(LootTables.ENDERMITE.getLootTable());

        slime.setSize(0);
        slime
            .getPersistentDataContainer()
            .set(new NamespacedKey(plugin, "slimekitslime"), PersistentDataType.INTEGER, 1);
      }
    }
  }

  @EventHandler
  public void onSlimeDeath(EntityDeathEvent event) {
    if (event.getEntity() instanceof org.bukkit.entity.Slime slime) {
      if (slime
          .getPersistentDataContainer()
          .has(new NamespacedKey(plugin, "slimekitslime"), PersistentDataType.INTEGER)) {
        event.getDrops().clear();
        event.setDroppedExp(0);
      }
    }
  }

  @EventHandler
  public void onSlimeballClick(PlayerInteractEvent event) {
    Player player = event.getPlayer();
    ItemStack item = event.getItem();

    if (item != null && item.getType() == Material.SLIME_BALL) {
      if (event.getAction() == Action.RIGHT_CLICK_AIR
          || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
        throwSlimeball(player);
        if (player.getGameMode() != GameMode.CREATIVE) {
          item.setAmount(item.getAmount() - 1);
        }
      }
    }
  }

  public void throwSlimeball(Player player) {
    Snowball snowball = player.launchProjectile(Snowball.class);
    snowball.setItem(new ItemStack(Material.SLIME_BALL));
  }

  @EventHandler
  public void onProjectileHit(ProjectileHitEvent event) {
    if (!(event.getEntity() instanceof Snowball snowball)) {
      return;
    }
    if (!(event.getEntity() instanceof Player)) {
      return;
    }
    plugin.getComponentLogger().debug(Component.text("Applying Slowness"));
    LivingEntity hitEntity = (LivingEntity) event.getEntity();
    if (hitEntity.getPotionEffect(PotionEffectType.SLOWNESS) != null) {
      int s_amp = hitEntity.getPotionEffect(PotionEffectType.SLOWNESS).getAmplifier();
      hitEntity.addPotionEffect(
          new PotionEffect(PotionEffectType.SLOWNESS, 7, s_amp + 1, true, false));
    }
    hitEntity.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 7, 0, true, false));
    // Play sound and particles on hit
    Location location = hitEntity.getLocation();
    location.getWorld().playSound(location, Sound.ENTITY_SLIME_HURT, 1, 1);
    location
        .getWorld()
        .spawnParticle(Particle.ITEM_SLIME, location.clone().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
  }
}
