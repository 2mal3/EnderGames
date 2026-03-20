package io.github.mal32.endergames.kitsystem.kits;

import io.github.mal32.endergames.kitsystem.api.AbstractKit;
import io.github.mal32.endergames.kitsystem.api.Difficulty;
import io.github.mal32.endergames.kitsystem.api.KitDescription;
import io.github.mal32.endergames.kitsystem.api.KitService;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class Mace extends AbstractKit {
  public Mace(KitService kitService, JavaPlugin plugin) {
    super(
        new KitDescription(
            "Mace",
            Material.MACE,
            "Gets 4 Wind Charges per player kill",
            "Maces with Wind Burst, 8 Wind Charges, Feather Falling III boots",
            Difficulty.HARD,
            "enga:mace"),
        kitService,
        plugin);
  }

  @EventHandler
  private void onPlayerKill(PlayerDeathEvent event) {
    Player killer = event.getEntity().getKiller();
    if (killer == null) return;
    if (!playerCanUseThisKit(killer)) return;

    killer.getInventory().addItem(new ItemStack(Material.WIND_CHARGE, 4));
  }

  @Override
  public void initPlayer(Player player) {
    ItemStack mace = new ItemStack(Material.MACE);
    enchantItem(mace, Enchantment.WIND_BURST, 1);
    mace.addUnsafeEnchantment(Enchantment.VANISHING_CURSE, 1);
    player.getInventory().addItem(mace);

    player.getInventory().addItem(new ItemStack(Material.WIND_CHARGE, 8));

    player
        .getInventory()
        .setBoots(
            enchantItem(new ItemStack(Material.LEATHER_BOOTS), Enchantment.FEATHER_FALLING, 3));
  }
}
