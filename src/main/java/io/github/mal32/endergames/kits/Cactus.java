package io.github.mal32.endergames.kits;

import io.github.mal32.endergames.EnderGames;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import org.bukkit.*;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;

public class Cactus extends AbstractKit {
  private final HashMap<UUID, ArrayList<BlockDisplay>> cactusPlayerMapping = new HashMap<>();
  private final HashMap<UUID, Boolean> cactusPlayerLocked = new HashMap<>();

  public Cactus(EnderGames plugin) {
    super(plugin);
  }

  @Override
  public void start(Player player) {
    player
        .getInventory()
        .setLeggings(
            colorLeatherArmor(new ItemStack(Material.LEATHER_LEGGINGS), Color.fromRGB(3064446)));
    player
        .getInventory()
        .setBoots(colorLeatherArmor(new ItemStack(Material.LEATHER_BOOTS), Color.fromRGB(3064446)));
  }

  @EventHandler
  public void onEntityDamagedByEntity(EntityDamageByEntityEvent event) {
    if (!(event.getEntity() instanceof Player player)
        || !(event.getDamager() instanceof Damageable damager)) return;
    if (!playerCanUseThisKit(player)) return;

    if (Math.random() > 0.8) return;

    Location location = player.getLocation();
    location.getWorld().playSound(location, Sound.ENCHANT_THORNS_HIT, 1, 1);

    int damage = 1 + (int) (Math.round(Math.random() * 4));
    damager.damage(damage, player);
  }

  @EventHandler
  public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
    Player player = event.getPlayer();
    if (!playerCanUseThisKit(player)) return;

    if (event.isSneaking()) {
      boolean currentBlockEmpty = player.getLocation().getBlock().isEmpty();
      boolean standingBlockSolid = player.getLocation().clone().add(0, -1, 0).getBlock().isSolid();

      if (currentBlockEmpty && standingBlockSolid) {
        enterCactus(player);
      }
    } else {
      leaveCactus(player);
    }
  }

  private void enterCactus(Player player) {
    UUID uuid = player.getUniqueId();

    for (Player p : Bukkit.getOnlinePlayers()) {
      p.hidePlayer(plugin, player);
    }

    player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1, 1);

    cactusPlayerLocked.put(uuid, true);

    if (!cactusPlayerMapping.containsKey(uuid)) {
      cactusPlayerMapping.put(uuid, new ArrayList<>());
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

  @EventHandler(ignoreCancelled = true)
  public void onPlayerDeath(PlayerDeathEvent event) {
    if (!(playerCanUseThisKit(event.getPlayer()))) return;

    leaveCactus(event.getPlayer());
  }

  @EventHandler
  public void onPlayerLeave(PlayerQuitEvent event) {
    Player player = event.getPlayer();
    if (!playerCanUseThisKit(player)) return;

    leaveCactus(player);
  }

  @EventHandler
  public void onPlayerMove(PlayerMoveEvent event) {
    if (!event.hasChangedBlock()) return;

    Player player = event.getPlayer();
    if (!playerCanUseThisKit(player)) return;

    if (cactusPlayerLocked.get(player.getUniqueId()) == null
        || !cactusPlayerLocked.get(player.getUniqueId())) return;

    event.setCancelled(true);
  }

  @Override
  public KitDescriptionItem getDescriptionItem() {
    return new KitDescriptionItem(
        Material.CACTUS,
        "Cactus",
        "Deals thorns damage to attackers. It can sneak to disguise itself as a cactus.",
        "Green leather helmet and leggings");
  }
}
