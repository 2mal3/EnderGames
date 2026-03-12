package io.github.mal32.endergames.world;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

import io.github.mal32.endergames.services.PlayerInWorld;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

class GamePlayerInitServiceTest {
  private ServerMock server;

  @BeforeEach
  void setUp() {
    server = MockBukkit.mock();
  }

  @AfterEach
  void tearDown() {
    MockBukkit.unmock();
  }

  @Test
  void init() {
    GamePlayerInitService service = new GamePlayerInitService();

    PlayerMock player = spy(server.addPlayer());
    doAnswer(
            invocation -> {
              Location loc = invocation.getArgument(0);
              player.teleport(loc);
              return null;
            })
        .when(player)
        .teleportAsync(any(Location.class));

    player.setGameMode(GameMode.SPECTATOR);
    Location spawn = new Location(player.getWorld(), 100, 64, 200);

    service.init(player, spawn);

    assertTrue(player.hasTeleported());
    assertEquals(PlayerInWorld.GAME, PlayerInWorld.get(player));

    assertEquals(100, player.getLocation().getX());
    assertEquals(69, player.getLocation().getY());
    assertEquals(200, player.getLocation().getZ());
  }
}
