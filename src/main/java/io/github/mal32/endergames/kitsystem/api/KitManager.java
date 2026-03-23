package io.github.mal32.endergames.kitsystem.api;

import io.github.mal32.endergames.kitsystem.registry.KitValidator;
import java.util.*;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;

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
public class KitManager {
  private final Map<String, AbstractKit> kits = new LinkedHashMap<>(32);
  private final Plugin plugin;
  private final Set<AbstractKit> activeKits = new HashSet<>();

  public KitManager(Plugin plugin) {
    this.plugin = plugin;
  }

  /**
   * Registers a kit and validates it using {@link KitValidator}.
   *
   * @param kit the kit to register
   * @throws IllegalArgumentException for invalid advancement requirement
   * @throws IllegalStateException for invalid id
   */
  public void register(AbstractKit kit) {
    KitValidator.validate(kit);
    kits.put(kit.id(), kit);
  }

  /**
   * Retrieves a kit by its ID.
   *
   * @param id the kit ID
   * @return an {@link Optional} containing the kit if found
   */
  public Optional<AbstractKit> get(String id) {
    return Optional.ofNullable(kits.get(id));
  }

  /**
   * Returns all registered kits.
   *
   * @return an unmodifiable collection of kits
   */
  public Collection<AbstractKit> all() {
    return Collections.unmodifiableCollection(kits.values());
  }

  /**
   * Activates a kit for the current game.
   *
   * <p>Registers its event listeners and calls {@link AbstractKit#onEnable()}. If the kit is
   * already active, this method does nothing.
   *
   * @param kit the kit to enable
   */
  public void enableKit(AbstractKit kit) {
    Objects.requireNonNull(kit);
    if (activeKits.contains(kit)) return;
    Bukkit.getPluginManager().registerEvents(kit, plugin);
    kit.onEnable();
    activeKits.add(kit);
  }

  /**
   * Deactivates a kit for the current game.
   *
   * <p>Unregisters its event listeners and calls {@link AbstractKit#onDisable()}. If the kit is not
   * active, this method does nothing.
   *
   * @param kit the kit to disable
   */
  public void disableKit(AbstractKit kit) {
    Objects.requireNonNull(kit);
    if (!activeKits.contains(kit)) return;
    HandlerList.unregisterAll(kit);
    kit.onDisable();
    activeKits.remove(kit);
  }

  /** Disables all currently active kits. */
  public void disableAll() {
    new ArrayList<>(activeKits).forEach(this::disableKit);
  }
}
