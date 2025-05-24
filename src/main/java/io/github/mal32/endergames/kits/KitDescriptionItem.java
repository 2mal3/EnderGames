package io.github.mal32.endergames.kits;

import org.bukkit.Material;

public class KitDescriptionItem {
  public final Material item;
  public final String name;

  public final String abilities;
  public final String equipment;

  public KitDescriptionItem(Material item, String name, String abilities, String equipment) {
    this.item = item;
    this.name = name;
    this.abilities = abilities;
    this.equipment = equipment;
  }
}
