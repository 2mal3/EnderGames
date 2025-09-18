package io.github.mal32.endergames.worlds.game.game;

import io.github.mal32.endergames.AbstractModule;
import io.github.mal32.endergames.EnderGames;
import io.github.mal32.endergames.kits.AbstractKit;
import io.github.mal32.endergames.worlds.game.AbstractPhase;
import io.github.mal32.endergames.worlds.game.GameWorld;
import java.time.Duration;
import java.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

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
            new Tracker(plugin),
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
        if (Objects.equals(kit.getNameLowercase(), playerKit)) {
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

  @EventHandler(priority = EventPriority.HIGH)
  private void onFatalPlayerDamage(EntityDamageEvent event) {
    if (!(event.getEntity() instanceof Player player)) return;
    if (!GameWorld.playerIsInGame(player)) return;
    double damage = event.getFinalDamage();
    if (player.getHealth() - damage > 0) return;

    event.setCancelled(true);

    Player damager = null;
    if (event instanceof EntityDamageByEntityEvent ede && ede.getDamager() instanceof Player d) {
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

    abstractPlayerDeath(player, damager);
  }

  @EventHandler
  private void onPlayerQuit(PlayerQuitEvent event) {
    if (!GameWorld.playerIsInGame(event.getPlayer())) return;

    abstractPlayerDeath(event.getPlayer(), null);
  }

  private void abstractPlayerDeath(Player player, Player damager) {
    resetPlayer(player);

    for (Player p : GameWorld.getPlayersInGameWorld()) {
      player.playSound(p, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 1, 1);
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

    Bukkit.getScheduler()
        .runTask(
            plugin,
            () -> {
              boolean moreThanOnePlayersAlive = GameWorld.getPlayersInGame().length > 1;
              if (!moreThanOnePlayersAlive) {
                gameEnd();
              }
            });
  }

  private void resetPlayer(Player player) {
    World world = player.getWorld();

    for (ItemStack item : player.getInventory().getContents()) {
      if (item == null) continue;
      if (item.getType() == Material.COMPASS) continue;
      world.dropItem(player.getLocation(), item);
    }
    player.getInventory().clear();

    while (player.getLevel() > 0) {
      ExperienceOrb orb =
          (ExperienceOrb) world.spawnEntity(player.getLocation(), EntityType.EXPERIENCE_ORB);
      orb.setExperience(player.getExpToLevel());
      player.setLevel(player.getLevel() - 1);
    }

    for (PotionEffect effect : player.getActivePotionEffects()) {
      player.removePotionEffect(effect.getType());
    }

    player.setGameMode(GameMode.SPECTATOR);
    player.setHealth(20);
  }

  private void gameEnd() {
    Title title;

    Player[] survivalPlayers = GameWorld.getPlayersInGame();

    for (Player p : survivalPlayers) {
      resetPlayer(p);
    }

    if (survivalPlayers.length >= 1) {
      Player lastPlayer = survivalPlayers[0];
      title =
          Title.title(
              Component.text(lastPlayer.getName() + " has Won!").color(NamedTextColor.GOLD),
              Component.text(""),
              Title.Times.times(
                  Duration.ofSeconds(1), Duration.ofSeconds(5), Duration.ofSeconds(1)));
      lastPlayer.playSound(
          lastPlayer, Sound.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.MASTER, 1, 1);
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

    if (event.getPlayer().isSneaking()) return;

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
