package io.github.mal32.endergames.kitsystem;

import io.github.mal32.endergames.game.phases.GameEndEvent;
import io.github.mal32.endergames.game.phases.GameStartEvent;
import io.github.mal32.endergames.kitsystem.api.AbstractKit;
import io.github.mal32.endergames.kitsystem.kits.Lumberjack;
import java.util.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Manages registration, activation and deactivation of all kits.
 *
 * <p>Kits must be registered before they can be used. The manager ensures:
 *
 * <ul>
 *   <li>Kit validation via {@link KitValidator}
 *   <li>Event listeners are registered when a kit becomes active
 *   <li>Event listeners are unregistered when a kit is disabled
 * </ul>
 *
 * <h2>Lifecycle</h2>
 *
 * <ul>
 *   <li>{@link #enableKit(AbstractKit)} is called at game start for all used kits
 *   <li>{@link #disableKit(AbstractKit)} is called at game end
 *   <li>{@link #disableAll()} disables all currently active kits
 * </ul>
 */
public class KitManager implements Listener {
  private final Plugin plugin;
  private final Map<String, AbstractKit> kits;

  public KitManager(Plugin plugin) {
    this.plugin = plugin;
    this.kits = KitRegisty.getKits((JavaPlugin) plugin);

    this.enable();
  }

  public void enable() {
    Bukkit.getPluginManager().registerEvents(this, plugin);
  }

  private void enableKits() {
    for (AbstractKit kit : kits.values()) {
      kit.onEnable();
    }
  }

  public void disable() {
    disableKits();
    HandlerList.unregisterAll(this);
  }

  private void disableKits() {
    for (AbstractKit kit : kits.values()) {
      kit.onDisable();
    }
  }

  @EventHandler
  public void onGameStart(GameStartEvent event) {
    Set<AbstractKit> toActivate = new LinkedHashSet<>();
    for (Player player : event.getPlayers()) {
      final AbstractKit kit = KitStorage.getKit((JavaPlugin) plugin, player);
      if (kit != null) {
        toActivate.add(kit);
        kit.initPlayer(player);
      }
    }

    enableKits();
  }

  @EventHandler
  public void onGameEnd(GameEndEvent event) {
    disableKits();
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
    if (hasValidKit(player)) return;
    KitStorage.setKit(player, KitRegisty.getKits((JavaPlugin) plugin).get(Lumberjack.id));
  }

  /**
   * Checks whether the player has a valid and unlocked kit assigned.
   *
   * <p>A kit is considered valid if:
   *
   * <ul>
   *   <li>the persistent data contains a kit ID
   *   <li>the ID resolves to a registered kit
   *   <li>the kit is unlocked according to {@link UnlockChecker}
   * </ul>
   *
   * @param player the player to check
   * @return true if the player has a valid, unlocked kit
   */
  public boolean hasValidKit(Player player) {
    AbstractKit kit = KitStorage.getKit((JavaPlugin) plugin, player);
    if (kit == null) return false;
    return (UnlockChecker.isUnlocked(player, kit));
  }
}
