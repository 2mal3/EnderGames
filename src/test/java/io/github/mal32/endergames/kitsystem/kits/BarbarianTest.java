package io.github.mal32.endergames.kitsystem.kits;

import static org.junit.jupiter.api.Assertions.*;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.junit.jupiter.api.Test;

class BarbarianTest extends KitMockBukkitTest<Barbarian> {
  @Override
  protected Barbarian createKit() {
    return new Barbarian(plugin);
  }

  @Override
  @Test
  protected void initPlayerGivesCorrectItems() {
    player.getInventory().clear();
    kit.initPlayer(player);

    assertNotNull(player.getInventory().getHelmet());
    assertEquals(Material.LEATHER_HELMET, player.getInventory().getHelmet().getType());
    assertTrue(
        player.getInventory().getHelmet().getEnchantments().containsKey(Enchantment.UNBREAKING));

    assertNotNull(player.getInventory().getChestplate());
    assertEquals(Material.LEATHER_CHESTPLATE, player.getInventory().getChestplate().getType());
    assertNotNull(player.getInventory().getLeggings());
    assertEquals(Material.LEATHER_LEGGINGS, player.getInventory().getLeggings().getType());
    assertNotNull(player.getInventory().getBoots());
    assertEquals(Material.LEATHER_BOOTS, player.getInventory().getBoots().getType());

    assertTrue(player.getInventory().contains(Material.WOODEN_SWORD));
  }
}
