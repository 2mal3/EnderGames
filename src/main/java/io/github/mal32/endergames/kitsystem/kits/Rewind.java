package io.github.mal32.endergames.kitsystem.kits;

import io.github.mal32.endergames.EnderGames;
import io.github.mal32.endergames.game.phases.PhaseController;
import io.github.mal32.endergames.kitsystem.api.AbstractKit;
import io.github.mal32.endergames.kitsystem.api.Difficulty;
import io.github.mal32.endergames.kitsystem.api.KitDescription;
import io.github.mal32.endergames.kitsystem.api.KitService;
import java.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

public class Rewind extends AbstractKit {
  private static final int REWIND_SECONDS = 10;
  private static final int PLAYER_STATE_INTERVAL_TICKS = 10;
  private static final int USE_COOLDOWN_SECONDS = 40;
  private final NamespacedKey rewindKey;
  private final HashMap<UUID, ArrayList<PlayerState>> playerStates = new HashMap<>();
  private BukkitTask task;

  public Rewind(KitService kitService, JavaPlugin plugin) {
    super(
        new KitDescription(
            "Rewind",
            Material.CLOCK,
            "Can go back 10 seconds in time every 40 seconds.",
            "Rewind Clock",
            Difficulty.EASY),
        kitService,
        plugin);

    rewindKey = new NamespacedKey(plugin, "rewind");
  }

  @Override
  public void initPlayer(Player player) {
    var clock = ItemStack.of(Material.CLOCK);

    var meta = clock.getItemMeta();
    meta.getPersistentDataContainer().set(rewindKey, PersistentDataType.BOOLEAN, true);
    meta.itemName(Component.text("Rewind").color(NamedTextColor.GOLD));
    clock.setItemMeta(meta);
    clock.addUnsafeEnchantment(Enchantment.VANISHING_CURSE, 1);
    player.getInventory().addItem(clock);

    playerStates.put(player.getUniqueId(), new ArrayList<>());
  }

  @Override
  public void onEnable() {
    BukkitScheduler scheduler = plugin.getServer().getScheduler();
    task =
        scheduler.runTaskTimer(
            plugin, this::tick, PLAYER_STATE_INTERVAL_TICKS, PLAYER_STATE_INTERVAL_TICKS);
  }

  @Override
  public void onDisable() {
    task.cancel();
  }

  private void tick() {
    for (Player player : PhaseController.getPlayersInGame()) {
      if (!playerCanUseThisKit(player)) continue;

      var playerState =
          new PlayerState(
              player.getLocation(), player.getHealth(), player.getActivePotionEffects());

      ArrayList<PlayerState> stateRecords = playerStates.get(player.getUniqueId());
      stateRecords.addFirst(playerState);
      if (stateRecords.size() >= REWIND_SECONDS * (20 / PLAYER_STATE_INTERVAL_TICKS)) {
        stateRecords.removeLast();
      }
    }
  }

  @EventHandler
  private void onClockClick(PlayerInteractEvent event) {
    if (event.getAction() != Action.RIGHT_CLICK_AIR
        && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
    var player = event.getPlayer();
    if (!playerCanUseThisKit(player)) return;
    var item = event.getItem();
    if (item == null || item.getType() != Material.CLOCK) return;
    if (!item.getItemMeta().getPersistentDataContainer().has(rewindKey, PersistentDataType.BOOLEAN))
      return;
    if (player.hasCooldown(Material.CLOCK)) return;

    if (EnderGames.isInDebugMode()) {
      player.setCooldown(Material.CLOCK, 20);
    } else {
      player.setCooldown(Material.CLOCK, USE_COOLDOWN_SECONDS * 20);
    }

    player
        .getLocation()
        .getWorld()
        .playSound(
            player, Sound.ENTITY_ENDER_EYE_DEATH, SoundCategory.PLAYERS, Integer.MAX_VALUE, 1);
    player
        .getLocation()
        .getWorld()
        .playSound(
            player, Sound.ENTITY_ENDERMAN_AMBIENT, SoundCategory.PLAYERS, Float.MAX_VALUE, 1.5f);

    // teleport player back with nice animation
    Enderman enderman = player.getWorld().spawn(player.getLocation(), Enderman.class);
    enderman.setAI(false);
    enderman.setInvulnerable(true);
    enderman.setInvisible(true);
    enderman.setSilent(true);

    rewindStart(player, enderman);

    final int ANIMATION_SPEED_TICKS = 3;
    var stateRecords = playerStates.get(player.getUniqueId());
    int i = 0;
    for (PlayerState playerState : stateRecords) {
      Bukkit.getScheduler()
          .runTaskLater(
              plugin,
              () -> rewindTick(player, playerState, enderman),
              (long) i * ANIMATION_SPEED_TICKS);
      i++;
    }

    Bukkit.getScheduler()
        .runTaskLater(
            plugin,
            () -> rewindEnd(player, stateRecords.getLast(), enderman),
            (long) stateRecords.size() * ANIMATION_SPEED_TICKS);
  }

  private void rewindStart(Player player, Enderman enderman) {
    player.setGameMode(GameMode.SPECTATOR);

    player.setSpectatorTarget(enderman);

    player
        .getLocation()
        .getWorld()
        .playSound(player.getLocation(), Sound.ENTITY_ENDER_EYE_DEATH, 1, 1);
  }

  private void rewindTick(Player player, PlayerState playerState, Enderman enderman) {
    enderman.teleport(playerState.location());
    player
        .getLocation()
        .getWorld()
        .spawnParticle(
            Particle.END_ROD, player.getLocation().clone().add(0, 1, 0), 3, 0.2, 0.2, 0.2, 0);
  }

  private void rewindEnd(Player player, PlayerState lastState, Enderman enderman) {
    player.teleport(lastState.location());
    player.setGameMode(GameMode.SURVIVAL);
    player.setHealth(lastState.health());

    for (PotionEffect effect : player.getActivePotionEffects()) {
      player.removePotionEffect(effect.getType());
    }
    for (PotionEffect effect : lastState.potionEffects()) {
      player.addPotionEffect(effect);
    }

    player
        .getLocation()
        .getWorld()
        .playSound(player.getLocation(), Sound.ENTITY_ENDER_EYE_DEATH, 1, 1);

    enderman.remove();
  }
}

record PlayerState(Location location, double health, Collection<PotionEffect> potionEffects) {}
