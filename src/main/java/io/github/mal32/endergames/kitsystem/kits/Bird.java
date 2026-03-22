package io.github.mal32.endergames.kitsystem.kits;

import io.github.mal32.endergames.kitsystem.api.AbstractKit;
import io.github.mal32.endergames.kitsystem.api.Difficulty;
import io.github.mal32.endergames.kitsystem.api.KitDescription;
import io.github.mal32.endergames.kitsystem.api.KitService;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class Bird extends AbstractKit {
  public Bird(KitService kitService, JavaPlugin plugin) {
    super(
        new KitDescription(
            "Bird",
            Material.ELYTRA,
            "Starts with an Elytra and 10 Rockets. Gains 5 rockets per player kill. Fly like a bird!",
            "1 Elytra, 5 Firework Rockets",
            Difficulty.MEDIUM),
        kitService,
        plugin);
  }

  @Override
  public void initPlayer(Player player) {
    // Give the player an Elytra
    final ItemStack elytra = new ItemStack(Material.ELYTRA);
    elytra.addUnsafeEnchantment(Enchantment.VANISHING_CURSE, 1);
    player.getInventory().setChestplate(elytra);
    // Give the player 5 rockets (firework rockets)
    player.getInventory().addItem(new ItemStack(Material.FIREWORK_ROCKET, 5));
  }

  @EventHandler
  public void onPlayerKill(EntityDeathEvent event) {
    if (!(event.getEntity() instanceof Player victim)) return;
    Player killer = victim.getKiller();
    if (killer == null || !playerCanUseThisKit(killer)) return;

    killer.getInventory().addItem(new ItemStack(Material.FIREWORK_ROCKET, 5));
  }
}
