package io.github.mal32.endergames.kits;

import java.util.Arrays;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Dolphin extends AbstractKit {
  public Dolphin(JavaPlugin plugin) {
    super(plugin);
  }

  @Override
  public void start(Player player) {
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
    if (!playerHasKit(player)) return;

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
    if (!playerHasKit(player)) return;

    if (!player.isSwimming()) return;

    final double FISHING_LOOT_PROBABILITY = 0.05;
    if (Math.random() > FISHING_LOOT_PROBABILITY) return;

    Bukkit.dispatchCommand(
        Bukkit.getConsoleSender(),
        "loot give " + player.getName() + " loot minecraft:gameplay/fishing/fish");
  }

  @Override
  public ItemStack getDescriptionItem() {
    ItemStack item = new ItemStack(Material.TROPICAL_FISH, 1);
    ItemMeta meta = item.getItemMeta();

    meta.displayName(
        Component.text("Dolphin")
            .color(NamedTextColor.GOLD)
            .decoration(TextDecoration.ITALIC, false));
    meta.lore(
        Arrays.asList(
            Component.text("Abilities:")
                .decorate(TextDecoration.UNDERLINED)
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false),
            Component.text("Has permanent Conduit Power")
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false),
            Component.text("and Dolphins Grace.")
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false),
            Component.text("Gets Regeneration and Resistance")
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false),
            Component.text("in Water.")
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false),
            Component.text("Has Weakness on Land.")
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false),
            Component.text("Swimming gives Fish.")
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false),
            Component.text(" "),
            Component.text("Equipment:")
                .decorate(TextDecoration.UNDERLINED)
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false),
            Component.text("Water Bucket")
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false),
            Component.text("Blue Leather Boots")
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false)));
    item.setItemMeta(meta);

    return item;
  }
}
