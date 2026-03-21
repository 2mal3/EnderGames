package io.github.mal32.endergames.kitsystem.api;

import static org.junit.jupiter.api.Assertions.*;

import io.github.mal32.endergames.BaseMockBukkitTest;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

class KitServiceTest extends BaseMockBukkitTest {
  private KitService service;
  private DummyKit kit;

  @Override
  protected void onSetUp() {
    KitManager manager = new KitManager(plugin);
    service = new KitService(plugin, manager);
    kit = new DummyKit("Dummy", service, plugin);
    manager.register(kit);
  }

  @Test
  void setAndGet() {
    PlayerMock player = server.addPlayer();
    service.set(player, kit);

    AbstractKit stored = service.get(player);
    assertNotNull(stored);
    assertEquals("Dummy", stored.id());
    assertTrue(service.hasKit(player));
    assertTrue(service.isUsing(player, kit));
  }

  @Test
  void getReturnsNullWhenNoKit() {
    PlayerMock player = server.addPlayer();
    assertNull(service.get(player));
    assertFalse(service.hasKit(player));
  }
}
