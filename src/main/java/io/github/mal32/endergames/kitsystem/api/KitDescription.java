package io.github.mal32.endergames.kitsystem.api;

import java.util.Objects;
import org.bukkit.Material;

/**
 * Immutable metadata describing a kit.
 *
 * @param displayName Human‑readable name of the kit
 * @param icon Icon used in menus
 * @param abilities Short description of the kit's abilities
 * @param equipment Description of the starting equipment
 * @param difficulty Difficulty rating for UI purposes
 *     <p>All fields are required during construction.
 */
public record KitDescription(
    String displayName, Material icon, String abilities, String equipment, Difficulty difficulty) {

  /**
   * Creates a new immutable kit description.
   *
   * @param displayName Human‑readable name of the kit
   * @param icon Icon used in menus
   * @param abilities Short description of the kit's abilities
   * @param equipment Description of the starting equipment
   * @param difficulty Difficulty rating for UI purposes
   * @throws NullPointerException if any parameter is null
   */
  public KitDescription {
    Objects.requireNonNull(displayName);
    Objects.requireNonNull(icon);
    Objects.requireNonNull(abilities);
    Objects.requireNonNull(equipment);
    Objects.requireNonNull(difficulty);
  }
}
