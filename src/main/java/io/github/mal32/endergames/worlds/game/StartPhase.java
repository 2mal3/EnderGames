package io.github.mal32.endergames.worlds.game;

import io.github.mal32.endergames.EnderGames;
import java.time.Duration;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.BlockVector;

public class StartPhase extends AbstractPhase {
  public StartPhase(EnderGames plugin, GameManager manager, Location spawnLocation) {
    super(plugin, manager, spawnLocation);

    this.manager.getWorldManager().prepareWorldForGame();

    for (Player player : Bukkit.getOnlinePlayers()) {
      player
          .getPersistentDataContainer()
          .set(new NamespacedKey("endergames", "world"), PersistentDataType.STRING, "game"); // TODO
    }

    Bukkit.getScheduler().runTaskLater(plugin, this::distributePlayers, 20);
    Bukkit.getScheduler().runTaskLater(plugin, this::runCountdown, 25);
  }

  public void distributePlayers() {
    int playerindex = 0;
    final int totalPlayers = Bukkit.getServer().getOnlinePlayers().size();
    for (Player player : GameManager.getPlayersInGameWorld()) { // TODO: playing players
      player.setGameMode(GameMode.ADVENTURE);
      player.getInventory().clear();

      teleportToPlayerSpawns(player, playerindex, totalPlayers);
      playerindex += 1;
    }
  }

  @Override
  public void disable() {
    super.disable();

    for (Player player : GameManager.getPlayersInGame()) {
      player.clearActivePotionEffects();
    }
  }

  private void runCountdown() {
    BukkitScheduler scheduler = plugin.getServer().getScheduler();

    final int totalCountdownTimeSeconds = 30;
    var titleTimes = new Integer[] {1, 2, 3, 4, 5, 10, 15, 20, 25, 30};

    for (Integer titleTime : titleTimes) {
      final int scheduleTicks = (totalCountdownTimeSeconds - titleTime) * 20;
      scheduler.runTaskLater(
          plugin,
          () -> {
            for (Player player : GameManager.getPlayersInGame()) {
              showTitleToPlayerWithSound(
                  player,
                  Component.text(titleTime + "").color(NamedTextColor.YELLOW),
                  Sound.BLOCK_NOTE_BLOCK_HARP);
            }
          },
          scheduleTicks);
    }

    // Game Start
    scheduler.runTaskLater(
        plugin,
        () -> {
          for (Player player : GameManager.getPlayersInGame()) {
            showTitleToPlayerWithSound(
                player,
                Component.text("Start").color(NamedTextColor.GOLD),
                Sound.BLOCK_NOTE_BLOCK_FLUTE);
          }
        },
        totalCountdownTimeSeconds * 20);
    scheduler.runTaskLater(plugin, manager::nextPhase, totalCountdownTimeSeconds * 20);
  }

  private void showTitleToPlayerWithSound(Player player, Component text, Sound sound) {
    final Title.Times titleTime =
        Title.Times.times(
            Duration.ofMillis(5 * 50), Duration.ofMillis(10 * 50), Duration.ofMillis(5 * 50));

    Title title = Title.title(text, Component.text(""), titleTime);
    player.showTitle(title);
    player.playSound(player.getLocation(), sound, 1, 1);
  }

  private void teleportToPlayerSpawns(Player player, int playerIndex, int totalPlayers) {
    if (totalPlayers == 0) return;

    List<BlockVector> offsets = makeSpawnOffsets();
    int offsetIndex = (playerIndex * offsets.size()) / totalPlayers;
    BlockVector offset = offsets.get(offsetIndex);

    World world = spawnLocation.getWorld();
    double x = spawnLocation.getX() + offset.getBlockX();
    double y = spawnLocation.getY();
    double z = spawnLocation.getZ() + offset.getBlockZ();
    Location dest =
        new Location(
            world, x - 0.5, y + 1.5,
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
  private void onPlayerMove(PlayerMoveEvent event) {
    if (!GameManager.playerIsInGame(event.getPlayer())) return;

    Location startLocation = event.getFrom();
    event.getTo().setX(startLocation.getX());
    event.getTo().setZ(startLocation.getZ());
  }
}
