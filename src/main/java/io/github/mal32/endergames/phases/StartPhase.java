package io.github.mal32.endergames.phases;

import io.github.mal32.endergames.EnderGames;
import io.github.mal32.endergames.phases.game.GamePhase;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.structure.Structure;
import org.bukkit.structure.StructureManager;
import org.bukkit.util.BlockVector;

import java.time.Duration;
import java.util.List;
import java.util.Random;

public class StartPhase extends AbstractPhase {

  public StartPhase(EnderGames plugin) {
    super(plugin);
    placeSpawnPlatform();
  }

  @Override
  public void start() {
    Bukkit.getPluginManager().registerEvents(this, plugin);
    runCountdown();
    int playerindex = 0;
    int totalPlayers = Bukkit.getServer().getOnlinePlayers().size();
    for (Player player : Bukkit.getServer().getOnlinePlayers()) {   // TODO: playing players
      teleportToPlayerSpawns(player, playerindex, totalPlayers);
      playerindex += 1;
    }
  }

  @Override
  public void stop() {
    HandlerList.unregisterAll(this);

    for (Player player : plugin.getServer().getOnlinePlayers()) {     // TODO: playing players
      player.clearActivePotionEffects();
    }
  }

  private void runCountdown() {
    BukkitScheduler scheduler = plugin.getServer().getScheduler();

    // Wait 1 second before the countdown starts
    scheduler.runTaskLater(
        plugin,
        () -> {
          for (Player player : plugin.getServer().getOnlinePlayers()) { // TODO: playing players
            player.getInventory().clear();

            for (int i = 0; i <= 9; i++) {
              int finalI = 10 - i;
              scheduler.runTaskLater(
                  plugin,
                  () -> {
                    Title title =
                        Title.title(
                            Component.text(Integer.toString(finalI)).color(NamedTextColor.YELLOW),
                            Component.text(""),
                            Title.Times.times(
                                Duration.ofMillis(5 * 50),
                                Duration.ofMillis(10 * 50),
                                Duration.ofMillis(5 * 50)));
                    player.showTitle(title);
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1, 1);
                  },
                  i * 20);
            }
            scheduler.runTaskLater(
                plugin,
                () -> {
                  Title title =
                      Title.title(
                          Component.text("Start").color(NamedTextColor.GOLD),
                          Component.text(""),
                          Title.Times.times(
                              Duration.ofMillis(5 * 50),
                              Duration.ofMillis(10 * 50),
                              Duration.ofMillis(5 * 50)));
                  player.showTitle(title);
                  player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_FLUTE, 1, 1);
                },
                10 * 20);
          }

          scheduler.runTaskLater(plugin, plugin::nextPhase, 10 * 20);
        },
        1 * 20);
  }

  private void placeSpawnPlatform() {
    StructureManager manager = Bukkit.getServer().getStructureManager();
    Structure structure = manager.loadStructure(new NamespacedKey("enga", "spawn_platform"));

    BlockVector structureSize = structure.getSize();
    Location spawnLocation = ((GamePhase) this.plugin.getPhase(EnderGames.Phase.RUNNING)).getCenter();
    int posX = (int) (spawnLocation.x() - (structureSize.getBlockX() / 2.0));
    int posZ = (int) (spawnLocation.z() - (structureSize.getBlockZ() / 2.0));
    Location location =
        new Location(spawnLocation.getWorld(), posX, spawnLocation.getY(), posZ);
    structure.place(location, true, StructureRotation.NONE, Mirror.NONE, 0, 1.0f, new Random());
  }

  protected void replaceSpawn() {
    ((GamePhase) this.plugin.getPhase(EnderGames.Phase.RUNNING)).newSpawn();
    this.placeSpawnPlatform();
  }

  private void teleportToPlayerSpawns(Player player, int playerIndex, int totalPlayers) {  // TODO: calculate with sin, cos
    if (totalPlayers == 0) return;

    List<BlockVector> offsets = makeSpawnOffsets();
    int offsetIndex = (playerIndex * offsets.size()) / totalPlayers;
    BlockVector offset = offsets.get(offsetIndex);

    Location spawnLocation = ((GamePhase) this.plugin.getPhase(EnderGames.Phase.RUNNING)).getCenter();

    int centerX = spawnLocation.getBlockX() - 1;
    int centerY = spawnLocation.getBlockY();
    int centerZ = spawnLocation.getBlockZ();

    World world = spawnLocation.getWorld();
    double x = spawnLocation.getX() + offset.getBlockX();
    double y = spawnLocation.getY();
    double z = spawnLocation.getZ() + offset.getBlockZ();
    Location dest =
        new Location(
            world,
            x - 0.5,
            y + 1.5,
            z + 0.5); // Add 0.5 on y and z to center player. Substract one from z because
    // spawnLocation is not the middle    TODO: fix spawnLocation is not the middle

    double dx = spawnLocation.getX() - x;
    double dz = spawnLocation.getZ() - z;
    float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
    dest.setYaw(yaw);
    dest.setPitch(0);

    // Teleport the player
    player.teleport(dest);
    plugin.getLogger().info("Teleporting to " + dest);
  }

  public List<BlockVector> makeSpawnOffsets() {
    // not correct yet
    return List.of(
        new BlockVector(0, 0, -9),
        new BlockVector(2, 0, -9),
        new BlockVector(4, 0, -8),
        new BlockVector(6, 0, -6),
        new BlockVector(8, 0, -4),
        new BlockVector(9, 0, -2),
        new BlockVector(9, 0, 0),
        new BlockVector(9, 0, 2),
        new BlockVector(8, 0, 4),
        new BlockVector(6, 0, 6),
        new BlockVector(4, 0, 8),
        new BlockVector(2, 0, 9),
        new BlockVector(0, 0, 9),
        new BlockVector(-2, 0, 9),
        new BlockVector(-4, 0, 8),
        new BlockVector(-6, 0, 6),
        new BlockVector(-8, 0, 4),
        new BlockVector(-9, 0, 2),
        new BlockVector(-9, 0, 0),
        new BlockVector(-9, 0, -2),
        new BlockVector(-8, 0, -4),
        new BlockVector(-6, 0, -6),
        new BlockVector(-4, 0, -8),
        new BlockVector(-2, 0, -9));
  }

  @EventHandler
  private void onPlayerMove(PlayerMoveEvent event) { // TODO: playing players
    Player player = event.getPlayer();

    Location startLocation = event.getFrom();
    Location startLocationBlock = startLocation.getBlock().getLocation();
    Location endLocation = event.getTo();
    Location endLocationBlock = endLocation.getBlock().getLocation();

    if (startLocationBlock.getX() != endLocationBlock.getX()
        || startLocationBlock.getZ() != endLocationBlock.getZ()) {
      event.getTo().setX(startLocation.getX());
      event.getTo().setZ(startLocation.getZ());
    }
  }

//  @EventHandler
//  private void onPlayerJoin(PlayerJoinEvent event) {
//    Player player = event.getPlayer();
//    player.setGameMode(GameMode.SPECTATOR);
//  }
}
