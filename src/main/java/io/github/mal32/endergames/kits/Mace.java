package io.github.mal32.endergames.kits;

import io.github.lambdaphoenix.advancementLib.AdvancementAPI;
import io.github.mal32.endergames.EnderGames;
import io.github.mal32.endergames.worlds.game.GameWorld;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
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

  @Override
  public void registerAdvancement(AdvancementAPI api) {
    api.register(PlayerInteractEvent.class)
        .advancementKey("enga:mace")
        .condition(
            (player, event) -> {
              if (!GameWorld.playerIsInGame(player)) return false;
              if (event.getItem() == null) return false;
              return event.getItem().getType() == Material.WIND_CHARGE;
            })
        .targetValue(5)
        .build();
  }
}
