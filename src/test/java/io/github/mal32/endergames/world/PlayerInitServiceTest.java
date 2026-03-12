package io.github.mal32.endergames.world;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Objects;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

class PlayerInitServiceTest {
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
  void resetBase() {
    PlayerMock player = server.addPlayer();

    player.setHealth(5);
    player.setFoodLevel(5);
    player.setFireTicks(100);
    player.setVelocity(new Vector(1, 2, 3));
    player.setFallDistance(50);

    PlayerInitService service =
        new PlayerInitService() {
          @Override
          public void init(Player p, org.bukkit.Location spawn) {}
        };

    service.resetBase(player);

    assertEquals(20, player.getFoodLevel());
    assertEquals(
        Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).getValue(),
        player.getHealth());
    assertEquals(0, player.getFireTicks());
    assertEquals(0, player.getFallDistance());
    assertEquals(new Vector(0, 0, 0), player.getVelocity());
    assertTrue(player.getActivePotionEffects().isEmpty());
  }
}
