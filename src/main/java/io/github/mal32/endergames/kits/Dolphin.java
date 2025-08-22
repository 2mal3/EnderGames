package io.github.mal32.endergames.kits;

import io.github.mal32.endergames.EnderGames;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Dolphin extends AbstractKit {
  public Dolphin(EnderGames plugin) {
    super(plugin);
  }

  @Override
  public void start(Player player) {
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
  public void waterEffects(PlayerMoveEvent event) {
    if (!event.hasChangedBlock()) return;

    Player player = event.getPlayer();
    if (!playerCanUseThisKit(player)) return;

    boolean hasRegeneration =
        player.hasPotionEffect(PotionEffectType.REGENERATION)
            && player.getPotionEffect(PotionEffectType.REGENERATION).getDuration()
                == PotionEffect.INFINITE_DURATION;
    boolean hasWeakness =
        player.hasPotionEffect(PotionEffectType.WEAKNESS)
            && player.getPotionEffect(PotionEffectType.WEAKNESS).getDuration()
                == PotionEffect.INFINITE_DURATION;
    boolean hasResistance =
        player.hasPotionEffect(PotionEffectType.RESISTANCE)
            && player.getPotionEffect(PotionEffectType.RESISTANCE).getDuration()
                == PotionEffect.INFINITE_DURATION;

    // Effects in Water
    if (player.isInWater()) {
      if (!hasRegeneration) {
        player.addPotionEffect(
            new PotionEffect(
                PotionEffectType.REGENERATION, PotionEffect.INFINITE_DURATION, 0, true, false));
      }
      if (!hasResistance) {
        player.addPotionEffect(
            new PotionEffect(
                PotionEffectType.RESISTANCE, PotionEffect.INFINITE_DURATION, 0, true, false));
      }
      if (hasWeakness) {
        player.removePotionEffect(PotionEffectType.WEAKNESS);
      }

      // Effects out of Water
    } else {
      if (hasRegeneration) {
        player.removePotionEffect(PotionEffectType.REGENERATION);
      }
      if (hasResistance) {
        player.removePotionEffect(PotionEffectType.RESISTANCE);
      }
      if (!hasWeakness) {
        player.addPotionEffect(
            new PotionEffect(
                PotionEffectType.WEAKNESS, PotionEffect.INFINITE_DURATION, 0, true, false));
      }
    }
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

  @Override
  public KitDescription getDescription() {
    return new KitDescription(
        Material.TROPICAL_FISH,
        "Dolphin",
        "Has permanent Conduit Power and Dolphins Grace. Gets Regeneration and Resistance in Water."
            + " Has Weakness on Land. Swimming gives Fish.",
        "Water Bucket, Blue Leather Boots",
        Difficulty.HARD);
  }
}
