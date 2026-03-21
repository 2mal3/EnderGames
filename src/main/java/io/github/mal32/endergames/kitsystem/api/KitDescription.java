package io.github.mal32.endergames.kitsystem.api;

import java.util.Objects;
import org.bukkit.Material;

public record KitDescription(
    String displayName, Material icon, String abilities, String equipment, Difficulty difficulty) {

  public KitDescription {
    Objects.requireNonNull(displayName);
    Objects.requireNonNull(icon);
    Objects.requireNonNull(abilities);
    Objects.requireNonNull(equipment);
    Objects.requireNonNull(difficulty);
  }
}
