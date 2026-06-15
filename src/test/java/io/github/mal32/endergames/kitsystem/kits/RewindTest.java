package io.github.mal32.endergames.kitsystem.kits;

import static org.junit.jupiter.api.Assertions.*;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

class RewindTest extends KitMockBukkitTest<Rewind> {

  @Override
  protected Rewind createKit() {
    return new Rewind(plugin);
  }

  @Test
  void testTickWithoutInitPlayerDoesNotThrowNPE() {
    // In KitMockBukkitTest, the player is added to the game, and the kit is assigned.
    // However, kit.initPlayer(player) is NOT called automatically.
    // The kit's scheduled task runs every 10 ticks (PLAYER_STATE_INTERVAL_TICKS).
    assertDoesNotThrow(
        () -> {
          server.getScheduler().performTicks(10);
        },
        "tick() should not throw NPE even if initPlayer was not called");
  }

  @Test
  void testRewindAbilityRestoresLocation() {
    kit.initPlayer(player);

    // Initial state
    player.teleport(new Location(player.getWorld(), 0, 100, 0));

    assertTrue(player.hasCooldown(Material.CLOCK), "Player shoud have a starting cooldown");
    player.setCooldown(Material.CLOCK, 0);

    // Perform ticks to save this state
    server.getScheduler().performTicks(10);

    // Move player
    player.teleport(new Location(player.getWorld(), 10, 100, 10));

    // Get the clock
    ItemStack clock = null;
    for (ItemStack item : player.getInventory().getContents()) {
      if (item != null && item.getType() == Material.CLOCK) {
        clock = item;
        break;
      }
    }
    assertNotNull(clock, "Clock should be in the inventory");

    player.getInventory().setItemInMainHand(clock);

    // Right click
    PlayerInteractEvent event =
        new PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR, clock, null, null);
    server.getPluginManager().callEvent(event);

    // Perform the animation ticks (at least 3 ticks per state + 1 extra)
    // 1 state was saved, so 1 * 3 ticks for rewindEnd
    server.getScheduler().performTicks(10);
    assertTrue(
        player.hasCooldown(Material.CLOCK), "Clock cooldown should be set after using ability");

    // Assert player location was restored
    assertEquals(0, player.getLocation().getX(), 0.1);
    assertEquals(100, player.getLocation().getY(), 0.1);
    assertEquals(0, player.getLocation().getZ(), 0.1);
  }
}
