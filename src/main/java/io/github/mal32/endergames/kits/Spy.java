package io.github.mal32.endergames.kits;

import io.github.mal32.endergames.EnderGames;
import io.github.mal32.endergames.services.KitType;
import java.util.HashMap;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Spy extends AbstractKit {
  private static final int HIT_COOLDOWN_SECONDS = 5;
  private final HashMap<UUID, SpyPlayerData> spyData = new HashMap<>();

  public Spy(EnderGames plugin) {
    super(plugin, KitType.SPY);
  }

  @Override
  public void initPlayer(Player player) {
    player
        .getInventory()
        .setHelmet(colorLeatherArmor(new ItemStack(Material.LEATHER_HELMET), Color.BLACK));
    player
        .getInventory()
        .setChestplate(colorLeatherArmor(new ItemStack(Material.LEATHER_CHESTPLATE), Color.BLACK));
    player
        .getInventory()
        .setLeggings(colorLeatherArmor(new ItemStack(Material.LEATHER_LEGGINGS), Color.BLACK));
    player
        .getInventory()
        .setBoots(colorLeatherArmor(new ItemStack(Material.LEATHER_BOOTS), Color.BLACK));
  }

  @EventHandler
  public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
    Player player = event.getPlayer();
    if (!playerCanUseThisKit(player)) return;

    UUID uuid = player.getUniqueId();
    SpyPlayerData data = spyData.computeIfAbsent(uuid, k -> new SpyPlayerData());

    // Intentonaly rough check to make it easier for players to enter spy mode
    boolean standingOnGround =
        !player.getLocation().clone().add(0, -2, 0).getBlock().isPassable()
            || !player.getLocation().clone().add(0, -0.1, 0).getBlock().isPassable();

    if (event.isSneaking() && !data.spyModeActive && standingOnGround) {
      if (System.currentTimeMillis() - data.lastHitTime < 1000 * HIT_COOLDOWN_SECONDS) {
        player.sendActionBar(Component.text("Can't use that right now", NamedTextColor.RED));
        return;
      }
      enterSpyMode(player, data);
    } else if (!event.isSneaking() && data.spyModeActive) {
      exitSpyMode(player, data);
    }
  }

  @EventHandler
  public void onDamageTaken(EntityDamageByEntityEvent event) {
    if (!(event.getEntity() instanceof Player player)) return;
    if (!playerCanUseThisKit(player)) return;

    UUID uuid = player.getUniqueId();
    SpyPlayerData data = spyData.computeIfAbsent(uuid, k -> new SpyPlayerData());

    data.lastHitTime = System.currentTimeMillis();

    if (data.spyModeActive) {
      exitSpyMode(player, data);
      player.setSneaking(false);
    }
  }

  @EventHandler
  private void onItemPickup(EntityPickupItemEvent event) {
    if (!(event.getEntity() instanceof Player player)) return;
    if (!playerCanUseThisKit(player)) return;
    SpyPlayerData data = spyData.computeIfAbsent(player.getUniqueId(), k -> new SpyPlayerData());
    if (!data.spyModeActive) return;

    event.setCancelled(true);
  }

  @EventHandler
  public void onAttackWhileInvisible(EntityDamageByEntityEvent event) {
    if (!(event.getDamager() instanceof Player attacker)) return;
    if (!playerCanUseThisKit(attacker)) return;

    UUID uuid = attacker.getUniqueId();
    SpyPlayerData data = spyData.get(uuid);

    if (data != null && data.spyModeActive) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOW)
  public void onPlayerDeath(PlayerDeathEvent event) {
    Player player = event.getPlayer();
    if (!playerCanUseThisKit(player)) return;

    UUID uuid = player.getUniqueId();
    SpyPlayerData data = spyData.get(uuid);

    if (data != null) {
      if (data.spyModeActive) {
        exitSpyMode(player, data);
      }

      spyData.remove(uuid);
    }
  }

  private void enterSpyMode(Player player, SpyPlayerData data) {
    data.inventory = player.getInventory().getContents();
    player.getInventory().clear();

    data.spyModeActive = true;

    player.addPotionEffect(
        new PotionEffect(
            PotionEffectType.INVISIBILITY, PotionEffect.INFINITE_DURATION, 0, true, false, true));

    playSound(player.getLocation());
  }

  private void exitSpyMode(Player player, SpyPlayerData data) {
    if (data.inventory != null) {
      player.getInventory().setContents(data.inventory);
    }

    data.spyModeActive = false;

    player.removePotionEffect(PotionEffectType.INVISIBILITY);

    data.inventory = null;

    playSound(player.getLocation());
  }

  private void playSound(Location location) {
    location
        .getWorld()
        .playSound(location, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, SoundCategory.PLAYERS, 1, 1.5f);
  }

  @Override
  public KitDescription getDescription() {
    return new KitDescription(
        Material.SPYGLASS,
        "Spy",
        "Become invisible while sneaking. Cannot attack while invisible and armor is removed.",
        "Black Leather Armor",
        Difficulty.MEDIUM);
  }
}

class SpyPlayerData {
  ItemStack[] inventory;
  long lastHitTime;
  boolean spyModeActive;

  SpyPlayerData() {
    this.inventory = null;
    this.lastHitTime = 0;
    this.spyModeActive = false;
  }
}
