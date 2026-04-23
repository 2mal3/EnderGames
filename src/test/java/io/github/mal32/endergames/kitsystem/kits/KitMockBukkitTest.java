package io.github.mal32.endergames.kitsystem.kits;

import io.github.mal32.endergames.BaseMockBukkitTest;
import io.github.mal32.endergames.kitsystem.KitManager;
import io.github.mal32.endergames.kitsystem.KitStorage;
import io.github.mal32.endergames.kitsystem.api.AbstractKit;
import io.github.mal32.endergames.kitsystem.api.DummyKit;
import io.github.mal32.endergames.kitsystem.registry.KitValidator;
import io.github.mal32.endergames.services.PlayerInWorld;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public abstract class KitMockBukkitTest<Kit extends AbstractKit> extends BaseMockBukkitTest {
  protected KitManager manager;
  protected Kit kit;
  protected DummyKit dummyKit;

  protected PlayerMock player;

  @BeforeEach
  @Override
  protected void setUp() {
    server = MockBukkit.mock();
    plugin = MockBukkit.createMockPlugin("Test Plugin");
    manager = new KitManager(plugin);

    kit = createKit();
    try (MockedStatic<KitValidator> mocked = Mockito.mockStatic(KitValidator.class)) {
      mocked.when(() -> KitValidator.validate(Mockito.any())).thenAnswer(inv -> null);

      manager.register(kit);
    }

    dummyKit = new DummyKit(Lumberjack.id, plugin);
    manager.register(dummyKit);

    player = server.addPlayer();
    PlayerInWorld.GAME.set(player);
    KitStorage.setKit(player, dummyKit);

    onSetUp();
  }

  protected abstract Kit createKit();

  @Test
  protected abstract void initPlayerGivesCorrectItems();
}
