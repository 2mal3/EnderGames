package io.github.mal32.endergames.kits;

import io.github.mal32.endergames.EnderGames;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

public class Bomber extends AbstractKit {
  public Bomber(EnderGames plugin) {
    super(plugin);
  }

  @Override
  public void start(Player player) {
    ItemStack tntStack = new ItemStack(Material.TNT, 5);
    player.getInventory().addItem(tntStack);
  }

  @EventHandler
  private void onExplosionDamage(EntityDamageEvent event) {
    if (!(event.getEntity() instanceof Player player)) return;
    if (!playerCanUseThisKit(player)) return;

    if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION
        || event.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
      event.setCancelled(true);
    }
  }

  @EventHandler
  private void onEntityDeath(EntityDeathEvent event) {
    // When a player or entity is killed by a Bomber, create an explosion at the death location
    Player killer = event.getEntity().getKiller();
    if (killer == null || !playerCanUseThisKit(killer)) return;

    Location location = event.getEntity().getLocation();
    location.getWorld().createExplosion(location, 4f, false, true);
  }

  @EventHandler(priority = EventPriority.LOW)
  private void onTNTPlace(BlockPlaceEvent event) {
    if (event.getBlock().getType() != Material.TNT) return;
    Player player = event.getPlayer();
    if (!playerCanUseThisKit(player)) return;

    event.setCancelled(true);

    player.getInventory().removeItem(new ItemStack(Material.TNT, 1));

    Location location = event.getBlock().getLocation();
    event.getBlock().setType(Material.AIR);
    location.getWorld().createExplosion(location, 4f, false, true);
  }

  @Override
  public KitDescriptionItem getDescriptionItem() {
    return new KitDescriptionItem(
        Material.TNT,
        "Bomber",
        "Takes no explosion damage. Killed entities explode. TNT placed explodes instantly.",
        "5 TNT");
  }
}
