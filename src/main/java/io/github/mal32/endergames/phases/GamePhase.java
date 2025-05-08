package io.github.mal32.endergames.phases;

import io.github.mal32.endergames.EnderGames;
import io.github.mal32.endergames.kits.*;
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
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

public class GamePhase extends AbstractPhase implements Listener {
  private List<AbstractKit> kits = List.of(new Lumberjack(plugin), new Cat(plugin), new Cactus(plugin));
  private List<EnderChest> enderChests = new ArrayList<>();
  private final BukkitTask playerSwapTask;
  private final BukkitTask enderChestTeleportTask;

  public GamePhase(EnderGames plugin, Location spawn) {
    super(plugin, spawn);

    for (Player player : plugin.getServer().getOnlinePlayers()) {
      player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 20 * 60 * 3, 4, true));
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

    World world = spawnLocation.getWorld();

    world.setTime(0);
    world.getClearWeatherDuration();

    WorldBorder worldBorder = world.getWorldBorder();
    worldBorder.setSize(600);
    worldBorder.setSize(50, 20 * 60);
    worldBorder.setWarningDistance(10);
    worldBorder.setWarningTime(30);
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

    playerSwapTask =
        Bukkit.getScheduler().runTaskTimer(plugin, this::playerSwapTask, 20 * 60, 20 * 60);
    enderChestTeleportTask =
        Bukkit.getScheduler().runTaskTimer(plugin, this::enderChestTeleportTask, 20 * 10, 20 * 10);
  }

  private void playerSwapTask() {
    // get two distinct players
    List<Player> players =
        Bukkit.getOnlinePlayers().stream()
            .filter(player -> player.getGameMode() == GameMode.SURVIVAL)
            .collect(Collectors.toList());
    if (players.size() < 2) {
      return;
    }

    Player player1 = players.get(new Random().nextInt(players.size()));
    players.remove(player1);
    Player player2 = players.get(new Random().nextInt(players.size()));

    // swap their locations
    Location player1Location = player1.getLocation().clone();
    Location player2Location = player2.getLocation().clone();

    player1.teleport(player2Location);
    player2.teleport(player1Location);

    playerSwapEffects(player1);
    playerSwapEffects(player2);
  }

  private void playerSwapEffects(Player player) {
    Location location = player.getLocation();
    location.getWorld().playSound(location, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 0.5f);
    location.getWorld().spawnParticle(Particle.PORTAL, location, 50, 0, 0, 0);

    player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 0, true));
  }

  private void enderChestTeleportTask() {
    EnderChest enderChest = enderChests.get(new Random().nextInt(enderChests.size()));

    // get a random location near a player unobstructed by blocks
    List<Player> players =
        Bukkit.getOnlinePlayers().stream()
            .filter(player -> player.getGameMode() == GameMode.SURVIVAL)
            .collect(Collectors.toList());
    if (players.isEmpty()) {
      return;
    }
    Player player = players.get(new Random().nextInt(players.size()));

    Location location = player.getLocation().getBlock().getLocation().clone();
    // get a random location near the player
    final int range = 64;
    int xOffset = new Random().nextInt(range * 2) - range;
    int zOffset = new Random().nextInt(range * 2) - range;
    location.add(xOffset, 0, zOffset);
    location.setY(location.getWorld().getHighestBlockYAt(location));
    location.add(0, 1, 0);

    enderChest.teleport(location);
  }

  @Override
  public void stop() {
    playerSwapTask.cancel();
    enderChestTeleportTask.cancel();

    HandlerList.unregisterAll(this);
    for (AbstractKit kit : kits) {
      kit.stop();
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

    Player nearestPlayer = getNearestPlayer(player);

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
    player.setGameMode(GameMode.SPECTATOR);
    player.setHealth(20);

    abstractPlayerDeath(player);
  }

  private void abstractPlayerDeath(Player player) {
    for (ItemStack item : player.getInventory().getContents()) {
      if (item == null) {
        continue;
      }
      player.getWorld().dropItem(player.getLocation(), item);
    }
    player.getInventory().clear();

    for (Player p : Bukkit.getOnlinePlayers()) {
      player.playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1, 1);
    }

    Bukkit.getServer()
        .sendMessage(Component.text("☠ " + player.getName()).color(NamedTextColor.RED));

    if (!moreThanOnePlayersAlive()) {
      plugin.getServer().getScheduler().runTask(plugin, this::win);
    }
  }

  public Player getNearestPlayer(Player executor) {
    Player nearest = null;
    double nearestDistance = Double.MAX_VALUE;
    Location executorLocation = executor.getLocation();

    for (Player other : executor.getServer().getOnlinePlayers()) {
      if (other.equals(executor)) {
        continue;
      }
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
    abstractPlayerDeath(event.getPlayer());
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
  private void onEnderChestInteract(PlayerInteractEvent event) {
    if (event.getAction() != Action.RIGHT_CLICK_BLOCK
        || event.getClickedBlock() == null
        || event.getClickedBlock().getType() != Material.ENDER_CHEST) {
      return;
    }

    Location blockLocation = event.getClickedBlock().getLocation().clone();
    EnderChest enderChest = null;
    for (EnderChest e : enderChests) {
      if (e.getLocation().getX() == blockLocation.getX()
          && e.getLocation().getZ() == blockLocation.getZ()) {
        enderChest = e;
        break;
      }
    }
    if (enderChest == null) {
      enderChest = new EnderChest(plugin, blockLocation);
      enderChests.add(enderChest);
    }

    EnderChest finalEnderChest = enderChest;
    Bukkit.getScheduler()
        .runTask(
            plugin,
            () -> {
              event.getPlayer().openInventory(finalEnderChest.getInventory());
            });
  }

  @EventHandler
  private void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    player.setGameMode(GameMode.SPECTATOR);
  }
}

class EnderChest implements InventoryHolder {
  private final Inventory inventory;
  private Location location;

  public EnderChest(JavaPlugin plugin, Location location) {
    this.inventory = plugin.getServer().createInventory(this, 27);
    this.location = location;

    fill();
  }

  public void teleport(Location location) {
    new ArrayList<>(inventory.getViewers()).forEach(HumanEntity::closeInventory);

    destroy();
    this.location = location;
    place();
    fill();
  }

  public void destroy() {
    Block block = this.location.getBlock();

    if (!location.getChunk().isLoaded()) {
      location.getChunk().load();
    }

    block.setType(Material.AIR);
    effects();
  }

  private void place() {
    World world = location.getWorld();

    Location blockSpawnLocation = this.location.clone();
    blockSpawnLocation.setY(300);
    FallingBlock fallingBlock = (FallingBlock) world.spawnEntity(blockSpawnLocation, EntityType.FALLING_BLOCK);
    fallingBlock.setDropItem(false);
    fallingBlock.setBlockData(Bukkit.createBlockData(Material.OBSIDIAN));

    Block block = world.getBlockAt(location);
    block.setType(Material.ENDER_CHEST);

    effects();
  }

  private void effects() {
    location.getWorld().playSound(location, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 0.5f);
    location.getWorld().spawnParticle(Particle.PORTAL, location, 50, 0, 0, 0);
  }

  private void fill() {
    inventory.clear();

    LootTable lootTable = Bukkit.getLootTable(new NamespacedKey("enga", "ender_chest"));
    LootContext.Builder lootContextBuilder = new LootContext.Builder(location);
    LootContext lootContext = lootContextBuilder.build();
    lootTable.fillInventory(this.inventory, new Random(), lootContext);
  }

  @Override
  @NotNull
  public Inventory getInventory() {
    return inventory;
  }

  public Location getLocation() {
    return location;
  }
}
