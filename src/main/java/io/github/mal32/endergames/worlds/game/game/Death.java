package io.github.mal32.endergames.worlds.game.game;

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;
import io.github.mal32.endergames.AbstractModule;
import io.github.mal32.endergames.EnderGames;
import io.github.mal32.endergames.worlds.game.GameWorld;
import java.time.Duration;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
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
 * Strategy for handling player deaths: Use the normal player death event and dont cancel it to keep
 * vanilla mechanics, we just store the location the player is at. Because of immediate respawn the
 * player respawns directly to which we listen with the player post respawn event to set him to
 * spectator mode and teleport him back.
 */
public class Death extends AbstractModule {
  private final World world = Objects.requireNonNull(Bukkit.getWorld("world"));
  private GameWorld manager;
  private final HashMap<UUID, Location> deathLocations = new HashMap<>();

  public Death(EnderGames plugin, GameWorld manager) {
    super(plugin);

    this.manager = manager;

    world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
  }

  @EventHandler
  private void onPlayerDeath(PlayerDeathEvent event) {
    Player player = event.getPlayer();
    if (!GameWorld.playerIsInGame(player)) return;
    Location location = player.getLocation();

    event.setNewExp(0);
    event.setNewLevel(0);
    event.setNewTotalExp(0);
    event.setDroppedExp(player.calculateTotalExperiencePoints());
    event.setShouldDropExperience(true);

    location
        .getWorld()
        .playSound(
            location,
            Sound.ENTITY_LIGHTNING_BOLT_THUNDER,
            SoundCategory.PLAYERS,
            Float.MAX_VALUE,
            1);

    deathLocations.put(player.getUniqueId(), location);

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

      player.sendMessage(
          Component.text("")
              .append(Component.text(killer.getName()).color(NamedTextColor.DARK_RED))
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

    Bukkit.getScheduler().runTask(plugin, this::checkAndGameEnd);
  }

  @EventHandler
  private void onPlayerRespawn(PlayerPostRespawnEvent event) {
    Player player = event.getPlayer();
    if (!EnderGames.playerIsInGameWorld(player)) return;

    Location deathPos = deathLocations.get(player.getUniqueId());
    if (deathPos == null) return;
    player.teleport(deathPos);

    player.setGameMode(GameMode.SPECTATOR);
  }

  // Handle player quitting just like a death
  @EventHandler
  private void onPlayerQuit(PlayerQuitEvent event) {
    Player player = event.getPlayer();
    if (!GameWorld.playerIsInGame(player)) return;

    player.setHealth(0);
  }

  public void checkAndGameEnd() {
    Player[] survivalPlayers = GameWorld.getPlayersInGame();

    Title title;
    if (survivalPlayers.length >= 1) {
      // kill surviving player to reset him, not clean but simple
      for (Player p : survivalPlayers) {
        p.setHealth(0);
      }

      Player lastPlayer = survivalPlayers[0];
      title =
          Title.title(
              Component.text(lastPlayer.getName() + " has Won!").color(NamedTextColor.GOLD),
              Component.text(""),
              Title.Times.times(
                  Duration.ofSeconds(1), Duration.ofSeconds(5), Duration.ofSeconds(1)));
      lastPlayer.playSound(
          lastPlayer, Sound.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.MASTER, Float.MAX_VALUE, 1);
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
}
