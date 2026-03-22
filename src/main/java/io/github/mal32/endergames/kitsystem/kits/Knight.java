package io.github.mal32.endergames.kitsystem.kits;

import io.github.mal32.endergames.kitsystem.api.AbstractKit;
import io.github.mal32.endergames.kitsystem.api.Difficulty;
import io.github.mal32.endergames.kitsystem.api.KitDescription;
import io.github.mal32.endergames.kitsystem.api.KitService;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityMountEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.HorseInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootTables;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

// The orphaned horses are intentionally kept alive since they won't cause any problems
public class Knight extends AbstractKit {
  private static final int HORSE_RESPAWN_INTERVAL_SECONDS = 30;
  private static final int HORSE_TETHER_INTERVAL_SECONDS = 10;
  private static final double MAX_MOUNT_DISTANCE = 32;
  private static final double HORSE_SPEED = 0.19;
  private static final double HORSE_JUMP_HIGHT = 0.9;
  private static final int HORSE_HEALTH = 20;

  private final Map<UUID, Horse> mounts = new HashMap<>();
  private BukkitTask horseRespawnTask;
  private BukkitTask horseTetherTask;

  public Knight(KitService kitService, JavaPlugin plugin) {
    super(
        new KitDescription(
            "Knight",
            Material.IRON_SPEAR,
            "Rides into battle atop a regenerating warhorse clad in iron armor.",
            "Full Golden Armor, Iron Spear",
            Difficulty.HARD),
        kitService,
        plugin);
  }

  @Override
  public void onEnable() {
    var scheduler = plugin.getServer().getScheduler();
    horseRespawnTask =
        scheduler.runTaskTimer(
            plugin,
            this::horseRespawn,
            HORSE_RESPAWN_INTERVAL_SECONDS * 20,
            HORSE_RESPAWN_INTERVAL_SECONDS * 20);
    horseTetherTask =
        scheduler.runTaskTimer(
            plugin,
            this::horseTether,
            HORSE_TETHER_INTERVAL_SECONDS * 20,
            HORSE_TETHER_INTERVAL_SECONDS * 20);
  }

  @Override
  public void onDisable() {
    horseRespawnTask.cancel();
    horseTetherTask.cancel();

    mounts.clear();
  }

  @Override
  public void initPlayer(Player player) {
    var inventory = player.getInventory();
    inventory.setHelmet(ItemStack.of(Material.GOLDEN_HELMET));
    inventory.setChestplate(ItemStack.of(Material.GOLDEN_CHESTPLATE));
    inventory.setLeggings(ItemStack.of(Material.GOLDEN_LEGGINGS));
    inventory.setBoots(ItemStack.of(Material.GOLDEN_BOOTS));

    ItemStack spear = ItemStack.of(Material.IRON_SPEAR);
    spear.addEnchantment(Enchantment.UNBREAKING, 1);
    spear.addEnchantment(Enchantment.VANISHING_CURSE, 1);
    inventory.addItem(spear);

    spawnHorse(player, true);
  }

  @EventHandler
  public void onPlayerDeath(PlayerDeathEvent event) {
    mounts.remove(event.getPlayer().getUniqueId());
  }

  @EventHandler
  public void onPlayerMount(EntityMountEvent event) {
    if (!(event.getEntity() instanceof Player player)) return;
    if (!(event.getMount() instanceof Horse horse)) return;

    // don't protect horses from non-knight players
    if (!mounts.containsValue(horse)) return;
    // check if the player is the rightful owner
    // using .equals here since the paperapi doesn't guarantee the same entity instance for the same
    // entity
    Horse playerHorse = mounts.get(player.getUniqueId());
    if (playerHorse != null && playerHorse.equals(horse)) return;

    horse
        .getLocation()
        .getWorld()
        .playSound(horse.getLocation(), Sound.ENTITY_HORSE_ANGRY, SoundCategory.NEUTRAL, 1, 1);
    Component actionBarMessage =
        Component.text("You are not noble enough to ride this steed!").color(NamedTextColor.RED);
    player.sendActionBar(actionBarMessage);

    event.setCancelled(true);
  }

  private void horseRespawn() {
    for (UUID playerId : mounts.keySet()) {
      Player player = Bukkit.getPlayer(playerId);
      if (player == null) {
        continue;
      }

      Horse horse = mounts.get(playerId);
      if (!horseExists(horse)) {
        spawnHorse(player, false);
      }
    }
  }

  private void horseTether() {
    for (UUID playerId : mounts.keySet()) {
      Player player = Bukkit.getPlayer(playerId);
      if (player == null) {
        continue;
      }

      Horse horse = mounts.get(playerId);
      if (!horseExists(horse)) continue;

      if (horse.getLocation().distance(player.getLocation()) > MAX_MOUNT_DISTANCE) {
        teleportHorseNearPlayer(player, horse);
      }
    }
  }

  private void spawnHorse(Player player, boolean start) {
    Location spawnLocation = player.getLocation().clone();
    spawnLocation.setY(spawnLocation.getWorld().getMaxHeight());

    Horse horse = player.getWorld().spawn(spawnLocation, Horse.class);
    horse.setTamed(true);
    horse.setOwner(player);
    horse.setAdult();
    horse.setAgeLock(true);
    horse.setLootTable(LootTables.ENDERMITE.getLootTable());

    // Buffs
    horse.setJumpStrength(HORSE_JUMP_HIGHT);
    horse.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(HORSE_SPEED);
    PotionEffect regen =
        new PotionEffect(
            PotionEffectType.REGENERATION, PotionEffect.INFINITE_DURATION, 1, true, false, false);
    horse.addPotionEffect(regen);
    var maxHealth = horse.getAttribute(Attribute.MAX_HEALTH);
    maxHealth.setBaseValue(HORSE_HEALTH);
    horse.setHealth(maxHealth.getBaseValue());
    if (start) {
      horse.addPotionEffect(
          new PotionEffect(PotionEffectType.RESISTANCE, 20 * 60 * 3, 4, true, false, false));
    }

    // Inventory
    HorseInventory inventory = horse.getInventory();
    var saddle = ItemStack.of(Material.SADDLE);
    saddle.addUnsafeEnchantments(
        Map.of(Enchantment.VANISHING_CURSE, 1, Enchantment.BINDING_CURSE, 1));
    inventory.setSaddle(saddle);
    var armor = ItemStack.of(Material.IRON_HORSE_ARMOR);
    armor.addUnsafeEnchantment(Enchantment.VANISHING_CURSE, 1);
    inventory.setArmor(armor);

    mounts.put(player.getUniqueId(), horse);
    teleportHorseNearPlayer(player, horse);
  }

  private boolean horseExists(@Nullable Horse horse) {
    if (horse == null) return false;
    return horse.isValid() && !horse.isDead();
  }

  private void teleportHorseNearPlayer(Player player, Horse horse) {
    int randomXModifier = (int) (Math.random() * 8) - 4;
    int randomZModifier = (int) (Math.random() * 8) - 4;
    Location targetLocation = player.getLocation().clone().add(randomXModifier, 0, randomZModifier);
    targetLocation.setY(
        targetLocation
            .getWorld()
            .getHighestBlockAt(targetLocation, HeightMap.MOTION_BLOCKING)
            .getY());
    targetLocation.add(0, 1, 0);

    horse.teleport(targetLocation);

    targetLocation
        .getWorld()
        .playSound(targetLocation, Sound.ENTITY_HORSE_AMBIENT, SoundCategory.PLAYERS, 1f, 1f);
  }
}
