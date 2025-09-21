package io.github.mal32.endergames.kits;

import io.github.mal32.endergames.EnderGames;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.enchantments.Enchantment;

public class Bird extends AbstractKit {

  public Bird(EnderGames plugin) {
    super(plugin);
  }

  @Override
  public void start(Player player) {
    // Give the player an Elytra
    ItemStack elytra = new ItemStack(Material.ELYTRA);
    elytra.addEnchantment(Enchantment.VANISHING_CURSE, 1);
    player.getInventory().setChestplate(elytra);
    // Give the player 10 rockets (firework rockets)
    ItemStack rockets = new ItemStack(Material.FIREWORK_ROCKET, 10);
    rockets.addEnchantment(Enchantment.VANISHING_CURSE, 1);
    player.getInventory().addItem(rockets);
  }

  @EventHandler
  public void onPlayerKill(EntityDeathEvent event) {
    if (!(event.getEntity() instanceof Player victim)) return;
    Player killer = victim.getKiller();
    if (killer == null) return;
    if (!playerCanUseThisKit(killer)) return;

    ItemStack rockets = new ItemStack(Material.FIREWORK_ROCKET, 5);
    rockets.addEnchantment(Enchantment.VANISHING_CURSE, 1);
    killer.getInventory().addItem(rockets);
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
