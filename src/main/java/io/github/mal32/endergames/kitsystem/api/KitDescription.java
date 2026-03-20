package io.github.mal32.endergames.kitsystem.api;

import java.util.Objects;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

public record KitDescription(
    String displayName,
    Material icon,
    String abilities,
    String equipment,
    Difficulty difficulty,
    @Nullable String advancementKey) {

  public KitDescription {
    Objects.requireNonNull(displayName);
    Objects.requireNonNull(icon);
    Objects.requireNonNull(abilities);
    Objects.requireNonNull(equipment);
    Objects.requireNonNull(difficulty);
  }

  public KitDescription(
      String displayName,
      Material icon,
      String abilities,
      String equipment,
      Difficulty difficulty) {
    this(displayName, icon, abilities, equipment, difficulty, null);
  }
}
