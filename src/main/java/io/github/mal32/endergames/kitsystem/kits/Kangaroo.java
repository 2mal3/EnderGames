package io.github.mal32.endergames.kitsystem.kits;

import io.github.mal32.endergames.kitsystem.api.AbstractKit;
import io.github.mal32.endergames.kitsystem.api.Difficulty;
import io.github.mal32.endergames.kitsystem.api.KitDescription;
import io.github.mal32.endergames.kitsystem.api.KitService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class Kangaroo extends AbstractKit {
  private static final double VERTICAL_JUMP_SPEED = 1;
  private static final double HORIZONTAL_JUMP_SPEED = 2;
  private static final double SLOWNESS_JUMP_STRENGTH_DECREASE_PERCENT = 0.4;

  public Kangaroo(KitService kitService, JavaPlugin plugin) {
    super(
        new KitDescription(
            "Kangaroo",
            Material.RABBIT_FOOT,
            "Can double jump at the cost of hunger",
            "",
            Difficulty.EASY),
        kitService,
        plugin);
  }

  @Override
  public void initPlayer(Player player) {
    player.setAllowFlight(true);

    AttributeInstance attribute = player.getAttribute(Attribute.SAFE_FALL_DISTANCE);
    attribute.setBaseValue(10);
  }

  @EventHandler(priority = EventPriority.LOW)
  public void onPlayerDeath(PlayerDeathEvent event) {
    var player = event.getPlayer();
    if (!playerCanUseThisKit(player)) return;

    player.setAllowFlight(false);

    AttributeInstance attribute = player.getAttribute(Attribute.SAFE_FALL_DISTANCE);
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

    // The actual jump, reduced by slowness
    int slownessLevel = 0;
    if (player.getPotionEffect(PotionEffectType.SLOWNESS) != null) {
      slownessLevel = player.getPotionEffect(PotionEffectType.SLOWNESS).getAmplifier() + 1;
    }
    double jumpStrengthDecrease =
        Math.max((1 - SLOWNESS_JUMP_STRENGTH_DECREASE_PERCENT / slownessLevel), 0);
    double horizontalMultiplier = HORIZONTAL_JUMP_SPEED * jumpStrengthDecrease;
    double verticalMultiplier = VERTICAL_JUMP_SPEED * jumpStrengthDecrease;
    Vector jump =
        player.getLocation().getDirection().multiply(horizontalMultiplier).setY(verticalMultiplier);
    player.setVelocity(jump);

    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1, 1);

    // Hunger effects
    var hungerEffect = player.getPotionEffect(PotionEffectType.HUNGER);
    final int durationSeconds = 30;
    final int startHungerLoss = 2;
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
