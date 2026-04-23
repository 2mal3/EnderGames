package io.github.mal32.endergames.kitsystem.kits;

import static io.github.mal32.endergames.kitsystem.KitUtils.enchantItem;

import io.github.mal32.endergames.kitsystem.api.*;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class Mace extends AbstractKit implements KitUnlockAdvancement {
  public Mace(JavaPlugin plugin) {
    super(
        new KitDescription(
            "Mace",
            Material.MACE,
            "Gets 4 Wind Charges per player kill",
            "Maces with Wind Burst, 8 Wind Charges, Feather Falling III boots",
            Difficulty.HARD),
        plugin);
  }

  public String getKitAdvancementKey() {
    return "enga:mace";
  }

  @EventHandler
  private void onPlayerKill(PlayerDeathEvent event) {
    Player killer = event.getEntity().getKiller();
    if (killer == null) return;
    if (!playerCanUseThisKit(killer)) return;

    killer.getInventory().addItem(ItemStack.of(Material.WIND_CHARGE, 4));
  }

  @Override
  public void initPlayer(Player player) {
    ItemStack mace = ItemStack.of(Material.MACE);
    enchantItem(mace, Enchantment.WIND_BURST, 1);
    mace.addUnsafeEnchantment(Enchantment.VANISHING_CURSE, 1);
    player.getInventory().addItem(mace);

    player.getInventory().addItem(ItemStack.of(Material.WIND_CHARGE, 8));

    player
        .getInventory()
        .setBoots(
            enchantItem(ItemStack.of(Material.LEATHER_BOOTS), Enchantment.FEATHER_FALLING, 3));
  }
}
