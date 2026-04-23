package io.github.mal32.endergames.kitsystem.kits;

import io.github.mal32.endergames.BaseMockBukkitTest;
import io.github.mal32.endergames.kitsystem.KitManager;
import io.github.mal32.endergames.kitsystem.KitRegisty;
import io.github.mal32.endergames.kitsystem.KitStorage;
import io.github.mal32.endergames.kitsystem.api.AbstractKit;
import io.github.mal32.endergames.services.PlayerInWorld;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockito.Mockito;

public abstract class KitMockBukkitTest<Kit extends AbstractKit> extends BaseMockBukkitTest {
  protected KitManager manager;
  protected Kit kit;
  protected Lumberjack dummyKit;

  protected PlayerMock player;

  @BeforeEach
  @Override
  protected void setUp() {
    server = MockBukkit.mock();
    plugin = MockBukkit.createMockPlugin("Test Plugin");

    mockedKitRegistry = Mockito.mockStatic(KitRegisty.class, Mockito.CALLS_REAL_METHODS);
    mockedKitRegistry.when(() -> KitRegisty.validate(Mockito.any())).thenAnswer(inv -> null);

    manager = new KitManager(plugin);

    kit = createKit();
    kit.onEnable();

    dummyKit = new Lumberjack(plugin);

    player = server.addPlayer();
    PlayerInWorld.GAME.set(player);
    KitStorage.setKit(player, kit);

    onSetUp();
  }

  protected abstract Kit createKit();

  @Test
  protected abstract void initPlayerGivesCorrectItems();
}
