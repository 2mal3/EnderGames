package io.github.mal32.endergames.kits;

import io.github.mal32.endergames.EnderGames;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemEnchantments;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

public class Bird extends AbstractKit {

  public Bird(EnderGames plugin) {
    super(plugin);
  }

  @Override
  public void start(Player player) {
    // Give the player an Elytra
    final ItemStack elytra = new ItemStack(Material.ELYTRA);
    elytra.setData(
        DataComponentTypes.ENCHANTMENTS,
        ItemEnchantments.itemEnchantments().add(Enchantment.VANISHING_CURSE, 1).build());
    player.getInventory().setChestplate(elytra);
    // Give the player 5 rockets (firework rockets)
    player.getInventory().addItem(new ItemStack(Material.FIREWORK_ROCKET, 10));
  }

  @EventHandler
  public void onPlayerKill(EntityDeathEvent event) {
    if (!(event.getEntity() instanceof Player victim)) return;
    Player killer = victim.getKiller();
    if (killer == null) return;
    if (!playerCanUseThisKit(killer)) return;

    killer.getInventory().addItem(new ItemStack(Material.FIREWORK_ROCKET, 5));
  }

  @Override
  public KitDescription getDescription() {
    return new KitDescription(
        Material.ELYTRA,
        "Bird",
        "Starts with an Elytra and 10 Rockets. Gains 5 rockets per player kill. Fly like a bird!",
        "1 Elytra, 5 Firework Rockets",
        Difficulty.MEDIUM);
  }
}
