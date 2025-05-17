package io.github.mal32.endergames.worlds.game.game;

import io.github.mal32.endergames.EnderGames;
import io.github.mal32.endergames.kits.AbstractKit;
import io.github.mal32.endergames.worlds.game.AbstractPhase;
import io.github.mal32.endergames.worlds.game.GameManager;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.LodestoneTracker;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class GamePhase extends AbstractPhase {
  private final List<AbstractModule> modules;
  private final World world;

  public GamePhase(EnderGames plugin, GameManager manager, Location spawnLocation) {
    super(plugin, manager, spawnLocation);

    world = spawnLocation.getWorld();

    this.modules =
        List.of(
            new EnchanterManager(plugin, spawnLocation),
            new EnderChestManager(plugin),
            new PlayerRegenerationManager(plugin),
            new PlayerSwapManager(plugin));

    for (Player player : GameManager.getPlayersInGame()) {
      Bukkit.dispatchCommand(
          Bukkit.getConsoleSender(), "loot give " + player.getName() + " loot enga:tracker");

      String playerKit =
          player
              .getPersistentDataContainer()
              .get(new NamespacedKey(plugin, "kit"), PersistentDataType.STRING);
      for (AbstractKit kit : AbstractKit.getKits(plugin)) {
        if (Objects.equals(kit.getName(), playerKit)) {
          kit.start(player);
        }
      }
    }

    var worldBorder = world.getWorldBorder();
    worldBorder.setSize(50, 20 * 60);

    plugin.getServer().getScheduler().runTaskLater(plugin, this::removeSpawnPlatform, 30 * 20);

    initProtectionTime();

    for (AbstractModule module : modules) {
      module.enable();
    }
    for (AbstractKit kit : AbstractKit.getKits(plugin)) {
      kit.enable();
    }
  }

  private void removeSpawnPlatform() {
    for (int x = spawnLocation.blockX() - 20; x <= spawnLocation.blockX() + 20; x++) {
      for (int z = spawnLocation.blockZ() - 20; z <= spawnLocation.blockZ() + 20; z++) {
        for (int y = spawnLocation.blockY() - 20; y <= spawnLocation.blockY() + 20; y++) {
          world.getBlockAt(x, y, z).setType(Material.AIR);
        }
      }
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
          .runTaskLater(plugin, () -> protectionTimeBar.setProgress(progress), i * 20L);
    }

    Bukkit.getScheduler()
        .runTaskLater(plugin, protectionTimeBar::removeAll, 20 * protectionTimeDurationSeconds);

    for (Player player : GameManager.getPlayersInGame()) {
      protectionTimeBar.addPlayer(player); // TODO: disable when leaving?

      player.addPotionEffect(
          new PotionEffect(
              PotionEffectType.RESISTANCE, 20 * protectionTimeDurationSeconds, 4, true, false));
    }
  }

  @Override
  public void disable() {
    super.disable();

    for (AbstractModule module : modules) {
      module.disable();
    }
    for (AbstractKit kit : AbstractKit.getKits(plugin)) {
      kit.disable();
    }
  }

  @EventHandler
  private void onTrackerClick(PlayerInteractEvent event) {
    Player player = event.getPlayer();
    if (!GameManager.playerIsInGame(player)) return;
    ItemStack item = event.getItem();
    if (item == null || item.getType() != Material.COMPASS) {
      return;
    }

    Player nearestPlayer = getNearestValidPlayer(player);

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
    if (!EnderGames.playerIsInGameWorld(event.getEntity())) return;
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

    // TODO: not the Tracker
    for (ItemStack item : player.getInventory().getContents()) {
      if (item == null) continue;
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

    for (Player p : GameManager.getPlayersInGame()) {
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
      plugin.getServer().getScheduler().runTask(plugin, this::gameEnd);
    }
  }

  private boolean moreThanOnePlayersAlive() {
    int playersAlive = 0;
    for (Player player : GameManager.getPlayersInGame()) {
      playersAlive++; // TODO: maybe count down with every death?
    }
    return playersAlive > 1;
  }

  public Player getNearestValidPlayer(Player executor) {
    Player nearest = null;
    double nearestDistance = Double.MAX_VALUE;
    Location executorLocation = executor.getLocation();

    for (Player other : GameManager.getPlayersInGame()) {
      if (other.equals(executor)) continue;

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
    if (!GameManager.playerIsInGame(event.getPlayer())) return;

    abstractPlayerDeath(event.getPlayer(), null);
  }

  private void gameEnd() {
    Player[] survivalPlayers = GameManager.getPlayersInGame();
    Player lastPlayer = survivalPlayers[0];

    Title title =
        Title.title(
            Component.text(lastPlayer.getName() + " has Won!").color(NamedTextColor.GOLD),
            Component.text(""),
            Title.Times.times(Duration.ofSeconds(1), Duration.ofSeconds(5), Duration.ofSeconds(1)));

    for (Player player : Bukkit.getOnlinePlayers()) {
      if (!EnderGames.playerIsInGameWorld(player)) continue;

      player.showTitle(title);
    }

    lastPlayer.playSound(lastPlayer.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1, 1);

    manager.nextPhase();
  }


  @EventHandler
  private void onPlayerPlaceTNT(BlockPlaceEvent event) {
    if (!GameManager.playerIsInGame(event.getPlayer())) return;
    if (event.getBlock().getType() != Material.TNT) return;

    Block block = event.getBlock();

    block.setType(Material.AIR);

    TNTPrimed tnt =
        (TNTPrimed)
            block
                .getWorld()
                .spawnEntity(block.getLocation().clone().add(0.5, 0, 0.5), EntityType.TNT);
    tnt.setFuseTicks(30);
  }

  @EventHandler
  public void onPlayerArrowClick(PlayerInteractEvent event) {
    Player player = event.getPlayer();

    if (event.getAction() != Action.RIGHT_CLICK_AIR) return;
    if (!GameManager.playerIsInGame(player)) return;

    ItemStack item = event.getItem();
    if (item == null || item.getType() != Material.ARROW) return;

    item.setAmount(item.getAmount() - 1);

    final double speedMultiplier = 1;
    Vector direction = player.getEyeLocation().getDirection();
    Vector customVelocity = direction.normalize().multiply(speedMultiplier);

    Arrow arrow = event.getPlayer().launchProjectile(Arrow.class, customVelocity);
    arrow.setShooter(event.getPlayer());
    arrow.setDamage(1);
  }
}
