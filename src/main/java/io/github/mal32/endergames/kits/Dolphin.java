package io.github.mal32.endergames.kits;

import io.github.mal32.endergames.EnderGames;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class Dolphin extends AbstractKit {
  private static final double VERTICAL_JUMP_SPEED = 1;
  private static final double HORIZONTAL_JUMP_SPEED = 2;

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
    //  player.addPotionEffect(
    //      new PotionEffect(
    //          PotionEffectType.DOLPHINS_GRACE, PotionEffect.INFINITE_DURATION, 0, true, false));

    player.setAllowFlight(
        false); // unnecessary but just to be safe, we only allow flight when in water
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
    System.console()
        .printf("Player %s toggled flight (event triggered)\n", event.getPlayer().getName());
    var player = event.getPlayer();
    if (!playerCanUseThisKit(player)) return;

    // Only allow the special jump if the player's feet are in water
    if (!playerIsInWater(player)) return;

    event.setCancelled(true);
    player.setFlying(false);

    // Temporarily disable flight; it will be re-enabled only when back in water.
    player.setAllowFlight(false);

    // The actual jump, reduced by slowness
    int slownessLevel = 0;
    var slowEffect = player.getPotionEffect(PotionEffectType.SLOWNESS);
    if (slowEffect != null) {
      slownessLevel = slowEffect.getAmplifier() + 1;
    }
    double horizontalMultiplier = HORIZONTAL_JUMP_SPEED * (1 - 0.30 * slownessLevel);
    double verticalMultiplier = VERTICAL_JUMP_SPEED * (1 - 0.30 * slownessLevel);
    Vector jump =
        player.getLocation().getDirection().multiply(horizontalMultiplier).setY(verticalMultiplier);
    player.setVelocity(jump);

    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1, 1);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerMoveForFlight(PlayerMoveEvent event) {
    if (!event.hasChangedBlock()) return;
    var player = event.getPlayer();
    if (!playerCanUseThisKit(player)) return;

    // Efficiently gate flight: only when in water and not standing on solid ground.
    boolean inWater = playerIsInWater(player);
    if (!inWater) {
      // Outside water, never allow flight to avoid normal flying.
      if (player.getAllowFlight()) {
        player.setAllowFlight(false);
      }
      return;
    }

    // In water: allow flight so the player can trigger the dolphin jump.
    if (!player.getAllowFlight()) {
      player.setAllowFlight(true);
    }
  }

  private boolean playerIsInWater(Player player) {
    // Check the block at the player's feet position.
    Block feetBlock = player.getLocation().getBlock();

    switch (feetBlock.getType()) {
      case WATER:
      case BUBBLE_COLUMN:
        return true;
      default:
        break;
    }

    var data = feetBlock.getBlockData();
    if (data instanceof Waterlogged waterlogged) {
      return waterlogged.isWaterlogged();
    }

    return false;
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
