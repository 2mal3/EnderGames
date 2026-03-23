package io.github.mal32.endergames.kitsystem.api;

import io.github.mal32.endergames.game.phases.GameEndEvent;
import io.github.mal32.endergames.game.phases.GameStartEvent;
import io.github.mal32.endergames.kitsystem.kits.Lumberjack;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

/**
 * Central controller for the kit system.
 *
 * <p>Handles:
 *
 * <ul>
 *   <li>Kit activation at game start
 *   <li>Kit deactivation at game end
 *   <li>Assigning a default kit ({@link Lumberjack}) to new players or players with invalid kits
 * </ul>
 *
 * <h2>Game Integration</h2>
 *
 * <ul>
 *   <li>On {@link GameStartEvent}: initializes players and enables all used kits
 *   <li>On {@link GameEndEvent}: disables all active kits
 *   <li>On {@link PlayerJoinEvent}: ensures the player has a valid kit
 * </ul>
 *
 * <p>This class owns a {@link KitManager} and {@link KitService} instance.
 */
public final class KitSystem implements Listener {
  private final Plugin plugin;
  private final KitManager manager;
  private final KitService service;

  public KitSystem(Plugin plugin) {
    this.plugin = Objects.requireNonNull(plugin);
    this.manager = new KitManager(plugin);
    this.service = new KitService(plugin, manager);
  }

  public KitManager manager() {
    return manager;
  }

  public KitService service() {
    return service;
  }

  /** Enables the kit system by registering its event listeners. */
  public void enable() {
    plugin.getServer().getPluginManager().registerEvents(this, plugin);
  }

  /** Disables the kit system and all active kits. */
  public void disable() {
    manager.disableAll();
    HandlerList.unregisterAll(this);
  }

  /**
   * Called when a game starts.
   *
   * <p>Initializes all players' kits and activates all kits that are in use.
   *
   * @param event the game start event
   */
  @EventHandler
  public void onGameStart(GameStartEvent event) {
    Set<AbstractKit> toActivate = new LinkedHashSet<>();
    for (Player player : event.getPlayers()) {
      final AbstractKit kit = service.get(player);
      if (kit != null) {
        toActivate.add(kit);
        kit.initPlayer(player);
      }
    }

    toActivate.forEach(manager::enableKit);
  }

  /**
   * Called when a game ends.
   *
   * <p>Disables all active kits.
   *
   * @param event the game end event
   */
  @EventHandler
  public void onGameEnd(GameEndEvent event) {
    manager.disableAll();
  }

  /**
   * Ensures that a joining player has a valid kit.
   *
   * <p>If the player has no kit or an invalid/unlocked kit, the default kit ({@link Lumberjack}) is
   * assigned.
   *
   * @param event the join event
   */
  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    final Player player = event.getPlayer();
    if (service.hasValidKit(player)) return;
    assert (manager.get(Lumberjack.id).isPresent());
    service.set(player, manager.get(Lumberjack.id).get());
  }
}
