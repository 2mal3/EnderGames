package io.github.mal32.endergames.world;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

import io.github.mal32.endergames.services.PlayerInWorld;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.potion.PotionEffectType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.matcher.plugin.PluginManagerFiredEventClassMatcher;

class LobbyPlayerInitServiceTest {
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
    LobbyPlayerInitService service = new LobbyPlayerInitService();

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

    assertThat(
        server.getPluginManager(),
        PluginManagerFiredEventClassMatcher.hasFiredEventInstance(PlayerEnteredLobbyEvent.class));

    assertTrue(player.hasTeleported());
    assertTrue(PlayerInWorld.LOBBY.is(player));

    assertEquals(74, player.getLocation().getY());

    assertEquals(GameMode.ADVENTURE, player.getGameMode());
    assertTrue(player.hasPotionEffect(PotionEffectType.SATURATION));
    assertTrue(player.hasPotionEffect(PotionEffectType.RESISTANCE));
  }
}
