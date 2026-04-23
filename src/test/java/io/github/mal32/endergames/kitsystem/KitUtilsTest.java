package io.github.mal32.endergames.kitsystem;

import static org.junit.jupiter.api.Assertions.*;

import io.github.mal32.endergames.BaseMockBukkitTest;
import io.github.mal32.endergames.kitsystem.api.KitUtils;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.junit.jupiter.api.Test;

class KitUtilsTest extends BaseMockBukkitTest {
  @Test
  void enchantItem() {
    ItemStack item = ItemStack.of(Material.WOODEN_SWORD);

    ItemStack enchanted = KitUtils.enchantItem(item, Enchantment.UNBREAKING, 1);

    assertTrue(enchanted.getEnchantments().containsKey(Enchantment.UNBREAKING));
    assertEquals(1, enchanted.getEnchantmentLevel(Enchantment.UNBREAKING));
  }

  @Test
  void colorLeatherArmor() {
    ItemStack leggings = ItemStack.of(Material.LEATHER_LEGGINGS);
    Color color = Color.fromRGB(3064446);

    ItemStack colored = KitUtils.colorLeatherArmor(leggings, color);

    assertInstanceOf(LeatherArmorMeta.class, colored.getItemMeta());
    LeatherArmorMeta meta = (LeatherArmorMeta) colored.getItemMeta();
    assertEquals(color, meta.getColor());
  }
}
