package io.github.mal32.endergames.game.game;

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;
import io.github.mal32.endergames.AbstractModule;
import io.github.mal32.endergames.EnderGames;
import io.github.mal32.endergames.game.phases.PhaseController;
import io.github.mal32.endergames.kitsystem.api.AbstractKit;
import io.github.mal32.endergames.services.PlayerInWorld;
import java.time.Duration;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.GameRules;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Strategy for handling player deaths: Use the normal player death event and don't cancel it to
 * keep vanilla mechanics, we just store the location the player is at. Because of immediate respawn
 * the player respawns directly to which we listen with the player post respawn event to set him to
 * spectator mode and teleport him back.
 */
public class Death extends AbstractModule {
  private final PhaseController controller;
  private final HashMap<UUID, Location> deathLocations = new HashMap<>();
  // Immediate respawn doesn't actually respawn the player immediately
  private final int IMMEDIATE_RESPAWN_TICKS = 10;
  private boolean gameEnded = false;

  public Death(EnderGames plugin, PhaseController controller) {
    super(plugin);

    this.controller = controller;

    World world = Objects.requireNonNull(Bukkit.getWorld("world"));
    world.setGameRule(GameRules.IMMEDIATE_RESPAWN, true);
  }

  private static String capitalizeFully(String text) {
    return Pattern.compile("\\b(\\w)").matcher(text).replaceAll(m -> m.group().toUpperCase());
  }

  private void killEffects(PlayerDeathEvent event) {
    Player player = event.getPlayer();
    Location location = player.getLocation();

    location
        .getWorld()
        .playSound(
            location,
            Sound.ENTITY_LIGHTNING_BOLT_THUNDER,
            SoundCategory.PLAYERS,
            Float.MAX_VALUE,
            1);

    event.setShowDeathMessages(true);
    EntityDamageEvent lastDamage = player.getLastDamageCause();
    if (lastDamage != null
        && lastDamage.getDamageSource().getCausingEntity() instanceof Player killer) {
      // player died by another player
      event.deathMessage(
          Component.text("")
              .append(Component.text("☠ ").color(NamedTextColor.DARK_RED))
              .append(Component.text(player.getName()).color(NamedTextColor.RED))
              .append(Component.text(" was killed by ").color(NamedTextColor.DARK_RED))
              .append(Component.text(killer.getName()).color(NamedTextColor.RED)));

      final AbstractKit killerKit = plugin.getKitService().get(killer);
      TextComponent killerInfo = Component.text(killer.getName()).color(NamedTextColor.DARK_RED);
      if (killerKit != null) {
        killerInfo =
            killerInfo
                .append(Component.text(" (").color(NamedTextColor.RED))
                .append(Component.text(killerKit.id()).color(NamedTextColor.DARK_RED))
                .append(Component.text(")").color(NamedTextColor.RED));
      }
      player.sendMessage(
          killerInfo
              .append(Component.text(" has ").color(NamedTextColor.RED))
              .append(
                  Component.text(String.format("%.2f", killer.getHealth()) + "❤")
                      .color(NamedTextColor.DARK_RED))
              .append(Component.text(" left").color(NamedTextColor.RED)));
    } else {
      // player died from non-player cause
      event.deathMessage(
          Component.text("")
              .append(Component.text("☠ ").color(NamedTextColor.DARK_RED))
              .append(Component.text(player.getName()).color(NamedTextColor.RED)));
    }
  }

  @EventHandler
  private void onPlayerDeath(PlayerDeathEvent event) {
    Player player = event.getPlayer();
    if (!PhaseController.playerIsInGame(player)) return;
    Location location = player.getLocation();

    event.setNewExp(0);
    event.setNewLevel(0);
    event.setNewTotalExp(0);
    event.setDroppedExp(player.calculateTotalExperiencePoints());
    event.setShouldDropExperience(true);

    event.setShowDeathMessages(false);

    deathLocations.put(player.getUniqueId(), location);

    // game has already ended, we are currently the winning player that has been killed
    if (gameEnded) return;

    killEffects(event);
    Bukkit.getScheduler().runTaskLater(plugin, this::checkAndGameEnd, IMMEDIATE_RESPAWN_TICKS);
  }

  @EventHandler
  private void onPlayerRespawn(PlayerPostRespawnEvent event) {
    Player player = event.getPlayer();
    if (!PlayerInWorld.GAME.is(player)) return;

    Location deathPos = deathLocations.get(player.getUniqueId());
    if (deathPos == null) return;
    player.teleport(deathPos);

    player.setGameMode(GameMode.SPECTATOR);
  }

  // Handle player quitting just like a death
  @EventHandler
  private void onPlayerQuit(PlayerQuitEvent event) {
    Player player = event.getPlayer();
    if (!PhaseController.playerIsInGame(player)) return;

    player.setHealth(0);
  }

  private void checkAndGameEnd() {
    Player[] survivalPlayers = PhaseController.getPlayersInGame();
    if (survivalPlayers.length > 1) return;
    // prevent re-triggering this when the final player gets killed to reset him
    if (gameEnded) return;
    gameEnded = true;

    Title title;
    if (survivalPlayers.length == 1) {
      Player lastPlayer = survivalPlayers[0];
      // kill surviving player to reset him, not clean but simple
      lastPlayer.setHealth(0);

      title =
          Title.title(
              Component.text(lastPlayer.getName() + " has Won!").color(NamedTextColor.GOLD),
              Component.text(""),
              Title.Times.times(
                  Duration.ofSeconds(1), Duration.ofSeconds(5), Duration.ofSeconds(1)));

      // delay win sound because the player has to respawn first
      Bukkit.getScheduler()
          .runTaskLater(
              plugin,
              () ->
                  lastPlayer.playSound(
                      lastPlayer,
                      Sound.UI_TOAST_CHALLENGE_COMPLETE,
                      SoundCategory.MASTER,
                      Float.MAX_VALUE,
                      1),
              IMMEDIATE_RESPAWN_TICKS);

    } else {
      title =
          Title.title(
              Component.text("Draw").color(NamedTextColor.GOLD),
              Component.text(""),
              Title.Times.times(
                  Duration.ofSeconds(1), Duration.ofSeconds(5), Duration.ofSeconds(1)));
    }

    for (Player player : PlayerInWorld.GAME.all()) {
      player.showTitle(title);
    }

    Bukkit.getScheduler().runTaskLater(plugin, controller::next, IMMEDIATE_RESPAWN_TICKS);
  }
}
