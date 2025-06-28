package io.github.mal32.endergames.kits;

import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

public record KitDescriptionItem(
    Material item,
    String name,
    String abilities,
    @Nullable String equipment,
    Difficulty difficulty) {

  public KitDescriptionItem {
    if (item == null || name == null || abilities == null || difficulty == null) {
      throw new IllegalArgumentException("All fields must be non-null");
    }
  }
}
