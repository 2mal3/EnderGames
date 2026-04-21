package io.github.mal32.endergames.game.phases;

import io.github.mal32.endergames.EnderGames;
import io.github.mal32.endergames.kitsystem.api.AbstractKit;
import io.github.mal32.endergames.kitsystem.api.KitDescription;
import io.github.mal32.endergames.services.PlayerInWorld;
import io.github.mal32.endergames.services.PlayerState;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.BlockVector;

public class StartPhase extends AbstractPhase {
  public StartPhase(EnderGames plugin, PhaseController controller) {
    super(plugin, controller);

    Collection<Player> participants = new ArrayList<>(List.of(PlayerState.PLAYING.all()));
    participants.addAll(List.of(PlayerState.SPECTATING.all()));
    Bukkit.getPluginManager().callEvent(new GameStartingEvent(participants));

    for (Player player : PlayerState.PLAYING.all()) {
      PlayerInWorld.GAME.set(player);
    }
    for (Player player : PlayerState.SPECTATING.all()) {
      PlayerInWorld.GAME.set(player);
    }

    Bukkit.getScheduler().runTaskLater(plugin, this::distributePlayers, 20);
    Bukkit.getScheduler().runTaskLater(plugin, this::showPlayersKitInfo, 20);
    Bukkit.getScheduler().runTaskLater(plugin, this::runCountdown, 25);

    final World world = controller.getGameWorld().getWorld();
    world.setTime(0);
    world.getWorldBorder().setSize(500);
  }

  @Override
  public AbstractPhase nextPhase() {
    return new GamePhase(plugin, controller);
  }

  public void distributePlayers() {
    int playerindex = 0;
    final int totalPlayers = PlayerState.PLAYING.all().length;
    if (totalPlayers == 0) {
      System.out.println();
      // TODO!
    }
    for (Player player : PlayerState.PLAYING.all()) {
      player.setGameMode(GameMode.ADVENTURE);
      player.getInventory().clear();

      teleportToPlayerSpawns(player, playerindex, totalPlayers);
      playerindex += 1;
    }

    barriersAroundPlayers(false);

    for (Player player : PlayerState.SPECTATING.all()) {
      player.setGameMode(GameMode.SPECTATOR);
      player.getInventory().clear();
      controller.getGameWorld().initPlayer(player);
    }
  }

  private void showPlayersKitInfo() { // TODO: move to KitManager
    for (Player player : PlayerState.PLAYING.all()) {
      final AbstractKit kit = plugin.getKitSystem().service().get(player);

      final KitDescription kitDescription = kit.description();

      Component nameMessage =
          Component.text()
              .append(Component.text("\nYou are playing as ", NamedTextColor.YELLOW))
              .append(
                  Component.text(kitDescription.displayName())
                      .color(NamedTextColor.GOLD)
                      .decorate(TextDecoration.BOLD))
              .build();
      Component abilitiesMessage =
          Component.text()
              .append(
                  Component.text("\nAbilities\n")
                      .color(NamedTextColor.GOLD)
                      .decorate(TextDecoration.BOLD))
              .append(
                  Component.text(kitDescription.abilities() + "\n").color(NamedTextColor.YELLOW))
              .build();

      player.sendMessage(nameMessage);
      player.sendMessage(abilitiesMessage);
    }
  }

  @Override
  public void disable() {
    super.disable();

    for (Player player : PhaseController.getPlayersInGame()) {
      player.clearActivePotionEffects();
    }
  }

  private void runCountdown() {
    BukkitScheduler scheduler = plugin.getServer().getScheduler();

    final int totalCountdownTimeSeconds = EnderGames.isInDebugMode() ? 5 : 20;
    var titleTimes =
        EnderGames.isInDebugMode()
            ? new Integer[] {1, 2, 3, 4, 5}
            : new Integer[] {1, 2, 3, 4, 5, 10, 15, 20};

    for (Integer titleTime : titleTimes) {
      final int scheduleTicks = (totalCountdownTimeSeconds - titleTime) * 20;
      scheduler.runTaskLater(
          plugin,
          () -> {
            for (Player player : PhaseController.getPlayersInGame()) {
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
          // Remove the start cages so players can move when the game begins.
          barriersAroundPlayers(true);

          for (Player player : PhaseController.getPlayersInGame()) {
            showTitleToPlayerWithSound(
                player,
                Component.text("Start").color(NamedTextColor.GOLD),
                Sound.BLOCK_NOTE_BLOCK_FLUTE);
          }
        },
        totalCountdownTimeSeconds * 20);
    scheduler.runTaskLater(plugin, controller::next, totalCountdownTimeSeconds * 20);
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
    final World world = controller.getGameWorld().getWorld();
    final Location spawnLocation = controller.getGameWorld().getSpawnLocation();
    double x = spawnLocation.getX() + offset.getX();
    double y = spawnLocation.getY();
    double z = spawnLocation.getZ() + offset.getZ();

    var dest = new Location(world, x, y + 1.5, z);

    Location lookTarget = spawnLocation.clone().add(0.5, 0, 0.5);
    dest.setDirection(lookTarget.toVector().subtract(dest.toVector()));

    double dx = lookTarget.getX() - x;
    double dz = lookTarget.getZ() - z;
    float yaw = dest.getYaw();
    dest.setPitch(0);

    // Teleport the player
    player.teleport(dest);
  }

  public List<BlockVector> makeSpawnOffsets() {
    return List.of(
        new BlockVector(0.5, 0, -9.5),
        new BlockVector(2.5, 0, -9.5),
        new BlockVector(4.5, 0, -8.5),
        new BlockVector(6.5, 0, -6.5),
        new BlockVector(8.5, 0, -4.5),
        new BlockVector(9.5, 0, -2.5),
        new BlockVector(9.5, 0, -0.5),
        new BlockVector(9.5, 0, 1.5),
        new BlockVector(8.5, 0, 3.5),
        new BlockVector(6.5, 0, 5.5),
        new BlockVector(4.5, 0, 7.5),
        new BlockVector(2.5, 0, 8.5),
        new BlockVector(0.5, 0, 8.5),
        new BlockVector(-1.5, 0, 8.5),
        new BlockVector(-3.5, 0, 7.5),
        new BlockVector(-5.5, 0, 5.5),
        new BlockVector(-7.5, 0, 3.5),
        new BlockVector(-8.5, 0, 1.5),
        new BlockVector(-8.5, 0, 0),
        new BlockVector(-8.5, 0, -2.5),
        new BlockVector(-7.5, 0, -4.5),
        new BlockVector(-5.5, 0, -6.5),
        new BlockVector(-3.5, 0, -8.5),
        new BlockVector(-1.5, 0, -9.5));
  }

  private void barriersAroundPlayers() {
    barriersAroundPlayers(false);
  }

  private void barriersAroundPlayers(boolean remove) {
    final World world = controller.getGameWorld().getWorld();
    final Material material = remove ? Material.AIR : Material.BARRIER;
    final int baseY = controller.getGameWorld().getSpawnLocation().getBlockY() + 1;

    for (Player player : PhaseController.getPlayersInGame()) {
      Location base = player.getLocation();
      int bx = base.getBlockX();
      int bz = base.getBlockZ();

      int[][] sides = new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
      for (int[] side : sides) {
        int x = bx + side[0];
        int z = bz + side[1];
        for (int dy = 0; dy < 2; dy++) {
          world.getBlockAt(x, baseY + dy, z).setType(material, false);
        }
      }
    }
  }
}
