package io.github.mal32.endergames.worlds.game.game;

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;
import io.github.mal32.endergames.AbstractModule;
import io.github.mal32.endergames.EnderGames;
import io.github.mal32.endergames.worlds.game.GameWorld;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;

/**
 * Strategy for handling player deaths: Use the normal player death event and dont cancel it to keep
 * vanilla mechanics, we just store the location the player is at. Because of immediate respawn the
 * player respawns directly to which we listen with the player post respawn event to set him to
 * spectator mode and teleport him back.
 */
public class Death extends AbstractModule {
  private final World world = Objects.requireNonNull(Bukkit.getWorld("world"));
  private NamespacedKey deathLocationKey;
  private GamePhase gamePhase;

  public Death(EnderGames plugin, GamePhase gamePhase) {
    super(plugin);

    this.gamePhase = gamePhase;

    world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
    deathLocationKey = new NamespacedKey(plugin, "deathLocation");
  }

  @EventHandler
  private void onPlayerDeath(PlayerDeathEvent event) {
    Player player = event.getPlayer();
    if (!GameWorld.playerIsInGame(player)) return;
    Location location = player.getLocation();

    event.setNewExp(0);
    event.setDroppedExp(player.getTotalExperience());
    event.setShouldDropExperience(true);

    location
        .getWorld()
        .playSound(
            location,
            Sound.ENTITY_LIGHTNING_BOLT_THUNDER,
            SoundCategory.PLAYERS,
            Float.MAX_VALUE,
            1);

    player
        .getPersistentDataContainer()
        .set(
            deathLocationKey,
            PersistentDataType.INTEGER_ARRAY,
            new int[] {location.getBlockX(), location.getBlockY(), location.getBlockZ()});

    EntityDamageEvent lastDamage = player.getLastDamageCause();
    if (lastDamage.getDamageSource().getCausingEntity() instanceof Player killer) {
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
  }

  @EventHandler
  private void onPlayerRespawn(PlayerPostRespawnEvent event) {
    Player player = event.getPlayer();
    if (!EnderGames.playerIsInGameWorld(player)) return;

    int[] rawDeathPos =
        player.getPersistentDataContainer().get(deathLocationKey, PersistentDataType.INTEGER_ARRAY);
    Location deathPos = new Location(world, rawDeathPos[0], rawDeathPos[1], rawDeathPos[2]);
    player.teleport(deathPos);

    player.setGameMode(GameMode.SPECTATOR);
    Bukkit.getScheduler().runTask(plugin, gamePhase::checkAndGameEnd);
  }

  @EventHandler
  private void onPlayerQuit(PlayerQuitEvent event) {
    Player player = event.getPlayer();
    if (!GameWorld.playerIsInGame(player)) return;

    player.setHealth(0);
  }
}
