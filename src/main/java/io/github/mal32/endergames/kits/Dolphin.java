package io.github.mal32.endergames.kits;

import io.github.mal32.endergames.EnderGames;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class Dolphin extends AbstractKit {
  public Dolphin(EnderGames plugin) {
    super(plugin);
  }

  @Override
  public void initPlayer(Player player) {
    player
        .getInventory()
        .setLeggings(
            colorLeatherArmor(new ItemStack(Material.LEATHER_LEGGINGS), Color.fromRGB(3507428)));
    player.getInventory().addItem(new ItemStack(Material.WATER_BUCKET));

    player.addPotionEffect(
        new PotionEffect(
            PotionEffectType.CONDUIT_POWER, PotionEffect.INFINITE_DURATION, 0, true, false));
    player.addPotionEffect(
        new PotionEffect(
            PotionEffectType.DOLPHINS_GRACE, PotionEffect.INFINITE_DURATION, 0, true, false));
  }

  @EventHandler
  public void fishWhenSwimming(PlayerMoveEvent event) {
    if (!event.hasChangedBlock()) return;

    Player player = event.getPlayer();
    if (!playerCanUseThisKit(player)) return;

    if (!player.isSwimming()) return;

    final double FISHING_LOOT_PROBABILITY = 0.05;
    if (Math.random() > FISHING_LOOT_PROBABILITY) return;

    Bukkit.dispatchCommand(
        Bukkit.getConsoleSender(),
        "loot give " + player.getName() + " loot minecraft:gameplay/fishing/fish");
  }

  // Dolphin Jump While in water
  @EventHandler
  public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
    var player = event.getPlayer();
    if (!playerCanUseThisKit(player)) return;
    if (!playerisinwater(player)) return;

    event.setCancelled(true);
    player.setFlying(false);

    player.setAllowFlight(false);

    // The actual jump, reduced by slowness
    int slownessLevel = 0;
    if (player.getPotionEffect(PotionEffectType.SLOWNESS) != null) {
      slownessLevel = player.getPotionEffect(PotionEffectType.SLOWNESS).getAmplifier() + 1;
    }
    double horizontalMultiplier = HORIZONTAL_JUMP_SPEED * (1 - 0.30 * slownessLevel);
    double verticalMultiplier = VERTICAL_JUMP_SPEED * (1 - 0.30 * slownessLevel);
    Vector jump =
        player.getLocation().getDirection().multiply(horizontalMultiplier).setY(verticalMultiplier);
    player.setVelocity(jump);

    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1, 1);
}

    @Override
  public KitDescription getDescription() {
    return new KitDescription(
        Material.TROPICAL_FISH,
        "Dolphin",
        "Has permanent Conduit Power and Dolphins Grace. Swimming gives Fish.",
        "Water Bucket, Blue Leather Boots",
        Difficulty.MEDIUM);
  }
}
