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
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;

public class Cactus extends AbstractKit {
  private final HashMap<UUID, ArrayList<BlockDisplay>> cactusPlayerMapping = new HashMap<>();
  private final HashMap<UUID, Boolean> cactusPlayerLocked = new HashMap<>();
  private final HashMap<UUID, ItemStack[]> storedArmor = new HashMap<>();
  private final HashMap<UUID, ItemStack> storedOffhandItems = new HashMap<>();
  private final HashMap<UUID, ItemStack[]> storedHotbars = new HashMap<>();

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
    // Cancel attack if attacker is a disguised cactus
    if (event.getDamager() instanceof Player attacker) {
      if (playerCanUseThisKit(attacker)
          && cactusPlayerLocked.getOrDefault(attacker.getUniqueId(), false)) {
        event.setCancelled(true);
        return;
      }
    }

    if (!(event.getEntity() instanceof Player player)
        || !(event.getDamager() instanceof Damageable damager)) return;
    if (!playerCanUseThisKit(player)) return;
    // If the damaged player is disguised as a cactus, break disguise and unsneak
    if (cactusPlayerLocked.getOrDefault(player.getUniqueId(), false)) {
      leaveCactus(player);
      player.setSneaking(false);
    }

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
      boolean currentBlockEmpty = player.getLocation().clone().add(0, 1.1, 0).getBlock().isEmpty();
      boolean standingBlockSolid = player.getLocation().clone().add(0, -1, 0).getBlock().isSolid();

      if (currentBlockEmpty && standingBlockSolid) {
        enterCactus(player);
      }
    } else {
      leaveCactus(player);
    }
  }

  // Cancel everything when inside the cactus -------------------
  @EventHandler(ignoreCancelled = true)
  public void onPlayerInteract(PlayerInteractEvent event) {
    Player player = event.getPlayer();
    if (playerCanUseThisKit(player)
        && cactusPlayerLocked.getOrDefault(player.getUniqueId(), false)) {
      event.setCancelled(true);
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
    Player player = event.getPlayer();
    if (playerCanUseThisKit(player)
        && cactusPlayerLocked.getOrDefault(player.getUniqueId(), false)) {
      event.setCancelled(true);
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void onBlockBreak(BlockBreakEvent event) {
    Player player = event.getPlayer();
    if (playerCanUseThisKit(player)
        && cactusPlayerLocked.getOrDefault(player.getUniqueId(), false)) {
      event.setCancelled(true);
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void onBlockPlace(BlockPlaceEvent event) {
    Player player = event.getPlayer();
    if (playerCanUseThisKit(player)
        && cactusPlayerLocked.getOrDefault(player.getUniqueId(), false)) {
      event.setCancelled(true);
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void onProjectileLaunch(ProjectileLaunchEvent event) {
    ProjectileSource shooter = event.getEntity().getShooter();
    if (!(shooter instanceof Player player)) return;

    if (playerCanUseThisKit(player)
        && cactusPlayerLocked.getOrDefault(player.getUniqueId(), false)) {
      event.setCancelled(true);
    }
  }

  // ----------------------------------------

  @EventHandler
  public void onPlayerItemHeld(PlayerItemHeldEvent event) {
    Player player = event.getPlayer();
    UUID uuid = player.getUniqueId();

    if (!playerCanUseThisKit(player)) return;
    if (!cactusPlayerLocked.getOrDefault(uuid, false)) return;

    // Cancel switching
    event.setCancelled(true);

    // Force back to previous slot on next tick (or immediately)
    Bukkit.getScheduler()
        .runTask(
            plugin,
            () -> {
              player.getInventory().setHeldItemSlot(event.getPreviousSlot());
              player.updateInventory();
            });
  }

  private void enterCactus(Player player) {
    UUID uuid = player.getUniqueId();

    // cant use hideplayer he because that makes the cactus invincible
    player.addPotionEffect(
        new PotionEffect(
            PotionEffectType.INVISIBILITY, PotionEffect.INFINITE_DURATION, 0, false, false, false));

    player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1, 1);

    ItemStack[] hotbar = new ItemStack[9];
    for (int i = 0; i < 9; i++) {
      hotbar[i] = player.getInventory().getItem(i);
    }
    storedHotbars.put(uuid, hotbar);
    storedArmor.put(uuid, player.getInventory().getArmorContents());
    storedOffhandItems.put(uuid, player.getInventory().getItemInOffHand());
    // Fill hotbar with cactus blocks except items in hand
    ItemStack cactusBlock = new ItemStack(Material.CACTUS);
    for (int i = 0; i < 9; i++) {
      player.getInventory().setItem(i, cactusBlock);
    }
    player.getInventory().setItemInOffHand(null);
    player.getInventory().setItemInMainHand(null);

    // Give cactus blocks as armor pieces (or cactus items)
    player.getInventory().setHelmet(null);
    player.getInventory().setChestplate(cactusBlock);
    player.getInventory().setLeggings(cactusBlock);
    player.getInventory().setBoots(cactusBlock);

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
    UUID uuid = player.getUniqueId();
    cactusPlayerLocked.put(player.getUniqueId(), false);

    player.removePotionEffect(PotionEffectType.INVISIBILITY);

    if (storedHotbars.containsKey(uuid)) {
      ItemStack[] hotbar = storedHotbars.get(uuid);
      for (int i = 0; i < 9; i++) {
        player.getInventory().setItem(i, hotbar[i]);
      }
      storedHotbars.remove(uuid);
    }
    // restore armor and Items
    player.getInventory().setChestplate(null);
    player.getInventory().setLeggings(null);
    player.getInventory().setBoots(null);
    if (storedArmor.containsKey(uuid)) {
      player.getInventory().setArmorContents(storedArmor.get(uuid));
      storedArmor.remove(uuid);
    }
    if (storedOffhandItems.containsKey(uuid)) {
      player.getInventory().setItemInOffHand(storedOffhandItems.get(uuid));
      storedOffhandItems.remove(uuid);
    }

    ArrayList<BlockDisplay> blocks = cactusPlayerMapping.get(uuid);
    if (blocks != null) {
      for (BlockDisplay blockDisplay : blocks) {
        blockDisplay.remove();
      }
      blocks.clear();
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
  public void onPlayerTeleport(PlayerTeleportEvent event) {
    Player player = event.getPlayer();
    if (!playerCanUseThisKit(player)) return;
    if (cactusPlayerLocked.getOrDefault(player.getUniqueId(), false) == false) {
      return;
    } else {
      leaveCactus(player);
      Bukkit.getScheduler()
          .runTask(
              plugin,
              () -> {
                boolean currentBlockEmpty =
                    player.getLocation().clone().add(0, 1.1, 0).getBlock().isEmpty();
                boolean standingBlockSolid =
                    player.getLocation().clone().add(0, -1, 0).getBlock().isSolid();
                if (currentBlockEmpty && standingBlockSolid) {
                  enterCactus(player);
                }
              });
    }
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
        "Green leather helmet and leggings",
        Difficulty.EASY);
  }
}
