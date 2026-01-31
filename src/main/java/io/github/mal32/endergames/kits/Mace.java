package io.github.mal32.endergames.kits;

import io.github.mal32.endergames.EnderGames;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemEnchantments;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

public class Mace extends AbstractKit {
  public Mace(EnderGames plugin) {
    super(plugin);
  }

  @EventHandler
  private void onPlayerKill(PlayerDeathEvent event) {
    Player killer = event.getEntity().getKiller();
    if (killer == null) return;
    if (!playerCanUseThisKit(killer)) return;

    killer.getInventory().addItem(new ItemStack(Material.WIND_CHARGE, 4));
  }

  @Override
  public void start(Player player) {
    ItemStack mace = new ItemStack(Material.MACE);
    enchantItem(mace, Enchantment.WIND_BURST, 1);
    mace.setData(
        DataComponentTypes.ENCHANTMENTS,
        ItemEnchantments.itemEnchantments().add(Enchantment.VANISHING_CURSE, 1).build());
    player.getInventory().addItem(mace);

    player.getInventory().addItem(new ItemStack(Material.WIND_CHARGE, 8));

    player
        .getInventory()
        .setBoots(
            enchantItem(new ItemStack(Material.LEATHER_BOOTS), Enchantment.FEATHER_FALLING, 3));
  }

  @Override
  public KitDescription getDescription() {
    return new KitDescription(
        Material.MACE,
        "Mace",
        "Gets 4 Wind Charges per player kill",
        "Maces with Wind Burst, 8 Wind Charges, Feather Falling III boots",
        Difficulty.HARD);
  }
}
