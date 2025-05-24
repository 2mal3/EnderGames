package io.github.mal32.endergames.kits;

import io.github.mal32.endergames.EnderGames;
import org.bukkit.Material;
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
    player.getInventory().setChestplate(new ItemStack(Material.ELYTRA));
    // Give the player 5 rockets (firework rockets)
    player.getInventory().addItem(new ItemStack(Material.FIREWORK_ROCKET, 5));
  }

  @EventHandler
  public void onPlayerKill(EntityDeathEvent event) {
    if (!(event.getEntity() instanceof Player victim)) return;
    Player killer = victim.getKiller();
    if (killer == null) return;
    if (!playerCanUseThisKit(killer)) return;

    killer.getInventory().addItem(new ItemStack(Material.FIREWORK_ROCKET, 2));
  }

  @Override
  public KitDescriptionItem getDescriptionItem() {
    return new KitDescriptionItem(
        Material.ELYTRA,
        "Bird",
        "Starts with an Elytra and 5 rockets. Gains 2 rockets per player kill. Fly like a bird!",
        "1 Elytra, 5 Firework Rockets");
  }
}
