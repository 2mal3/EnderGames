package io.github.mal32.endergames.kits;

import io.github.lambdaphoenix.advancementLib.AdvancementAPI;
import io.github.mal32.endergames.EnderGames;
import io.github.mal32.endergames.worlds.game.GameWorld;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemEnchantments;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Blaze extends AbstractKit {
  private final HashMap<UUID, LocalTime> burnTime = new HashMap<>();

  public Blaze(EnderGames plugin) {
    super(plugin);
  }

  @Override
  public void start(Player player) {
    ItemStack blazePowder = new ItemStack(Material.BLAZE_POWDER);
    ItemMeta meta = blazePowder.getItemMeta();
    meta.displayName(
        Component.text("Burn").color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
    blazePowder.setItemMeta(meta);
    blazePowder.setData(
        DataComponentTypes.ENCHANTMENTS,
        ItemEnchantments.itemEnchantments().add(Enchantment.VANISHING_CURSE, 1).build());
    player.getInventory().addItem(blazePowder);

    player.getInventory().addItem(new ItemStack(Material.GOLDEN_SWORD));

    player.addPotionEffect(
        new PotionEffect(
            PotionEffectType.FIRE_RESISTANCE,
            PotionEffect.INFINITE_DURATION,
            0,
            true,
            false,
            false));
  }

  @EventHandler
  public void onPlayerMoveWater(PlayerMoveEvent event) {
    if (!event.hasChangedBlock()) return;

    Player player = event.getPlayer();
    if (!playerCanUseThisKit(player)) return;

    PotionEffect effect = player.getPotionEffect(PotionEffectType.WEAKNESS);

    if (player.isInWater()) {
      if (effect == null) {
        player.addPotionEffect(
            new PotionEffect(
                PotionEffectType.WEAKNESS, PotionEffect.INFINITE_DURATION, 0, true, false));
      }
    } else if (effect != null && effect.getDuration() == PotionEffect.INFINITE_DURATION) {
      player.removePotionEffect(PotionEffectType.WEAKNESS);
    }
  }

  @EventHandler
  public void onHit(EntityDamageByEntityEvent event) {
    if (!(event.getDamager() instanceof Player damager) || !playerCanUseThisKit(damager)) return;

    if (Math.random() > 0.25) return;

    event.getEntity().setFireTicks(20 * 3);
  }

  @EventHandler
  public void onBowShot(EntityShootBowEvent event) {
    if (!(event.getEntity() instanceof Player player)) return;
    if (!playerCanUseThisKit(player)) return;

    if (Math.random() > 0.60) return;

    event.getProjectile().setFireTicks(20 * 3);
  }

  @EventHandler
  public void onPowderClick(PlayerInteractEvent event) {
    final int useCooldownSeconds = 30;
    final int burnDurationSeconds = 10;

    Player player = event.getPlayer();

    if (!playerCanUseThisKit(player)) return;
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
    if (!playerCanUseThisKit(player)) return;

    if (!burnTime.containsKey(player.getUniqueId())) return;
    if (burnTime.get(player.getUniqueId()).isBefore(LocalTime.now())) return;

    player.getLocation().getBlock().setType(Material.FIRE);
  }

  @Override
  public KitDescription getDescription() {
    return new KitDescription(
        Material.BLAZE_POWDER,
        "Blaze",
        "Can leave a fire trail for a short time. It is immune to fire damage, but gains Weakness I"
            + " in water. Sword or Bow hits have a 20% chance to ignite enemies",
        "Golden Sword and Burn Power",
        Difficulty.EASY);
  }

  @Override
  public void registerAdvancement(AdvancementAPI api) {
    api.register(PlayerInteractEvent.class)
        .advancementKey("enga:blaze")
        .condition(
            (player, event) -> {
              if (!GameWorld.playerIsInGame(player)) return false;
              if (event.getItem() == null) return false;
              return event.getItem().getType() == Material.FLINT_AND_STEEL;
            })
        .targetValue(5)
        .build();
  }
}
