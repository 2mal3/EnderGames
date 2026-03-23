package io.github.mal32.endergames;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.plugin.PluginMock;

public abstract class BaseMockBukkitTest {
  protected ServerMock server;
  protected PluginMock plugin;

  @BeforeEach
  protected void setUp() {
    server = MockBukkit.mock();
    plugin = MockBukkit.createMockPlugin("Test Plugin");
    onSetUp();
  }

  protected void onSetUp() {}

  @AfterEach
  void tearDown() {
    onTearDown();
    MockBukkit.unmock();
  }

  protected void onTearDown() {}
}
