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

  private FindWorldSpawnService spawnService;

  private World world;

  @BeforeEach
  void setup() {
    server = MockBukkit.mock();
    PluginMock plugin = MockBukkit.createMockPlugin();

    world = server.addSimpleWorld("world");

    spawnService = mock(FindWorldSpawnService.class);

    when(spawnService.findNextValidSpawn(any(Location.class)))
        .thenReturn(new Location(world, 1000, 200, 0));

    gameWorld = new GameWorld(plugin, spawnService);
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
    assertEquals(newSpawn.getBlockX(), world.getWorldBorder().getCenter().getBlockX());
    assertEquals(newSpawn.getBlockZ(), world.getWorldBorder().getCenter().getBlockZ());
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

  @Test
  void saveAndLoadSpawn() {
    // First GameWorld creates and saves spawn at x=1000
    // Now create a new spawn location
    Location newSpawn = new Location(world, 2000, 200, 0);
    when(spawnService.findNextValidSpawn(any(Location.class))).thenReturn(newSpawn);

    gameWorld.findNewSpawn();

    assertEquals(2000, gameWorld.getSpawnLocation().getBlockX());

    // Create a new GameWorld instance to test that spawn is persisted
    PluginMock plugin = (PluginMock) server.getPluginManager().getPlugin("MockPlugin");
    GameWorld newGameWorld = new GameWorld(plugin, spawnService);

    assertEquals(2000, newGameWorld.getSpawnLocation().getBlockX());
  }
}
