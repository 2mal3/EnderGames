package io.github.mal32.endergames.kitsystem.api;

import io.github.mal32.endergames.kitsystem.UnlockChecker;
import java.util.Objects;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * Handles assignment, lookup and validation of player kits.
 *
 * <p>Kits are stored in the player's {@link org.bukkit.persistence.PersistentDataContainer} using a
 * {@link NamespacedKey}. This allows kits to persist across sessions.
 *
 * <h2>Responsibilities</h2>
 *
 * <ul>
 *   <li>Assigning kits to players via {@link #set(Player, AbstractKit)}
 *   <li>Resolving the current kit via {@link #get(Player)}
 *   <li>Checking whether a player is using a specific kit
 *   <li>Validating whether a player has a usable kit via {@link #hasValidKit(Player)}
 * </ul>
 */
public class KitService {
  private final NamespacedKey kitKey;
  private final KitManager kitManager;

  public KitService(Plugin plugin, KitManager kitManager) {
    this.kitKey = new NamespacedKey(Objects.requireNonNull(plugin), "kit");
    this.kitManager = Objects.requireNonNull(kitManager);
  }

  /**
   * Assigns a kit to a player by storing its ID in the player's persistent data.
   *
   * @param player the player to assign the kit to
   * @param kit the kit to assign
   */
  public void set(Player player, AbstractKit kit) {
    Objects.requireNonNull(player);
    Objects.requireNonNull(kit);
    player.getPersistentDataContainer().set(kitKey, PersistentDataType.STRING, kit.id());
  }

  /**
   * Retrieves the kit currently assigned to the player.
   *
   * @param player the player
   * @return the assigned kit, or null if none is set or the ID is invalid
   */
  public AbstractKit get(Player player) {
    Objects.requireNonNull(player);
    final String id = player.getPersistentDataContainer().get(kitKey, PersistentDataType.STRING);
    return id == null ? null : kitManager.get(id).orElse(null);
  }

  /**
   * Checks whether the player is currently using the given kit.
   *
   * @param player the player
   * @param kit the kit to check
   * @return true if the player is assigned to this kit
   */
  public boolean isUsing(Player player, AbstractKit kit) {
    final AbstractKit current = get(player);
    return current != null && current.id().equals(kit.id());
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
    return (player.getPersistentDataContainer().has(kitKey)
        && get(player) != null
        && UnlockChecker.isUnlocked(player, get(player)));
  }
}
