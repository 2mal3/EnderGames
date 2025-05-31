package io.github.mal32.endergames.kits;

import io.github.mal32.endergames.EnderGames;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class Mace extends AbstractKit {
  public Mace(EnderGames plugin) {
    super(plugin);
  }

  @Override
  public void start(Player player) {
    ItemStack mace = new ItemStack(Material.MACE);
    player.getInventory().addItem(mace);

    player
        .getInventory()
        .setBoots(
            enchantItem(new ItemStack(Material.LEATHER_BOOTS), Enchantment.FEATHER_FALLING, 3));
  }

  @Override
  public KitDescriptionItem getDescriptionItem() {
    return new KitDescriptionItem(
        Material.MACE,
        "Mace",
        "Delivers a crushing blow with a mighty mace",
        "Mace, Feather Falling III boots",
        Difficulty.HARD);
  }
}
