package io.github.mal32.endergames.kits;

import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

public record KitDescription(
    Material item,
    String name,
    String abilities,
    @Nullable String equipment,
    Difficulty difficulty) {

  public KitDescription {
    if (item == null || name == null || abilities == null || difficulty == null) {
      throw new IllegalArgumentException("All fields must be non-null");
    }
  }
}
