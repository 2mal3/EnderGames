package io.github.mal32.endergames.world;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.mal32.endergames.services.PlayerInWorld;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Difficulty;
import org.bukkit.GameRules;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.plugin.PluginMock;

class GameWorldTest {
  private ServerMock server;
  private GameWorld gameWorld;

  private WorldSpawnService spawnService;
  private WorldPersistenceService persistenceService;
  private WorldBorderService borderService;

  private World world;

  @BeforeEach
  void setup() {
    server = MockBukkit.mock();
    PluginMock plugin = MockBukkit.createMockPlugin();

    world = server.addSimpleWorld("world");

    spawnService = mock(WorldSpawnService.class);
    persistenceService = mock(WorldPersistenceService.class);
    borderService = spy(new WorldBorderService());

    when(persistenceService.loadSpawn(world)).thenReturn(null);
    when(spawnService.findNextValidSpawn(any(Location.class)))
        .thenReturn(new Location(world, 1000, 200, 0));

    gameWorld =
        new GameWorld(
            plugin, new GamePlayerInitService(), spawnService, persistenceService, borderService);
  }

  @AfterEach
  void tearDown() {
    MockBukkit.unmock();
  }

  @Test
  void setupWorld() {
    gameWorld.setupWorld();

    assertEquals(Difficulty.EASY, world.getDifficulty());
    assertNotEquals(Boolean.TRUE, world.getGameRuleValue(GameRules.ADVANCE_WEATHER));
    assertNotEquals(Boolean.TRUE, world.getGameRuleValue(GameRules.LOCATOR_BAR));
    assertNotEquals(Boolean.TRUE, world.getGameRuleValue(GameRules.SPAWN_PHANTOMS));
    assertNotEquals(
        Boolean.TRUE, world.getGameRuleValue(GameRules.ALLOW_ENTERING_NETHER_USING_PORTALS));
  }

  @Test
  void findNewSpawn() {
    Location newSpawn = new Location(world, 2000, 200, 0);
    when(spawnService.findNextValidSpawn(any(Location.class))).thenReturn(newSpawn);

    gameWorld.findNewSpawn();

    assertEquals(2000, gameWorld.getSpawnLocation().getBlockX());
    verify(persistenceService).saveSpawn(eq(world), eq(2000));
    verify(borderService).centerBorder(eq(world), eq(newSpawn));
  }

  @Test
  void initPlayer() {
    Player player = spy(server.addPlayer());
    doReturn(CompletableFuture.completedFuture(true))
        .when(player)
        .teleportAsync(any(Location.class));

    gameWorld.initPlayer(player);

    assertEquals(PlayerInWorld.GAME, PlayerInWorld.get(player));
    assertEquals(world, player.getWorld());
  }
}
