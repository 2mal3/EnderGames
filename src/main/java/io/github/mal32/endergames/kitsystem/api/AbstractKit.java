package io.github.mal32.endergames.kitsystem.api;

import io.github.mal32.endergames.game.phases.PhaseController;
import io.github.mal32.endergames.kitsystem.KitStorage;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Base class for all kits in the EnderGames kit system.
 *
 * <p>A kit defines its metadata, starting equipment, abilities and event listeners. Subclasses
 * should register their behavior using Bukkit's {@link org.bukkit.event.EventHandler} annotations
 * and implement {@link #initPlayer(Player)} to prepare the player at game start.
 *
 * <h2>Lifecycle</h2>
 *
 * <ul>
 *   <li>The constructor is called once during plugin initialization. Do not put per‑game logic
 *       here.
 *   <li>{@link #onEnable()} is called at the start of every game when the kit becomes active.
 *   <li>{@link #onDisable()} is called when a game ends. Use this to stop timers or clean up state.
 *   <li>{@link #initPlayer(Player)} is called for each player using this kit at game start.
 * </ul>
 */
public abstract class AbstractKit implements Listener {
  protected final JavaPlugin plugin;
  private final KitDescription kitDescription;

  public AbstractKit(KitDescription kitDescription, JavaPlugin plugin) {
    this.kitDescription = Objects.requireNonNull(kitDescription);
    this.plugin = Objects.requireNonNull(plugin);
  }

  /**
   * Called when the kit becomes active at the start of a game.
   *
   * <p>Override this to initialize timers, repeating tasks or other per‑game logic. This method is
   * invoked once per game, not once per player.
   */
  public void onEnable() {
    Bukkit.getPluginManager().registerEvents(this, plugin);
  }

  /**
   * Called when the kit is deactivated at the end of a game.
   *
   * <p>Override this to cancel tasks, clear temporary data or unregister logic. This method is
   * invoked once per game, not once per player.
   */
  public void onDisable() {
    HandlerList.unregisterAll(this);
  }

  /**
   * Checks whether the given player is currently allowed to use this kit.
   *
   * <p>A player can use this kit if:
   *
   * <ul>
   *   <li>the player is not null
   *   <li>the player is currently in the game ({@link PhaseController#playerIsInGame(Player)})
   *   <li>the player has this kit assigned via {@link KitService#isUsing(Player, AbstractKit)}
   * </ul>
   *
   * @param player the player to check
   * @return true if the player is actively using this kit
   */
  protected boolean playerCanUseThisKit(Player player) {
    return player != null
        && PhaseController.playerIsInGame(player)
        && KitStorage.isUsing(plugin, player, this);
  }

  /**
   * Initializes the player at the start of a game.
   *
   * <p>This is where starting equipment, potion effects or other initial state should be applied.
   * This method is called once per player at game start.
   *
   * @param player the player to initialize
   */
  public abstract void initPlayer(Player player);

  /**
   * Returns the unique identifier of this kit.
   *
   * <p>By default, this is the display name from {@link KitDescription}. The ID must be unique
   * across all registered kits.
   *
   * @return the kit ID
   */
  public String id() {
    return kitDescription.displayName();
  }

  /**
   * Returns the metadata describing this kit.
   *
   * @return the kit description
   */
  public KitDescription description() {
    return kitDescription;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AbstractKit that = (AbstractKit) o;
    return id().equals(that.id());
  }
}
