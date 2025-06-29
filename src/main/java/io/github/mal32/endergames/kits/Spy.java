package io.github.mal32.endergames.kits;

import io.github.mal32.endergames.EnderGames;
import java.util.HashSet;
import java.util.UUID;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Spy extends AbstractKit {
  private final HashSet<UUID> spiesInvisible = new HashSet<>();

  public Spy(EnderGames plugin) {
    super(plugin);
  }

  @Override
  public void start(Player player) {
    player
        .getInventory()
        .setChestplate(colorLeatherArmor(new ItemStack(Material.LEATHER_CHESTPLATE), Color.WHITE));
  }

  @Override
  public KitDescriptionItem getDescriptionItem() {
    return new KitDescriptionItem(
        Material.SPYGLASS,
        "Spy",
        "Go invisible while sneaking, but lose hunger faster",
        "White Leather Chestplate",
        Difficulty.EASY);
  }

  @EventHandler
  public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
    Player player = event.getPlayer();
    if (!playerCanUseThisKit(player)) return;

    if (event.isSneaking() && !spiesInvisible.contains(player.getUniqueId())) {
      enterInvisible(player);
      return;
    }

    if (!event.isSneaking() && spiesInvisible.contains(player.getUniqueId())) {
      leaveInvisible(player);
    }
  }

  private void enterInvisible(Player player) {
    final int HUNGER_LEVEL_LOSS_PER_MINUTE = 20;

    spiesInvisible.add(player.getUniqueId());

    player.addPotionEffect(
        new PotionEffect(
            PotionEffectType.INVISIBILITY, PotionEffect.INFINITE_DURATION, 0, true, false, true));
    player.addPotionEffect(
        new PotionEffect(
            PotionEffectType.RESISTANCE, PotionEffect.INFINITE_DURATION, 0, true, false, true));
    player.addPotionEffect(
        new PotionEffect(
            PotionEffectType.HUNGER,
            PotionEffect.INFINITE_DURATION,
            (int) (HUNGER_LEVEL_LOSS_PER_MINUTE / 1.5), // one hunger level per second per 40 levels
            true,
            false,
            true));
  }

  private void leaveInvisible(Player player) {
    spiesInvisible.remove(player.getUniqueId());
    player.removePotionEffect(PotionEffectType.INVISIBILITY);
    player.removePotionEffect(PotionEffectType.RESISTANCE);
    player.removePotionEffect(PotionEffectType.HUNGER);
  }
}
