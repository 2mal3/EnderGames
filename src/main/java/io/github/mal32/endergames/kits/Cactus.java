package io.github.mal32.endergames.kits;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class Cactus extends AbstractKit {
  public Cactus(JavaPlugin plugin) {
    super(plugin);
  }

  @EventHandler
  public void onEntityDamagedByEntity(EntityDamageByEntityEvent event) {
    if (!(event.getEntity() instanceof Player player)
        || !(event.getDamager() instanceof Damageable damager)) {
      return;
    }
    if (!playerHasKit(player)) {
      return;
    }

    if (Math.random() > 0.8) {
      return;
    }

    Location location = player.getLocation();
    location.getWorld().playSound(location, Sound.ENCHANT_THORNS_HIT, 1, 1);

    int damage = 1 + (int) (Math.round(Math.random() * 4));
    damager.damage(damage, player);
  }

  @EventHandler
  public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
    Player player = event.getPlayer();
    if (!playerHasKit(player)) {
      return;
    }

    if (event.isSneaking()) {
      enterCactus(player);
    } else {
      leaveCactus(player);
    }
  }

  private HashMap<UUID, ArrayList<BlockDisplay>> cactusPlayerMapping = new HashMap<>();
  private HashMap<UUID, Boolean> cactusPlayerLocked = new HashMap<>();

  private void enterCactus(Player player) {
    UUID uuid = player.getUniqueId();

    for (Player p : Bukkit.getOnlinePlayers()) {
      p.hidePlayer(plugin, player);
    }

    player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1, 1);

    cactusPlayerLocked.put(uuid, true);

    if (!cactusPlayerMapping.containsKey(uuid)) {
      cactusPlayerMapping.put(uuid, new ArrayList<BlockDisplay>());
    }

    Location blockLocation = player.getLocation().getBlock().getLocation();
    World world = player.getWorld();
    for (int i = 0; i < 3; i++) {
      Location relativeLocation = blockLocation.clone().add(0, i, 0);
      BlockDisplay blockDisplay =
          (BlockDisplay) world.spawnEntity(relativeLocation, EntityType.BLOCK_DISPLAY);
      blockDisplay.setBlock(Material.CACTUS.createBlockData());
      cactusPlayerMapping.get(uuid).add(blockDisplay);
    }
  }

  private void leaveCactus(Player player) {
    cactusPlayerLocked.put(player.getUniqueId(), false);

    for (Player p : Bukkit.getOnlinePlayers()) {
      p.showPlayer(plugin, player);
    }

    UUID uuid = player.getUniqueId();
    for (BlockDisplay blockDisplay : cactusPlayerMapping.get(uuid)) {
      blockDisplay.remove();
    }
  }

  @EventHandler
  public void onPlayerDeath(PlayerDeathEvent event) {
    if (!(playerHasKit(event.getPlayer()))) {
      return;
    }

    leaveCactus(event.getPlayer());
  }

  @EventHandler
  public void onPlayerLeave(PlayerQuitEvent event) {
    Player player = event.getPlayer();
    if (!playerHasKit(player)) {
      return;
    }

    leaveCactus(player);
  }

  @EventHandler
  public void onPlayerMove(PlayerMoveEvent event) {
    if (!event.hasChangedBlock()) {
      return;
    }

    Player player = event.getPlayer();
    if (!playerHasKit(player)) {
      return;
    }

    if (cactusPlayerLocked.get(player.getUniqueId()) == null
        || !cactusPlayerLocked.get(player.getUniqueId())) {
      return;
    }

    event.setCancelled(true);
  }
}
