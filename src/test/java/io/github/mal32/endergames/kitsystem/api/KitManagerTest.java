package io.github.mal32.endergames.kitsystem.api;

import static org.junit.jupiter.api.Assertions.*;

import io.github.mal32.endergames.BaseMockBukkitTest;
import io.github.mal32.endergames.kitsystem.KitManager;
import org.junit.jupiter.api.Test;

class KitManagerTest extends BaseMockBukkitTest {
  private KitManager manager;
  private DummyKit kit;

  @Override
  protected void onSetUp() {
    manager = new KitManager(plugin);
    kit = new DummyKit("Dummy", plugin);
    manager.register(kit);
  }

  @Test
  void registerGetAll() {
    assertTrue(manager.get("Dummy").isPresent());
    assertEquals(kit, manager.get("Dummy").get());
    assertEquals(1, manager.all().size());
    assertTrue(manager.get("42").isEmpty());
  }

  @Test
  void disableKits() {
    DummyKit kit2 = new DummyKit("Dummy2", service, plugin);
    manager.register(kit2);
    DummyKit kit3 = new DummyKit("Dummy3", service, plugin);
    manager.register(kit3);

    manager.enableKit(kit);
    manager.enableKit(kit2);
    manager.disableAll();

    assertTrue(kit.disabled);
    assertTrue(kit2.disabled);
    assertFalse(kit3.disabled);
  }
}
