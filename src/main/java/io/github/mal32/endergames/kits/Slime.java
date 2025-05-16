package io.github.mal32.endergames.kits;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import java.util.Arrays;
import java.util.Random;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
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
    player
        .getInventory()
        .setChestplate(
            colorLeatherArmor(new ItemStack(Material.LEATHER_CHESTPLATE), Color.fromRGB(3144049)));
    player
        .getInventory()
        .setBoots(colorLeatherArmor(new ItemStack(Material.LEATHER_BOOTS), Color.fromRGB(3144049)));
    player.getInventory().addItem(new ItemStack(Material.SLIME_BALL, 20));

    player.addPotionEffect(
        new PotionEffect(
            PotionEffectType.JUMP_BOOST, PotionEffect.INFINITE_DURATION, 1, true, false));
  }

  @EventHandler
  public void onEntityDamage(EntityDamageByEntityEvent event) {
    if (!(event.getEntity() instanceof Player player) || !playerCanUseThisKit(player)) return;

    if (event.getDamager() instanceof EnderPearl) return;

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
    if (!playerCanUseThisKit(player)) return;

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
    snowball.setShooter(player);
  }

  @EventHandler
  public void onProjectileHit(ProjectileHitEvent event) {
    if (!(event.getEntity() instanceof Snowball)) return;
    if (!(event.getHitEntity() instanceof Player hitEntity)) return;

    if (hitEntity.isBlocking()) return;

    if (hitEntity.getPotionEffect(PotionEffectType.SLOWNESS) != null) {
      int amplifier = hitEntity.getPotionEffect(PotionEffectType.SLOWNESS).getAmplifier();
      if (amplifier < 2) {
        amplifier += 1;
      }
      hitEntity.addPotionEffect(
          new PotionEffect(PotionEffectType.SLOWNESS, 7 * 20, amplifier, true, false));
    }
    hitEntity.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 7 * 20, 0, true, false));

    // Play sound and particles on hit
    Location location = hitEntity.getLocation();
    location.getWorld().playSound(location, Sound.ENTITY_SLIME_HURT, 1, 1);
    location
        .getWorld()
        .spawnParticle(Particle.ITEM_SLIME, location.clone().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
    // play Hit sound for Shooter
    Player shooterPlayer = (Player) event.getEntity().getShooter();
    if (shooterPlayer != null) {
      Location shooterLocation = shooterPlayer.getLocation();
      shooterLocation.getWorld().playSound(shooterLocation, Sound.ENTITY_SLIME_SQUISH_SMALL, 1, 1);
    }
  }

  // Get Slimeballs while Jumping
  @EventHandler
  public void onPlayerJump(PlayerJumpEvent event) {
    if (!playerCanUseThisKit(event.getPlayer())) return;

    Random random = new Random();
    ItemStack slimeball = new ItemStack(Material.SLIME_BALL, 1);
    if (random.nextInt(5) == 0) {
      event.getPlayer().getInventory().addItem(slimeball);
    }
  }

  @Override
  public ItemStack getDescriptionItem() {
    ItemStack item = new ItemStack(Material.SLIME_BALL, 1);
    ItemMeta meta = item.getItemMeta();
    meta.displayName(
        Component.text("Slime")
            .color(NamedTextColor.GOLD)
            .decoration(TextDecoration.ITALIC, false));
    meta.lore(
        Arrays.asList(
            Component.text("Abilities:")
                .decorate(TextDecoration.UNDERLINED)
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false),
            Component.text("Occasionally gains slimeballs when jumping,")
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false),
            Component.text("which can be thrown to slow enemies.")
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false),
            Component.text("Spawns small slimes when hit.")
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false),
            Component.text("Permanent Jump Boost II.")
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false),
            Component.text(" "), // Empty line — no styling needed
            Component.text("Equipment:")
                .decorate(TextDecoration.UNDERLINED)
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false),
            Component.text("10 slimeballs, green leather chestplate & boots")
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false)));
    item.setItemMeta(meta);

    return item;
  }
}
