package io.github.mal32.endergames.world;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import io.github.mal32.endergames.services.PlayerInWorld;
import java.util.concurrent.CompletableFuture;
import org.bukkit.GameRules;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.plugin.PluginMock;

class LobbyWorldTest {
  private ServerMock server;
  private LobbyWorld lobbyWorld;

  private World world;

  @BeforeEach
  void setUp() {
    server = MockBukkit.mock();
    PluginMock plugin = MockBukkit.createMockPlugin();

    world = server.addSimpleWorld("world_enga_lobby");
    final NamespacedKey placedLobbyVersionKey = new NamespacedKey(plugin, "placed_lobby_version");
    world.getPersistentDataContainer().set(placedLobbyVersionKey, PersistentDataType.INTEGER, 1);

    lobbyWorld = new LobbyWorld(plugin, new LobbyPlayerInitService());
  }

  @AfterEach
  void tearDown() {
    MockBukkit.unmock();
  }

  @Test
  void setupWorld() {
    lobbyWorld.setupWorld();

    assertNotEquals(Boolean.TRUE, world.getGameRuleValue(GameRules.ADVANCE_TIME));
    assertNotEquals(Boolean.TRUE, world.getGameRuleValue(GameRules.ADVANCE_WEATHER));
    assertNotEquals(Boolean.TRUE, world.getGameRuleValue(GameRules.LOCATOR_BAR));
    assertEquals(6, world.getGameRuleValue(GameRules.RESPAWN_RADIUS));
  }

  @Test
  void initPlayer() {
    Player player = spy(server.addPlayer());
    doReturn(CompletableFuture.completedFuture(true))
        .when(player)
        .teleportAsync(any(Location.class));

    lobbyWorld.initPlayer(player);

    assertEquals(PlayerInWorld.LOBBY, PlayerInWorld.get(player));
    assertEquals(world, player.getWorld());
  }
}
