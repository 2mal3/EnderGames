package io.github.mal32.endergames.world;

import static org.junit.jupiter.api.Assertions.*;

import io.github.mal32.endergames.game.FindWorldSpawnService;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.world.WorldMock;

class FindWorldSpawnServiceTest {

  @BeforeEach
  void setUp() {
    MockBukkit.mock();
  }

  @AfterEach
  void tearDown() {
    MockBukkit.unmock();
  }

  @Test
  void isInvalidBiome() {
    FindWorldSpawnService service = new FindWorldSpawnService();

    assertTrue(service.isInvalidBiome(Biome.OCEAN));
    assertTrue(service.isInvalidBiome(Biome.JUNGLE));
    assertFalse(service.isInvalidBiome(Biome.PLAINS));
  }

  @Test
  void findNextValidSpawn() {
    FindWorldSpawnService service = new FindWorldSpawnService();

    WorldMock world = new WorldMock(Material.STONE, 256, 0);
    world.setBiome(0, 200, 0, Biome.WARM_OCEAN);
    world.setBiome(1000, 200, 0, Biome.DEEP_FROZEN_OCEAN);
    world.setBiome(2000, 200, 0, Biome.DESERT);

    Location start = new Location(world, 0, 200, 0);
    Location result = service.findNextValidSpawn(start);

    assertEquals(2000, result.getBlockX());
  }
}
