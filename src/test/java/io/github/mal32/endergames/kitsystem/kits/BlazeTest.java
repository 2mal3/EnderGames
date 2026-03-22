package io.github.mal32.endergames.kitsystem.kits;

import static org.junit.jupiter.api.Assertions.*;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.simulate.entity.PlayerSimulation;

class BlazeTest extends KitMockBukkitTest<Blaze> {
  @Override
  protected Blaze createKit() {
    return new Blaze(service, plugin);
  }

  @Override
  protected void onSetUp() {
    manager.enableKit(kit);
  }

  @Override
  @Test
  protected void initPlayerGivesCorrectItems() {
    player.getInventory().clear();
    kit.initPlayer(player);

    assertTrue(player.getInventory().contains(Material.GOLDEN_SWORD));
    assertTrue(player.getInventory().contains(Material.BLAZE_POWDER));
    int slot = player.getInventory().first(Material.BLAZE_POWDER);
    ItemStack item = player.getInventory().getItem(slot);
    assertNotNull(item);
    assertTrue(item.containsEnchantment(Enchantment.VANISHING_CURSE));
    assertTrue(player.hasPotionEffect(PotionEffectType.FIRE_RESISTANCE));
  }

  @Test
  void testWeaknessAppliedWhenEnteringWater() {
    Location waterLoc = player.getLocation().clone().add(0, 0, 1);
    waterLoc.getBlock().setType(Material.WATER);

    PlayerSimulation sim = new PlayerSimulation(player);
    player.setInWater(true);
    sim.simulatePlayerMove(waterLoc);

    assertTrue(player.hasPotionEffect(PotionEffectType.WEAKNESS));
  }

  @Test
  void testWeaknessRemovedWhenLeavingWater() {
    player.addPotionEffect(
        PotionEffectType.WEAKNESS.createEffect(PotionEffect.INFINITE_DURATION, 0));

    Location dryLoc = player.getLocation().clone().add(0, 0, 1);

    PlayerSimulation sim = new PlayerSimulation(player);
    sim.simulatePlayerMove(dryLoc);
    assertFalse(player.isInWater());

    assertFalse(player.hasPotionEffect(PotionEffectType.WEAKNESS));
  }
}
