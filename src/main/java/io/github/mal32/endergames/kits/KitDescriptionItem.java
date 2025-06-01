package io.github.mal32.endergames.kits;

import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

public class KitDescriptionItem {
  public final Material item;
  public final String name;

  public final String abilities;
  @Nullable public final String equipment;
  public final Difficulty difficulty;

  public KitDescriptionItem(
      Material item,
      String name,
      String abilities,
      @Nullable String equipment,
      Difficulty difficulty) {
    this.item = item;
    this.name = name;
    this.abilities = abilities;
    this.equipment = equipment;
    this.difficulty = difficulty;
  }
}
