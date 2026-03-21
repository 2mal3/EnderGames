package io.github.mal32.endergames.kitsystem.api;

import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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
    if (advancementKey != null) {
      final NamespacedKey key = NamespacedKey.fromString(advancementKey);
      if (key == null || Bukkit.getAdvancement(key) == null)
        throw new IllegalArgumentException("Invalid advancement key: " + advancementKey);
    }
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
