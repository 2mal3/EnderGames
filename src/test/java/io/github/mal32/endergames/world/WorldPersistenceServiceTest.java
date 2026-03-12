package io.github.mal32.endergames.world;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.plugin.PluginMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

class WorldPersistenceServiceTest {
  private ServerMock server;
  private PluginMock plugin;

  @BeforeEach
  void setUp() {
    server = MockBukkit.mock();
    plugin = MockBukkit.createMockPlugin();
  }

  @AfterEach
  void tearDown() {
    MockBukkit.unmock();
  }

  @Test
  void saveAndLoadSpawn() {
    WorldPersistenceService service = new WorldPersistenceService(plugin);

    WorldMock world = new WorldMock();

    service.saveSpawn(world, 42);

    Integer loaded = service.loadSpawn(world);

    assertEquals(42, loaded);
  }
}
