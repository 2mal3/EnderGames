package io.github.mal32.endergames.kitsystem.util;

import org.bukkit.Color;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.jetbrains.annotations.NotNull;

/**
 * Utility methods for modifying and preparing kit-related items.
 *
 * <p>This class currently provides convenience helpers for applying enchantments coloring leather
 * armor pieces. All methods modify the provided {@link ItemStack} directly and return the same
 * instance for chaining.
 */
public final class KitUtils {
  /**
   * Adds an enchantment to the given item.
   *
   * <p>The enchantment is applied unsafely, meaning it may exceed normal Minecraft limits if
   * desired.
   *
   * @param item the item to enchant
   * @param enchantment the enchantment to apply
   * @param level the enchantment level
   * @return the same {@link ItemStack} instance with the enchantment applied
   */
  public static ItemStack enchantItem(
      @NotNull ItemStack item, @NotNull Enchantment enchantment, int level) {
    item.addUnsafeEnchantment(enchantment, level);
    return item;
  }

  /**
   * Applies a color to a leather armor piece.
   *
   * <p>The item must be a leather armor type (helmet, chestplate, leggings, boots). The method
   * casts the {@link ItemMeta} to {@link LeatherArmorMeta} and applies the given color.
   *
   * @param item the leather armor item to color
   * @param color the color to apply
   * @return the same {@link ItemStack} instance with the color applied
   * @throws ClassCastException if the item is not leather armor
   */
  public static ItemStack colorLeatherArmor(@NotNull ItemStack item, @NotNull Color color) {
    item.editMeta(meta -> ((LeatherArmorMeta) meta).setColor(color));
    return item;
  }
}
