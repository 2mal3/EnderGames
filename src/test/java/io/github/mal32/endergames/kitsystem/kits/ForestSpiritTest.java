package io.github.mal32.endergames.kitsystem.kits;

import static org.junit.jupiter.api.Assertions.*;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Zombie;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.world.WorldMock;

class ForestSpiritTest extends KitMockBukkitTest<ForestSpirit> {
  @Override
  protected ForestSpirit createKit() {
    return new ForestSpirit(plugin);
  }

  @Test
  void testGrowthAbilityPlacesTreeOnEntity() {
    kit.initPlayer(player);

    // Bypass the 60-second start time restriction using reflection
    player.setCooldown(Material.GREEN_DYE, 0);

    // Create a target entity and position them close to the caster
    WorldMock customWorld =
        new WorldMock() {
          @Override
          public boolean generateTree(
              Location loc, java.util.Random random, org.bukkit.TreeType type) {
            return false;
          }
        };
    server.addWorld(customWorld);
    player.teleport(customWorld.getSpawnLocation());

    LivingEntity targetEntity =
        customWorld.spawn(player.getLocation().clone().add(2, 0, 0), Zombie.class);
    Location targetLoc = targetEntity.getLocation();

    // Find the Growth item in the caster's inventory
    ItemStack growthItem = null;
    for (ItemStack item : player.getInventory().getContents()) {
      if (item != null && item.getType() == Material.GREEN_DYE) {
        growthItem = item;
        break;
      }
    }
    assertNotNull(growthItem, "Growth item (GREEN_DYE) should be in the inventory");

    player.getInventory().setItemInMainHand(growthItem);

    PlayerInteractEvent event =
        new PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR, growthItem, null, null);
    server.getPluginManager().callEvent(event);

    assertTrue(player.getCooldown(growthItem) > 0, "Item cooldown should be set");

    assertTrue(
        Tag.LOGS.isTagged(targetLoc.getBlock().getType()),
        "Growth ability should place logs on the targeted entity.");
  }
}
