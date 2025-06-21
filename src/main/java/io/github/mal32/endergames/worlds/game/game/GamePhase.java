package io.github.mal32.endergames.worlds.game.game;

import io.github.mal32.endergames.AbstractModule;
import io.github.mal32.endergames.EnderGames;
import io.github.mal32.endergames.kits.AbstractKit;
import io.github.mal32.endergames.worlds.game.AbstractPhase;
import io.github.mal32.endergames.worlds.game.GameWorld;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.LodestoneTracker;
import java.time.Duration;
import java.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Furnace;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class GamePhase extends AbstractPhase {
  private final List<AbstractModule> modules;
  private final List<AbstractKit> kits;

  public GamePhase(EnderGames plugin, GameWorld manager, Location spawnLocation) {
    super(plugin, manager, spawnLocation);

    kits = AbstractKit.getKits(plugin);

    this.modules =
        List.of(
            new EnchanterManager(plugin, spawnLocation),
            new EnderChestManager(plugin, spawnLocation),
            new PlayerRegenerationManager(plugin),
            new PlayerSwapManager(plugin),
            new SwapperItem(plugin),
            new SmithingTemplateManager(plugin),
            new SpectatorParticles(plugin),
            new SpeedObsidianManager(plugin, spawnLocation));

    List<NamespacedKey> allRecipeKeys = new ArrayList<>();
    Iterator<Recipe> it = Bukkit.recipeIterator();
    while (it.hasNext()) {
      org.bukkit.inventory.Recipe recipe = it.next();
      if (recipe instanceof Keyed) {
        allRecipeKeys.add(((Keyed) recipe).getKey());
      }
    }

    for (Player player : GameWorld.getPlayersInGame()) {
      player.setGameMode(GameMode.SURVIVAL);

      player.discoverRecipes(allRecipeKeys);

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

    var worldBoarder = spawnLocation.getWorld().getWorldBorder();

    worldBoarder.setSize(50, 20 * 60);
    worldBoarder.setWarningDistance(32);
    worldBoarder.setWarningTime(60);
    worldBoarder.setDamageBuffer(1);

    int spawnPlatformRemoveDelaySeconds = EnderGames.isInDebugMode() ? 60 * 10 : 30;
    plugin
        .getServer()
        .getScheduler()
        .runTaskLater(plugin, this::removeSpawnPlatform, spawnPlatformRemoveDelaySeconds * 20);

    initProtectionTime();

    for (AbstractModule module : modules) {
      module.enable();
    }
    for (AbstractKit kit : kits) {
      kit.enable();
    }
  }

  private void removeSpawnPlatform() {
    for (int x = spawnLocation.blockX() - 20; x <= spawnLocation.blockX() + 20; x++) {
      for (int z = spawnLocation.blockZ() - 20; z <= spawnLocation.blockZ() + 20; z++) {
        for (int y = spawnLocation.blockY() - 5; y <= spawnLocation.blockY() + 5; y++) {
          spawnLocation.getWorld().getBlockAt(x, y, z).setType(Material.AIR);
        }
      }
    }
  }

  private void initProtectionTime() {
    final int protectionTimeDurationSeconds = 60 * 3;

    final BossBar protectionTimeBar =
        Bukkit.createBossBar("Protection Time", BarColor.GREEN, BarStyle.SEGMENTED_6);
    protectionTimeBar.setProgress(0.0);

    for (int i = 0; i < protectionTimeDurationSeconds; i += 10) {
      final double progress = 1 - ((double) i / protectionTimeDurationSeconds);
      Bukkit.getScheduler()
          .runTaskLater(plugin, () -> protectionTimeBar.setProgress(progress), i * 20L);
    }

    Bukkit.getScheduler()
        .runTaskLater(plugin, protectionTimeBar::removeAll, 20 * protectionTimeDurationSeconds);

    for (Player player : GameWorld.getPlayersInGame()) {
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
    for (AbstractKit kit : kits) {
      kit.disable();
    }
  }

  @EventHandler
  private void onTrackerClick(PlayerInteractEvent event) {
    Player player = event.getPlayer();
    if (!GameWorld.playerIsInGame(player)) return;
    ItemStack item = event.getItem();
    if (item == null || item.getType() != Material.COMPASS) {
      return;
    }

    Player nearestPlayer = getNearestValidPlayer(player);
    if (nearestPlayer == null) return;

    Location targetLocation = nearestPlayer.getLocation();
    Location currentLocation = player.getLocation();
    double distance = (int) currentLocation.distance(targetLocation);
    player.sendActionBar(
        Component.text(distance + " Blocks").style(Style.style(NamedTextColor.YELLOW)));
    item.setData(
        DataComponentTypes.LODESTONE_TRACKER,
        LodestoneTracker.lodestoneTracker().tracked(false).location(targetLocation).build());
  }

  @EventHandler(priority = EventPriority.HIGH)
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
              .append(
                  Component.text(String.format("%.2f", damager.getHealth()) + "❤")
                      .color(NamedTextColor.DARK_RED))
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

    for (Player p : GameWorld.getPlayersInGameWorld()) {
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
    return GameWorld.getPlayersInGame().length > 1;
  }

  @Nullable
  public Player getNearestValidPlayer(Player executor) {
    Player nearest = null;
    double nearestDistance = Double.MAX_VALUE;
    Location executorLocation = executor.getLocation();

    for (Player other : GameWorld.getPlayersInGame()) {
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
    if (!GameWorld.playerIsInGame(event.getPlayer())) return;

    abstractPlayerDeath(event.getPlayer(), null);
  }

  private void gameEnd() {
    Title title;

    Player[] survivalPlayers = GameWorld.getPlayersInGame();
    if (survivalPlayers.length >= 1) {
      Player lastPlayer = survivalPlayers[0];
      title =
          Title.title(
              Component.text(lastPlayer.getName() + " has Won!").color(NamedTextColor.GOLD),
              Component.text(""),
              Title.Times.times(
                  Duration.ofSeconds(1), Duration.ofSeconds(5), Duration.ofSeconds(1)));
      lastPlayer.playSound(lastPlayer.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1, 1);
    } else {
      title =
          Title.title(
              Component.text("Draw").color(NamedTextColor.GOLD),
              Component.text(""),
              Title.Times.times(
                  Duration.ofSeconds(1), Duration.ofSeconds(5), Duration.ofSeconds(1)));
    }

    for (Player player : GameWorld.getPlayersInGameWorld()) {
      player.showTitle(title);
    }

    manager.nextPhase();
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  private void onPlayerPlaceTNT(BlockPlaceEvent event) {
    if (!GameWorld.playerIsInGame(event.getPlayer())) return;
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
    if (!GameWorld.playerIsInGame(player)) return;

    ItemStack item = event.getItem();
    if (item == null
        || (item.getType() != Material.ARROW && item.getType() != Material.SPECTRAL_ARROW)) return;

    item.setAmount(item.getAmount() - 1);

    final double speedMultiplier = 1;
    Vector direction = player.getEyeLocation().getDirection();
    Vector customVelocity = direction.normalize().multiply(speedMultiplier);

    var arrowClass = item.getType() == Material.ARROW ? Arrow.class : SpectralArrow.class;

    AbstractArrow arrow = event.getPlayer().launchProjectile(arrowClass, customVelocity);
    arrow.setShooter(event.getPlayer());
    arrow.setDamage(1);
  }

  @EventHandler
  public void onFurnacePlace(BlockPlaceEvent event) {
    var player = event.getPlayer();
    if (!GameWorld.playerIsInGame(player)) return;

    var block = event.getBlock();
    if (block.getType() != Material.FURNACE
        && block.getType() != Material.BLAST_FURNACE
        && block.getType() != Material.SMOKER) return;
    Furnace furnace = (Furnace) block.getState();
    furnace.setCookSpeedMultiplier(4);
    furnace.update();
  }
}
