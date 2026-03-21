package io.github.mal32.endergames.kitsystem.api;

import static org.junit.jupiter.api.Assertions.*;

import io.github.mal32.endergames.BaseMockBukkitTest;
import io.github.mal32.endergames.services.PlayerInWorld;
import org.bukkit.GameMode;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

class AbstractKitTest extends BaseMockBukkitTest {
  private KitService service;
  private DummyKit kit;
  private DummyKit kit2;
  private PlayerMock player;

  @Override
  public void onSetUp() {
    KitManager manager = new KitManager(plugin);
    service = new KitService(plugin, manager);
    kit = new DummyKit("Dummy", service, plugin);
    kit2 = new DummyKit("Dummy2", service, plugin);

    player = server.addPlayer();
    PlayerInWorld.GAME.set(player);
    player.setGameMode(GameMode.SURVIVAL);
    service.set(player, kit);

    manager.register(kit);
    manager.register(kit2);
  }

  @Test
  void returnsFalseWhenPlayerNull() {
    assertFalse(kit.playerCanUseThisKit(null));
  }

  @Test
  void returnsFalseWhenNotInGame() {
    PlayerInWorld.LOBBY.set(player);
    assertFalse(kit.playerCanUseThisKit(player));
  }

  @Test
  void returnsFalseWhenNotUsingKit() {
    assertFalse(kit2.playerCanUseThisKit(player));
  }

  @Test
  void returnsTrueWhenAllConditionsMet() {
    assertTrue(kit.playerCanUseThisKit(player));

    service.set(player, kit2);
    assertFalse(
        kit.playerCanUseThisKit(player), "Player should no longer be able to use this kit.");
    assertTrue(kit2.playerCanUseThisKit(player), "Player should now be able to use this kit.");
  }
}
