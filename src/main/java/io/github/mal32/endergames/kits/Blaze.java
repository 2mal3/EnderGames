package io.github.mal32.endergames.kits;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
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
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Blaze extends AbstractKit {
  private final HashMap<UUID, LocalTime> burnTime = new HashMap<>();

  public Blaze(JavaPlugin plugin) {
    super(plugin);
  }

  @Override
  public void start(Player player) {
    {
      ItemStack blazePowder = new ItemStack(Material.BLAZE_POWDER);
      ItemMeta meta = blazePowder.getItemMeta();
      meta.displayName(
          Component.text("Burn")
              .color(NamedTextColor.GOLD)
              .decoration(TextDecoration.ITALIC, false));
      blazePowder.setItemMeta(meta);
      player.getInventory().addItem(blazePowder);
    }
    player.getInventory().addItem(new ItemStack(Material.GOLDEN_SWORD));

    player.addPotionEffect(
        new PotionEffect(
            PotionEffectType.FIRE_RESISTANCE, PotionEffect.INFINITE_DURATION, 0, true, false));
  }

  @EventHandler
  public void onPlayerMoveWater(PlayerMoveEvent event) {
    if (!event.hasChangedBlock()) return;

    Player player = event.getPlayer();
    if (!playerHasKit(player)) return;

    PotionEffect effect = player.getPotionEffect(PotionEffectType.WEAKNESS);

    if (player.isInWater()) {
      if (effect == null) {
        player.addPotionEffect(
            new PotionEffect(
                PotionEffectType.WEAKNESS, PotionEffect.INFINITE_DURATION, 0, true, false));
      }
    } else if (effect.getDuration() == PotionEffect.INFINITE_DURATION) {
      player.removePotionEffect(PotionEffectType.WEAKNESS);
      if (player.isInWater()) {
        if (effect == null) {
          player.addPotionEffect(
                  new PotionEffect(
                          PotionEffectType.WEAKNESS, PotionEffect.INFINITE_DURATION, 0, true, false));
        }
      } else if (effect.getDuration() == PotionEffect.INFINITE_DURATION) {
        player.removePotionEffect(PotionEffectType.WEAKNESS);
      }
    }
  }

  @EventHandler
  public void onHit(EntityDamageByEntityEvent event) {
    if (!(event.getDamager() instanceof Player damager) || !playerHasKit(damager)) return;

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

  @Override
  public ItemStack getDescriptionItem() {
    ItemStack item = new ItemStack(Material.BLAZE_POWDER, 1);
    ItemMeta meta = item.getItemMeta();
    meta.displayName(
        Component.text("Blaze")
            .color(NamedTextColor.GOLD)
            .decoration(TextDecoration.ITALIC, false));
    meta.lore(
        Arrays.asList(
            Component.text("Abilities:")
                .decorate(TextDecoration.UNDERLINED)
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false),
            Component.text("Can leave a fire trail for a short time.")
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false),
            Component.text("It is immune to fire damage,")
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false),
            Component.text("but gains Weakness I in water.")
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false),
            Component.text("Sword or Bow hits have a")
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false),
            Component.text("20% chance to ignite enemies")
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false),
            Component.text(" "), // Empty line — no styling needed
            Component.text("Equipment:")
                .decorate(TextDecoration.UNDERLINED)
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false),
            Component.text("Golden Sword and Burn Power")
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false)));
    item.setItemMeta(meta);

    return item;
  }
}
