package io.github.mal32.endergames;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.plugin.PluginMock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import io.github.mal32.endergames.kitsystem.KitRegisty;

public abstract class BaseMockBukkitTest {
  protected ServerMock server;
  protected PluginMock plugin;
  protected MockedStatic<KitRegisty> mockedKitRegistry;

  @BeforeEach
  protected void setUp() {
    server = MockBukkit.mock();
    plugin = MockBukkit.createMockPlugin("Test Plugin");
    
    mockedKitRegistry = Mockito.mockStatic(KitRegisty.class, Mockito.CALLS_REAL_METHODS);
    mockedKitRegistry.when(() -> KitRegisty.validate(Mockito.any())).thenAnswer(inv -> null);

    onSetUp();
  }

  protected void onSetUp() {}

  @AfterEach
  protected void tearDown() {
    onTearDown();
    if (mockedKitRegistry != null) {
      mockedKitRegistry.close();
      mockedKitRegistry = null;
    }
    MockBukkit.unmock();
  }

  protected void onTearDown() {}
}

