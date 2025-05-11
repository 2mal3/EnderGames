package io.github.mal32.endergames.phases.game;

import io.github.mal32.endergames.EnderGames;
import io.github.mal32.endergames.kits.*;
import io.github.mal32.endergames.phases.AbstractPhase;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.LodestoneTracker;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitScheduler;

public class GamePhase extends AbstractPhase implements Listener {
  private final List<AbstractModule> modules =
      List.of(
          new EnchanterManager(plugin, spawnLocation),
          new EnderChestManager(plugin),
          new PlayerRegenerationManager(plugin),
          new PlayerSwapManager(plugin));

  public GamePhase(EnderGames plugin, Location spawn) {
    super(plugin, spawn);

    for (Player player : plugin.getServer().getOnlinePlayers()) {
      player.setGameMode(GameMode.SURVIVAL);

      Bukkit.dispatchCommand(
          Bukkit.getConsoleSender(), "loot give " + player.getName() + " loot enga:tracker");

      String playerKit =
          player
              .getPersistentDataContainer()
              .get(new NamespacedKey(plugin, "kit"), PersistentDataType.STRING);
      for (AbstractKit kit : kits) {
        if (Objects.equals(kit.getName(), playerKit)) {
          kit.start(player);
        }
      }
    }
    // Enable the event listener for all kits
    for (AbstractKit kit : kits) {
      kit.enable();
    }

    World world = spawnLocation.getWorld();

    world.setTime(0);
    world.getClearWeatherDuration();

    WorldBorder worldBorder = world.getWorldBorder();
    worldBorder.setSize(600);
    worldBorder.setSize(50, 20 * 60);
    worldBorder.setWarningDistance(32);
    worldBorder.setWarningTime(60);
    worldBorder.setDamageBuffer(1);

    BukkitScheduler scheduler = plugin.getServer().getScheduler();
    scheduler.runTaskLater(
        plugin,
        () -> {
          for (int x = spawn.blockX() - 20; x <= spawn.blockX() + 20; x++) {
            for (int z = spawn.blockZ() - 20; z <= spawn.blockZ() + 20; z++) {
              for (int y = spawn.blockY() - 20; y <= spawn.blockY() + 20; y++) {
                world.getBlockAt(x, y, z).setType(Material.AIR);
              }
            }
          }
        },
        30 * 20);

    initProtectionTime();

    for (AbstractModule module : modules) {
      module.enable();
    }
  }

  private void initProtectionTime() {
    final int protectionTimeDurationSeconds = 60 * 3;

    final BossBar protectionTimeBar =
        Bukkit.createBossBar("Protection Time", BarColor.GREEN, BarStyle.SEGMENTED_20);
    protectionTimeBar.setProgress(0.0);

    for (int i = 0; i < protectionTimeDurationSeconds; i += 10) {
      final double progress = 1 - ((double) i / protectionTimeDurationSeconds);
      Bukkit.getScheduler()
          .runTaskLater(plugin, () -> protectionTimeBar.setProgress(progress), (int) (i * 20));
    }

    Bukkit.getScheduler()
        .runTaskLater(plugin, protectionTimeBar::removeAll, 20 * protectionTimeDurationSeconds);

    for (Player player : plugin.getServer().getOnlinePlayers()) {
      protectionTimeBar.addPlayer(player);

      player.addPotionEffect(
          new PotionEffect(
              PotionEffectType.RESISTANCE, 20 * protectionTimeDurationSeconds, 4, true, false));
    }
  }

  @Override
  public void stop() {
    super.stop();

    for (AbstractKit kit : kits) {
      kit.stop();
    }
    for (AbstractModule module : modules) {
      module.disable();
    }

    WorldBorder worldBorder = spawnLocation.getWorld().getWorldBorder();
    worldBorder.setSize(600);

    for (Player player : plugin.getServer().getOnlinePlayers()) {
      player.getInventory().clear();
      player.setGameMode(GameMode.SPECTATOR);

      for (PotionEffect effect : player.getActivePotionEffects()) {
        player.removePotionEffect(effect.getType());
      }
    }
  }

  @EventHandler
  private void onTrackerClick(PlayerInteractEvent event) {
    Player player = event.getPlayer();
    ItemStack item = event.getItem();
    if (item == null || item.getType() != Material.COMPASS) {
      return;
    }

    Player nearestPlayer = getNearestSurvivalPlayer(player);

    Location targetLocation = nearestPlayer.getLocation();
    Location currentLocation = player.getLocation();
    double distance = (int) currentLocation.distance(targetLocation);
    player.sendActionBar(
        Component.text(distance + " Blocks").style(Style.style(NamedTextColor.YELLOW)));
    item.setData(
        DataComponentTypes.LODESTONE_TRACKER,
        LodestoneTracker.lodestoneTracker().tracked(false).location(targetLocation).build());
  }

  @EventHandler
  private void onPlayerDeath(PlayerDeathEvent event) {
    event.setCancelled(true);

    Player player = event.getEntity();

    Player damager = null;
    if (event.getDamageSource().getCausingEntity() instanceof Player d) {
      damager = d;
      player.sendMessage(
          Component.text("")
              .append(Component.text(damager.getName()).color(NamedTextColor.DARK_RED))
              .append(Component.text(" has ").color(NamedTextColor.RED))
              .append(Component.text(damager.getHealth() + "❤").color(NamedTextColor.DARK_RED))
              .append(Component.text(" left").color(NamedTextColor.RED)));
    }

    player.setGameMode(GameMode.SPECTATOR);
    player.setHealth(20);

    abstractPlayerDeath(player, damager);
  }

  private void abstractPlayerDeath(Player player, Player damager) {
    World world = player.getWorld();

    for (ItemStack item : player.getInventory().getContents()) {
      if (item == null) {
        continue;
      }
      world.dropItem(player.getLocation(), item);
    }
    player.getInventory().clear();

    while (player.getLevel() > 0) {
      ExperienceOrb orb =
          (ExperienceOrb) world.spawnEntity(player.getLocation(), EntityType.EXPERIENCE_ORB);
      orb.setExperience(player.getExpToLevel());
      player.setLevel(player.getLevel() - 1);
    }

    // clear the player's effects
    for (PotionEffect effect : player.getActivePotionEffects()) {
      player.removePotionEffect(effect.getType());
    }

    for (Player p : Bukkit.getOnlinePlayers()) {
      player.playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1, 1);
    }

    if (damager == null) {
      Bukkit.getServer()
          .sendMessage(
              Component.text("")
                  .append(Component.text("☠ ").color(NamedTextColor.DARK_RED))
                  .append(Component.text(player.getName()).color(NamedTextColor.RED)));
    } else {
      Bukkit.getServer()
          .sendMessage(
              Component.text("")
                  .append(Component.text("☠ ").color(NamedTextColor.DARK_RED))
                  .append(Component.text(player.getName()).color(NamedTextColor.RED))
                  .append(Component.text(" was killed by ").color(NamedTextColor.DARK_RED))
                  .append(Component.text(damager.getName()).color(NamedTextColor.RED)));
    }

    if (!moreThanOnePlayersAlive()) {
      plugin.getServer().getScheduler().runTask(plugin, this::win);
    }
  }

  public Player getNearestSurvivalPlayer(Player executor) {
    Player nearest = null;
    double nearestDistance = Double.MAX_VALUE;
    Location executorLocation = executor.getLocation();

    for (Player other : executor.getServer().getOnlinePlayers()) {
      if (other.equals(executor)) continue;
      if (other.getGameMode() == GameMode.SPECTATOR) continue;

      double distance = executorLocation.distance(other.getLocation());
      if (distance < nearestDistance) {
        nearestDistance = distance;
        nearest = other;
      }
    }
    return nearest;
  }

  @EventHandler
  private void onPlayerQuit(PlayerQuitEvent event) {
    if (event.getPlayer().getGameMode() != GameMode.SURVIVAL) return;

    abstractPlayerDeath(event.getPlayer(), null);
  }

  private void win() {
    try {
      List<Player> survivalPlayers =
          Bukkit.getOnlinePlayers().stream()
              .filter(player -> player.getGameMode() == GameMode.SURVIVAL)
              .collect(Collectors.toList());
      Player lastPlayer = survivalPlayers.getFirst();

      Title title =
          Title.title(
              Component.text(lastPlayer.getName() + " has Won!").color(NamedTextColor.GOLD),
              Component.text(""),
              Title.Times.times(
                  Duration.ofSeconds(1), Duration.ofSeconds(5), Duration.ofSeconds(1)));
      for (Player player : Bukkit.getOnlinePlayers()) {
        player.showTitle(title);
      }

      lastPlayer.playSound(lastPlayer.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1, 1);
    } catch (NoSuchElementException ignored) {
    }

    plugin.nextPhase();
  }

  private boolean moreThanOnePlayersAlive() {
    int playersAlive = 0;
    for (Player player : Bukkit.getOnlinePlayers()) {
      if (player.getGameMode() == GameMode.SURVIVAL) {
        playersAlive++;
      }
    }
    return playersAlive > 1;
  }

  @EventHandler
  private void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    player.setGameMode(GameMode.SPECTATOR);
  }

  @EventHandler
  private void onPlayerPlaceTNT(BlockPlaceEvent event) {
    if (event.getBlock().getType() != Material.TNT) {
      return;
    }

    Block block = event.getBlock();

    block.setType(Material.AIR);

    TNTPrimed tnt =
        (TNTPrimed)
            block
                .getWorld()
                .spawnEntity(block.getLocation().clone().add(0.5, 0, 0.5), EntityType.TNT);
    tnt.setFuseTicks(30);
  }
}
