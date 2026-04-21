package io.github.mal32.endergames.kitsystem.kits;

import io.github.mal32.endergames.kitsystem.KitUtils;
import io.github.mal32.endergames.kitsystem.api.AbstractKit;
import io.github.mal32.endergames.kitsystem.api.Difficulty;
import io.github.mal32.endergames.kitsystem.api.KitDescription;
import io.github.mal32.endergames.kitsystem.api.KitService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display.Brightness;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public class Spy extends AbstractKit {
  private static final int HIT_COOLDOWN_SECONDS = 5;
  private static final double SPY_MODE_HUGER_LOSS_PER_SECOND = 0.25;
  private static final long FOOTSTEP_COOLDOWN_MILLIS = 1000;
  private static final int FOOTSTEP_MAX_TICKS_LIVED = 50;
  private final HashMap<UUID, SpyPlayerData> spyData = new HashMap<>();
  private final ArrayList<BlockDisplay> footsteps = new ArrayList<>();
  private BukkitTask cleanupTask;
  private BukkitTask cooldownDisplayTask;

  public Spy(KitService kitService, JavaPlugin plugin) {
    super(
        new KitDescription(
            "Spy",
            Material.SPYGLASS,
            "Become invisible while sneaking. Cannot attack while invisible and armor is removed.",
            "Black Leather Armor",
            Difficulty.MEDIUM),
        kitService,
        plugin);
  }

  @Override
  public void onEnable() {
    super.onEnable();

    cleanupTask =
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::cleanupFootsteps, 20L, 20L);
    cooldownDisplayTask =
        plugin
            .getServer()
            .getScheduler()
            .runTaskTimer(plugin, this::displayCooldownProgress, 0L, 5L);
  }

  @Override
  public void onDisable() {
    super.onDisable();

    cleanupTask.cancel();
    cooldownDisplayTask.cancel();

    for (BlockDisplay footstep : footsteps) {
      footstep.remove();
    }
    footsteps.clear();
  }

  @Override
  public void initPlayer(Player player) {
    player
        .getInventory()
        .setChestplate(
            KitUtils.colorLeatherArmor(new ItemStack(Material.LEATHER_CHESTPLATE), Color.BLACK));
    player
        .getInventory()
        .setBoots(KitUtils.colorLeatherArmor(new ItemStack(Material.LEATHER_BOOTS), Color.BLACK));
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
      if (System.currentTimeMillis() - data.lastHitTime < 1000 * HIT_COOLDOWN_SECONDS) return;
      enterSpyMode(player, data);
    } else if (!event.isSneaking() && data.spyModeActive) {
      exitSpyMode(player, data);
    }
  }

  @EventHandler
  public void onSpyDamageTaken(EntityDamageByEntityEvent event) {
    if (!(event.getEntity() instanceof Player player)) return;
    if (!(event.getDamager() instanceof Player)) return;
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

  @EventHandler
  private void onSpyModeMove(PlayerMoveEvent event) {
    if (!playerCanUseThisKit(event.getPlayer())) return;
    if (!event.hasChangedPosition()) return;
    SpyPlayerData data = spyData.get(event.getPlayer().getUniqueId());
    if (data == null || !data.spyModeActive) return;

    if (!event.getPlayer().getLocation().clone().add(0, -0.1, 0).getBlock().isSolid()) return;

    long currentTime = System.currentTimeMillis();
    if (currentTime - data.lastFootstepTime < FOOTSTEP_COOLDOWN_MILLIS) return;
    data.lastFootstepTime = currentTime;

    Location spawnLocation = event.getPlayer().getLocation().clone().setRotation(0, 0);
    BlockDisplay footstep =
        (BlockDisplay)
            spawnLocation
                .getWorld()
                .spawnEntity(spawnLocation, EntityType.BLOCK_DISPLAY, SpawnReason.COMMAND);
    footstep.setTransformation(
        new Transformation(
            new Vector3f(0f, 0f, 0f),
            new AxisAngle4f(0f, 0f, 0f, 1f),
            new Vector3f(0.25f, 0.01f, 0.25f),
            new AxisAngle4f(0f, 0f, 0f, 1f)));
    footstep.setBlock(Material.LIGHT_GRAY_STAINED_GLASS.createBlockData());
    footstep.setBrightness(new Brightness(8, 8));

    footsteps.add(footstep);
  }

  private void cleanupFootsteps() {
    footsteps.removeIf(
        footstep -> {
          if (footstep.getTicksLived() > FOOTSTEP_MAX_TICKS_LIVED) {
            footstep.remove();
            return true;
          }
          return false;
        });
  }

  private void enterSpyMode(Player player, SpyPlayerData data) {
    data.inventory = player.getInventory().getContents();
    player.getInventory().clear();

    data.spyModeActive = true;

    player.addPotionEffect(
        new PotionEffect(
            PotionEffectType.INVISIBILITY, PotionEffect.INFINITE_DURATION, 0, true, false, true));

    // hunger takes 1 saturation per second per 40 levels
    player.addPotionEffect(
        new PotionEffect(
            PotionEffectType.HUNGER,
            PotionEffect.INFINITE_DURATION,
            (int) (40 * SPY_MODE_HUGER_LOSS_PER_SECOND),
            true,
            false,
            true));

    playSound(player.getLocation());
  }

  private void exitSpyMode(Player player, SpyPlayerData data) {
    if (data.inventory != null) {
      player.getInventory().setContents(data.inventory);
    }

    data.spyModeActive = false;

    player.removePotionEffect(PotionEffectType.INVISIBILITY);
    player.removePotionEffect(PotionEffectType.HUNGER);

    data.inventory = null;

    playSound(player.getLocation());
  }

  private void playSound(Location location) {
    location
        .getWorld()
        .playSound(location, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, SoundCategory.PLAYERS, 1, 1.5f);
  }

  private void displayCooldownProgress() {
    long currentTime = System.currentTimeMillis();

    for (var entry : spyData.entrySet()) {
      UUID uuid = entry.getKey();
      SpyPlayerData data = entry.getValue();

      double cooldownRemainingTicks =
          ((data.lastHitTime + (HIT_COOLDOWN_SECONDS * 1000)) - currentTime) / 50;

      Player player = plugin.getServer().getPlayer(uuid);

      if (cooldownRemainingTicks < 0) {
        if (cooldownRemainingTicks >= -10) {
          player.sendActionBar(Component.text(""));
        }
        return;
      }

      if (player == null || !playerCanUseThisKit(player)) continue;

      double progress = (double) cooldownRemainingTicks / (HIT_COOLDOWN_SECONDS * 20);

      // Build progress bar
      int totalBars = 10;
      int filledBars = (int) Math.ceil(progress * totalBars);

      StringBuilder barContent = new StringBuilder();
      for (int i = 0; i < totalBars; i++) {
        if (i < filledBars) {
          barContent.append("█");
        } else {
          barContent.append("░");
        }
      }

      Component subtitle =
          Component.text("")
              .append(Component.text("Spying Blocked ").color(NamedTextColor.RED))
              .append(Component.text("[").color(NamedTextColor.DARK_RED))
              .append(Component.text(barContent.toString()).color(NamedTextColor.RED))
              .append(Component.text("] ").color(NamedTextColor.DARK_RED))
              .append(
                  Component.text(String.format("%.0fs", cooldownRemainingTicks / 20.0))
                      .color(NamedTextColor.RED));

      player.sendActionBar(subtitle);
    }
  }
}

class SpyPlayerData {
  ItemStack[] inventory;
  long lastHitTime;
  long lastFootstepTime;
  boolean spyModeActive;

  SpyPlayerData() {
    this.inventory = null;
    this.lastHitTime = 0;
    this.lastFootstepTime = 0;
    this.spyModeActive = false;
  }
}
