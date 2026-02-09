package io.github.mal32.endergames.worlds.game.game;

import io.github.mal32.endergames.AbstractModule;
import io.github.mal32.endergames.EnderGames;
import io.github.mal32.endergames.kits.AbstractKit;
import io.github.mal32.endergames.worlds.game.AbstractPhase;
import io.github.mal32.endergames.worlds.game.GameWorld;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemEnchantments;
import java.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Furnace;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.meta.ItemMeta;
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
            new SpeedObsidianManager(plugin, spawnLocation),
            new FightDetection(plugin),
            new PotionEffectsStacking(plugin),
            new Death(plugin, manager));

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

      final ItemStack trackerItem = new ItemStack(Material.COMPASS);
      final ItemMeta trackerMeta = trackerItem.getItemMeta();
      trackerMeta.itemName(Component.text("Tracker").color(NamedTextColor.AQUA));
      trackerItem.setItemMeta(trackerMeta);
      trackerItem.setData(
          DataComponentTypes.ENCHANTMENTS,
          ItemEnchantments.itemEnchantments().add(Enchantment.VANISHING_CURSE, 1).build());
      player.getInventory().addItem(trackerItem);

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

    worldBoarder.changeSize(50, 20 * 60 * 15);
    worldBoarder.setWarningDistance(32);
    worldBoarder.setWarningTimeTicks(60 * 20);
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

    final double speedMultiplier = 1.5;
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
