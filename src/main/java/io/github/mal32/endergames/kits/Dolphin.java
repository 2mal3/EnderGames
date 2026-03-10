package io.github.mal32.endergames.kits;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import io.github.mal32.endergames.EnderGames;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Dolphin extends AbstractKit {
  private static final double MIN_VERTICAL_JUMP_SPEED = 1.2;
  private static final double MAX_VERTICAL_JUMP_SPEED = 1.8;
  private static final double HORIZONTAL_JUMP_SPEED = 1.4;
  private static final long WATER_JUMP_COOLDOWN_MS = 500;

  private final Map<UUID, Long> lastWaterJump = new HashMap<>();

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

  // Dolphin jump while in water, triggered by moving upwards in water
  @EventHandler
  public void OnPlayerMove(PlayerMoveEvent event) {
    var player = event.getPlayer();
    if (!playerCanUseThisKit(player)) return;

    // Only care when the player is in or at water
    if (!playerIsInWater(player)) return;

    var from = event.getFrom();
    var to = event.getTo();

    // Detect that the player started to move upwards (simulating a jump press)
    double dy = to.getY() - from.getY();
    if (dy <= 0.001) return;

    // Cooldown so we don't trigger every tick while rising
    long now = System.currentTimeMillis();
    UUID id = player.getUniqueId();
    long last = lastWaterJump.getOrDefault(id, 0L);
    if (now - last < WATER_JUMP_COOLDOWN_MS) return;
    lastWaterJump.put(id, now);

    // The actual jump, reduced by slowness
    int slownessLevel = 0;
    var slowEffect = player.getPotionEffect(PotionEffectType.SLOWNESS);
    if (slowEffect != null) {
      slownessLevel = slowEffect.getAmplifier() + 1;
    }

    // Randomized vertical speed between MIN_VERTICAL_JUMP_SPEED and MAX_VERTICAL_JUMP_SPEED
    double rawVertical =
        MIN_VERTICAL_JUMP_SPEED
            + Math.random() * (MAX_VERTICAL_JUMP_SPEED - MIN_VERTICAL_JUMP_SPEED);

    double horizontalMultiplier = HORIZONTAL_JUMP_SPEED * (1 - 0.30 * slownessLevel);
    double verticalMultiplier = rawVertical * (1 - 0.30 * slownessLevel);
    Vector jump =
        player.getLocation().getDirection().multiply(horizontalMultiplier).setY(verticalMultiplier);
    player.setVelocity(jump);

    // Wither shoot sound on dolphin jump
    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 0.2f, 0.2f);
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
