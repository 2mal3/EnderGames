package io.github.mal32.endergames.kitsystem.api;

import static org.junit.jupiter.api.Assertions.*;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class KitDescriptionTest {
  @Test
  void constructorRejectsNulls() {
    assertThrows(
        NullPointerException.class,
        () -> new KitDescription(null, Material.STONE, "", "", Difficulty.EASY));
    assertThrows(
        NullPointerException.class,
        () -> new KitDescription("Name", null, "", "", Difficulty.EASY));
    assertThrows(
        NullPointerException.class,
        () -> new KitDescription("Name", Material.STONE, null, "", Difficulty.EASY));
    assertThrows(
        NullPointerException.class,
        () -> new KitDescription("Name", Material.STONE, "", null, Difficulty.EASY));
    assertThrows(
        NullPointerException.class, () -> new KitDescription("Name", Material.STONE, "", "", null));
  }

  @Test
  void recordStoresValues() {
    KitDescription desc =
        new KitDescription(
            "Barbarian", Material.LEATHER_CHESTPLATE, "Abilities", "Equipment", Difficulty.HARD);

    assertEquals("Barbarian", desc.displayName());
    assertEquals(Material.LEATHER_CHESTPLATE, desc.icon());
    assertEquals("Abilities", desc.abilities());
    assertEquals("Equipment", desc.equipment());
    assertEquals(Difficulty.HARD, desc.difficulty());
  }
}
