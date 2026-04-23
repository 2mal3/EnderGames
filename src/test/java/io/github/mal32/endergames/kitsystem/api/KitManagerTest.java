package io.github.mal32.endergames.kitsystem.api;

import static org.junit.jupiter.api.Assertions.*;

import io.github.mal32.endergames.BaseMockBukkitTest;
import io.github.mal32.endergames.kitsystem.KitManager;
import io.github.mal32.endergames.kitsystem.KitStorage;
import io.github.mal32.endergames.kitsystem.kits.Lumberjack;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

class KitManagerTest extends BaseMockBukkitTest {
  private KitManager manager;

  @Override
  protected void onSetUp() {
    manager = new KitManager(plugin);
  }

  @Test
  void testHasValidKit() {
    PlayerMock player = server.addPlayer(); // Automatically assigns Lumberjack
    assertTrue(manager.hasValidKit(player));

    // Clear kit
    player.getPersistentDataContainer().remove(KitStorage.KIT_KEY);
    assertFalse(manager.hasValidKit(player));

    KitStorage.setKit(player, new Lumberjack(plugin));
    assertTrue(manager.hasValidKit(player));
  }

  @Test
  void testOnPlayerJoinAssignsLumberjack() {
    // adding a player triggers PlayerJoinEvent
    PlayerMock player = server.addPlayer();

    // Should have Lumberjack assigned automatically
    assertTrue(manager.hasValidKit(player));
    AbstractKit assigned = KitStorage.getKit(plugin, player);
    assertNotNull(assigned);
    assertEquals(Lumberjack.id, assigned.id());
  }
}
