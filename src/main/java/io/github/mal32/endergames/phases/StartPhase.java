package io.github.mal32.endergames.phases;

import io.github.mal32.endergames.EnderGames;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.time.Duration;

public class StartPhase extends AbstractPhase {
  public StartPhase(EnderGames plugin, Location spawn) {
    super(plugin, spawn);

    BukkitScheduler scheduler = plugin.getServer().getScheduler();

    for (Player player : plugin.getServer().getOnlinePlayers()) {
      player.getInventory().clear();

      for (int i = 0; i <= 4; i++) {
        int finalI = 5 - i;
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
          5 * 20);
    }

    scheduler.runTaskLater(plugin, plugin::nextPhase, 5 * 20);
  }

  @Override
  public void stop() {
    super.stop();

    for (Player player : plugin.getServer().getOnlinePlayers()) {
      player.clearActivePotionEffects();
    }
  }

  @EventHandler
  private void onPlayerMove(PlayerMoveEvent event) {
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

  @EventHandler
  private void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    player.setGameMode(GameMode.SPECTATOR);
  }
}
