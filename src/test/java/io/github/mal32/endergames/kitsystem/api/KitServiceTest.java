package io.github.mal32.endergames.kitsystem.api;

import static org.junit.jupiter.api.Assertions.*;

import io.github.mal32.endergames.BaseMockBukkitTest;
import io.github.mal32.endergames.kitsystem.util.UnlockChecker;
import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

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
    assertTrue(service.hasValidKit(player));
    assertTrue(service.isUsing(player, kit));
  }

  @Test
  void getReturnsNullWhenNoKit() {
    PlayerMock player = server.addPlayer();
    assertNull(service.get(player));
    assertFalse(service.hasValidKit(player));
  }

  @Test
  void hasValidKitReturnsFalseWhenKitNotRegistered() {
    PlayerMock player = server.addPlayer();

    player
        .getPersistentDataContainer()
        .set(
            new NamespacedKey("enga", "kit"),
            org.bukkit.persistence.PersistentDataType.STRING,
            "Dummy2");

    assertFalse(service.hasValidKit(player));
  }

  @Test
  void hasValidKitReturnsFalseWhenKitExistsButNotUnlocked() {
    PlayerMock player = server.addPlayer();

    service.set(player, kit);

    try (MockedStatic<UnlockChecker> mocked = Mockito.mockStatic(UnlockChecker.class)) {
      mocked.when(() -> UnlockChecker.isUnlocked(player, kit)).thenReturn(false);

      assertFalse(service.hasValidKit(player));
    }
  }

  @Test
  void returnsTrueWhenKitExistsAndUnlocked() {
    PlayerMock player = server.addPlayer();

    service.set(player, kit);

    try (MockedStatic<UnlockChecker> mocked = Mockito.mockStatic(UnlockChecker.class)) {
      mocked.when(() -> UnlockChecker.isUnlocked(player, kit)).thenReturn(true);

      assertTrue(service.hasValidKit(player));
    }
  }
}
