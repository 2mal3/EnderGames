package io.github.mal32.endergames.kitsystem.api;

import static org.junit.jupiter.api.Assertions.*;

import io.github.mal32.endergames.BaseMockBukkitTest;
import io.github.mal32.endergames.game.phases.GameEndEvent;
import io.github.mal32.endergames.game.phases.GameStartEvent;
import io.github.mal32.endergames.kitsystem.kits.Lumberjack;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockito.Mockito;

class KitSystemTest extends BaseMockBukkitTest {
  private KitSystem system;
  private DummyKit kit;
  private DummyKit kit2;

  @Override
  protected void onSetUp() {
    system = new KitSystem(plugin);
    kit = new DummyKit(Lumberjack.id, system.service(), plugin);
    kit2 = new DummyKit("Dummy2", system.service(), plugin);
    system.manager().register(kit);
    system.manager().register(kit2);

    system.enable();
  }

  @Test
  void onGameStart() {
    PlayerMock p1 = server.addPlayer();
    PlayerMock p2 = server.addPlayer();

    system.service().set(p1, kit);
    system.service().set(p2, kit);

    GameStartEvent event = new GameStartEvent(List.of(p1, p2));
    server.getPluginManager().callEvent(event);

    assertTrue(kit.enabled);
    assertFalse(kit2.enabled);
  }

  @Test
  void onGameEnd() {
    system.manager().enableKit(kit);

    GameEndEvent event = new GameEndEvent();
    server.getPluginManager().callEvent(event);

    assertTrue(kit.disabled);
  }

  @Test
  void onPlayerJoin() {
    AbstractKit lumberjackKit = Mockito.mock(AbstractKit.class);
    Mockito.when(lumberjackKit.id()).thenReturn(Lumberjack.id);
    system.manager().register(lumberjackKit);

    PlayerMock player = server.addPlayer();
    assertNotNull(system.service().get(player));
    assertEquals(Lumberjack.id, system.service().get(player).id());

    system.service().set(player, kit2);
    assertNotNull(system.service().get(player));
    assertNotEquals(Lumberjack.id, system.service().get(player).id());

    player.reconnect();
    assertNotNull(system.service().get(player));
    assertNotEquals(Lumberjack.id, system.service().get(player).id());
  }
}
