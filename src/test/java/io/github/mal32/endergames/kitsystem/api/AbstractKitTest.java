package io.github.mal32.endergames.kitsystem.api;

import static org.junit.jupiter.api.Assertions.*;

import io.github.mal32.endergames.BaseMockBukkitTest;
import io.github.mal32.endergames.kitsystem.KitStorage;
import io.github.mal32.endergames.kitsystem.kits.Cat;
import io.github.mal32.endergames.kitsystem.kits.Lumberjack;
import io.github.mal32.endergames.services.PlayerInWorld;
import org.bukkit.GameMode;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

class AbstractKitTest extends BaseMockBukkitTest {
  private Lumberjack kit;
  private Cat kit2;
  private PlayerMock player;

  @Override
  public void onSetUp() {
    kit = new Lumberjack(plugin);
    kit2 = new Cat(plugin);

    player = server.addPlayer();
    PlayerInWorld.GAME.set(player);
    player.setGameMode(GameMode.SURVIVAL);
    KitStorage.setKit(player, kit);
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
