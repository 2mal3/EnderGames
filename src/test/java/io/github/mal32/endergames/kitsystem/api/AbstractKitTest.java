package io.github.mal32.endergames.kitsystem.api;

import static org.junit.jupiter.api.Assertions.*;

import io.github.mal32.endergames.BaseMockBukkitTest;
import io.github.mal32.endergames.kitsystem.KitManager;
import io.github.mal32.endergames.kitsystem.KitStorage;
import io.github.mal32.endergames.services.PlayerInWorld;
import org.bukkit.GameMode;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

class AbstractKitTest extends BaseMockBukkitTest {
  private DummyKit kit;
  private DummyKit kit2;
  private PlayerMock player;

  @Override
  public void onSetUp() {
    KitManager manager = new KitManager(plugin);
    kit = new DummyKit("Dummy", plugin);
    kit2 = new DummyKit("Dummy2", plugin);

    player = server.addPlayer();
    PlayerInWorld.GAME.set(player);
    player.setGameMode(GameMode.SURVIVAL);
    KitStorage.setKit(player, kit);

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

    KitStorage.setKit(player, kit2);
    assertFalse(
        kit.playerCanUseThisKit(player), "Player should no longer be able to use this kit.");
    assertTrue(kit2.playerCanUseThisKit(player), "Player should now be able to use this kit.");
  }
}
