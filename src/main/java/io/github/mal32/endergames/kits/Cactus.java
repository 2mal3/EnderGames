package io.github.mal32.endergames.kits;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
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
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;

public class Cactus extends AbstractKit {
  public Cactus(JavaPlugin plugin) {
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
    if (!(event.getEntity() instanceof Player player) || !(event.getDamager() instanceof Damageable damager)) return;
    if (!playerHasKit(player)) return;

    if (Math.random() > 0.8) return;

    Location location = player.getLocation();
    location.getWorld().playSound(location, Sound.ENCHANT_THORNS_HIT, 1, 1);

    int damage = 1 + (int) (Math.round(Math.random() * 4));
    damager.damage(damage, player);
  }

  @EventHandler
  public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
    Player player = event.getPlayer();
    if (!playerHasKit(player)) return;

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

  private final HashMap<UUID, ArrayList<BlockDisplay>> cactusPlayerMapping = new HashMap<>();
  private final HashMap<UUID, Boolean> cactusPlayerLocked = new HashMap<>();

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
    if (!(playerHasKit(event.getPlayer()))) return;

    leaveCactus(event.getPlayer());
  }

  @EventHandler
  public void onPlayerLeave(PlayerQuitEvent event) {
    Player player = event.getPlayer();
    if (!playerHasKit(player)) return;

    leaveCactus(player);
  }

  @EventHandler
  public void onPlayerMove(PlayerMoveEvent event) {
    if (!event.hasChangedBlock()) return;

    Player player = event.getPlayer();
    if (!playerHasKit(player)) return;

    if (cactusPlayerLocked.get(player.getUniqueId()) == null
        || !cactusPlayerLocked.get(player.getUniqueId())) return;

    event.setCancelled(true);
  }

  @Override
  public ItemStack getDescriptionItem() {
    ItemStack item = new ItemStack(Material.CACTUS, 1);
    ItemMeta meta = item.getItemMeta();
    meta.displayName(
        Component.text("Cactus")
            .color(NamedTextColor.GOLD)
            .decoration(TextDecoration.ITALIC, false));
    meta.lore(
        Arrays.asList(
            Component.text("Abilities:")
                .decorate(TextDecoration.UNDERLINED)
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false),
            Component.text("Deals thorns damage to attackers.")
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false),
            Component.text("It can sneak to disguise itself as a cactus.")
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false),
            Component.text(" "), // Empty line — no styling needed
            Component.text("Equipment:")
                .decorate(TextDecoration.UNDERLINED)
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false),
            Component.text("Green leather helmet and leggings.")
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false)));
    item.setItemMeta(meta);

    return item;
  }
}
