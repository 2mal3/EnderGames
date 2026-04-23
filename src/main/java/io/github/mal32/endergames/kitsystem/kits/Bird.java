package io.github.mal32.endergames.kitsystem.kits;

import io.github.mal32.endergames.kitsystem.api.AbstractKit;
import io.github.mal32.endergames.kitsystem.api.Difficulty;
import io.github.mal32.endergames.kitsystem.api.KitDescription;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * The Bird kit.
 *
 * <p>Players using this kit begin the game with an Elytra and a small supply of firework rockets,
 * allowing them to glide and maneuver through the air right from the start. This kit grants
 * additional rockets whenever the player kills another player.
 *
 * <p>At game start, the player receives:
 *
 * <ul>
 *   <li>An Elytra enchanted with Curse of Vanishing
 *   <li>5 Firework Rockets
 * </ul>
 *
 * <h2>Ability: Sky Hunter</h2>
 *
 * When the player kills another player:
 *
 * <ul>
 *   <li>The killer receives +5 Firework Rockets
 * </ul>
 *
 * <p>This kit is classified as {@link Difficulty#MEDIUM}.
 */
public class Bird extends AbstractKit {
  public Bird(JavaPlugin plugin) {
    super(
        new KitDescription(
            "Bird",
            Material.ELYTRA,
            "Starts with an Elytra and a Rockets. Gains a rockets per player kill. Fly like a"
                + " bird!",
            "Elytra, Firework Rocket",
            Difficulty.MEDIUM),
        plugin);
  }

  @Override
  public void initPlayer(Player player) {
    // Give the player an Elytra
    final ItemStack elytra = ItemStack.of(Material.ELYTRA);
    elytra.addUnsafeEnchantment(Enchantment.VANISHING_CURSE, 1);
    player.getInventory().setChestplate(elytra);
    // Give the player 5 rockets (firework rockets)
    player.getInventory().addItem(ItemStack.of(Material.FIREWORK_ROCKET, 1));
  }

  @EventHandler
  public void onPlayerKill(EntityDeathEvent event) {
    if (!(event.getEntity() instanceof Player victim)) return;
    Player killer = victim.getKiller();
    if (killer == null || !playerCanUseThisKit(killer)) return;

    killer.getInventory().addItem(ItemStack.of(Material.FIREWORK_ROCKET, 1));
  }
}
