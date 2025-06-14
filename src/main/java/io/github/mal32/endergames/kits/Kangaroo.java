package io.github.mal32.endergames.kits;

import io.github.mal32.endergames.EnderGames;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class Kangaroo extends AbstractKit {
  public Kangaroo(EnderGames plugin) {
    super(plugin);
  }

  @Override
  public KitDescriptionItem getDescriptionItem() {
    return new KitDescriptionItem(
        Material.RABBIT_FOOT,
        "Kangaroo",
        "Can double jump at the cost of hunger",
        null,
        Difficulty.EASY);
  }

  @Override
  public void start(Player player) {
    player.setAllowFlight(true);

    var attribute = player.getAttribute(Attribute.SAFE_FALL_DISTANCE);
    attribute.setBaseValue(10);
  }

  @EventHandler(priority = EventPriority.LOW)
  public void onPlayerDeath(PlayerDeathEvent event) {
    var player = event.getPlayer();
    if (!playerCanUseThisKit(player)) return;

    player.setAllowFlight(false);

    var attribute = player.getAttribute(Attribute.SAFE_FALL_DISTANCE);
    attribute.setBaseValue(attribute.getDefaultValue());
  }

  @EventHandler
  public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
    var player = event.getPlayer();
    if (!playerCanUseThisKit(player)) return;

    event.setCancelled(true);
    player.setFlying(false);

    int foodLevel = player.getFoodLevel();
    if (foodLevel < 8) {
      player.sendActionBar(
          Component.text("At least 4 Food Levels needed")
              .style(Style.style(NamedTextColor.YELLOW)));
      return;
    }

    player.setAllowFlight(false);

    Vector jump = player.getLocation().getDirection().multiply(2).setY(1);
    player.setVelocity(jump);

    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1, 1);

    var hungerEffect = player.getPotionEffect(PotionEffectType.HUNGER);
    final int durationSeconds = 10;
    final int startHungerLoss = 4;
    final int hungerLossExponent = 3;
    if (hungerEffect == null) {
      // hunger takes 1 saturation per second per 40 levels
      player.addPotionEffect(
          new PotionEffect(
              PotionEffectType.HUNGER,
              20 * durationSeconds,
              40 / durationSeconds * startHungerLoss,
              true,
              true));
      return;
    }
    int hungerAmplifier = hungerEffect.getAmplifier();
    player.addPotionEffect(
        new PotionEffect(
            PotionEffectType.HUNGER,
            20 * durationSeconds,
            hungerAmplifier * hungerLossExponent,
            true,
            true));
  }

  @EventHandler
  public void onPlayerLand(PlayerMoveEvent event) {
    if (!event.hasChangedBlock()) return;
    var player = event.getPlayer();
    if (!playerCanUseThisKit(player)) return;

    if (player.getLocation().clone().add(0, -0.1, 0).getBlock().isPassable()) return;

    player.setAllowFlight(true);
  }
}
