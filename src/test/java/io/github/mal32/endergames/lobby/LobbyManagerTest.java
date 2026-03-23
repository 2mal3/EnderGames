package io.github.mal32.endergames.lobby;

import static org.junit.jupiter.api.Assertions.*;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.plugin.PluginMock;

class LobbyManagerTest {
  private ServerMock server;
  private PluginMock plugin;
  private LobbyManager manager;

  @BeforeEach
  void setUp() {
    server = MockBukkit.mock();
    plugin = MockBukkit.createMockPlugin();
    manager = new LobbyManager(plugin);
  }

  @AfterEach
  void tearDown() {
    MockBukkit.unmock();
  }

  @Test
  void registerModule() {
    FakeLobbyModule module = new FakeLobbyModule(plugin);

    manager.registerModule(module);
    assertEquals(1, module.registerCount);
  }

  @Test
  void onLobbyEnter() {
    FakeLobbyModule module1 = new FakeLobbyModule(plugin);
    manager.registerModule(module1);

    PlayerMock player = server.addPlayer();
    server.getPluginManager().callEvent(new PlayerEnteredLobbyEvent(player));

    FakeLobbyModule module2 = new FakeLobbyModule(plugin);
    manager.registerModule(module2);

    server.getPluginManager().callEvent(new PlayerEnteredLobbyEvent(player));

    assertEquals(2, module1.joinCount);
    assertEquals(1, module2.joinCount);
  }

  @Test
  void disable() {
    FakeLobbyModule module = new FakeLobbyModule(plugin);

    manager.registerModule(module);
    manager.disable();

    assertEquals(1, module.disableCount);
  }

  static class FakeLobbyModule extends LobbyModule {
    int registerCount = 0;
    int joinCount = 0;
    int disableCount = 0;

    FakeLobbyModule(JavaPlugin plugin) {
      super(plugin);
    }

    @Override
    public void onRegister() {
      registerCount++;
    }

    @Override
    public void onPlayerJoinLobby(Player player) {
      joinCount++;
    }

    @Override
    public void onDisable() {
      disableCount++;
    }
  }
}
