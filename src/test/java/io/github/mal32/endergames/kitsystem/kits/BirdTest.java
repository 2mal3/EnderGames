package io.github.mal32.endergames.kitsystem.kits;

import static org.junit.jupiter.api.Assertions.*;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class BirdTest extends KitMockBukkitTest<Bird> {
  @Override
  protected Bird createKit() {
    return new Bird(service, plugin);
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

    assertTrue(player.getInventory().contains(Material.ELYTRA));
    assertTrue(player.getInventory().contains(Material.FIREWORK_ROCKET, 5));
  }
}
