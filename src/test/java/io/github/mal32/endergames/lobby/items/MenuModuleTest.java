package io.github.mal32.endergames.lobby.items;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.mal32.endergames.game.phases.GameEndEvent;
import io.github.mal32.endergames.game.phases.GameStartAbortEvent;
import io.github.mal32.endergames.game.phases.GameStartingEvent;
import io.github.mal32.endergames.kitsystem.api.KitSystem;
import io.github.mal32.endergames.services.PlayerInWorld;
import io.github.mal32.endergames.services.PlayerState;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.plugin.PluginMock;

class MenuModuleTest {
  ServerMock server;
  PluginMock plugin;

  MenuModule module;
  FakeMenuItem itemA;
  FakeMenuItem itemB;

  @BeforeEach
  void setUp() {
    server = MockBukkit.mock();
    plugin = MockBukkit.createMockPlugin();

    itemA = new FakeMenuItem(plugin, (byte) 0, "a");
    itemB = new FakeMenuItem(plugin, (byte) 1, "b");

    module =
        new MenuModule(plugin, new KitSystem(plugin)) {
          @Override
          void registerDefaultItems() {
            registerItem(itemA);
            registerItem(itemB);
          }
        };

    module.onRegister();
  }

  @AfterEach
  void tearDown() {
    MockBukkit.unmock();
  }

  @Test
  void onPlayerJoinLobby() {
    PlayerMock player = server.addPlayer();

    module.onPlayerJoinLobby(player);

    assertEquals(1, itemA.initCount);
    assertEquals(1, itemB.initCount);
  }

  @Test
  void onPlayerJoinLobby_withOpPlayer() {
    PlayerMock opPlayer = server.addPlayer();
    opPlayer.setOp(true);

    module.onPlayerJoinLobby(opPlayer);

    assertEquals(1, itemA.initCount);
    assertEquals(1, itemB.initCount);
  }

  @Test
  void onPlayerJoinLobby_withNonOpPlayer() {
    PlayerMock nonOpPlayer = server.addPlayer();
    nonOpPlayer.setOp(false);

    module.onPlayerJoinLobby(nonOpPlayer);

    assertEquals(1, itemA.initCount);
    assertEquals(1, itemB.initCount);
  }

  @Test
  void onGameStarting() {
    PlayerMock player1 = server.addPlayer();
    PlayerState.IN_LOBBY.set(player1);
    PlayerMock player2 = server.addPlayer();
    PlayerState.PLAYING.set(player2);

    server.getPluginManager().callEvent(new GameStartingEvent(List.of(player2)));

    assertEquals(1, itemA.gameStartCount);

    server.getPluginManager().callEvent(new GameStartingEvent(List.of(player2)));

    assertEquals(2, itemA.gameStartCount);
  }

  @Test
  void onGameEnd() {
    PlayerMock player1 = server.addPlayer();
    PlayerInWorld.LOBBY.set(player1);
    PlayerMock player2 = server.addPlayer();
    PlayerInWorld.GAME.set(player2);

    server.getPluginManager().callEvent(new GameEndEvent());

    assertEquals(1, itemA.gameEndCount);
    assertEquals(1, itemB.gameEndCount);
  }

  @Test
  void onGameStartAbort() {
    PlayerMock player = server.addPlayer();
    PlayerInWorld.LOBBY.set(player);

    server.getPluginManager().callEvent(new GameStartAbortEvent());

    assertEquals(11, itemA.abortCount);
    assertEquals(11, itemB.abortCount);
  }

  @Disabled("TODO")
  @Test
  void onPlayerInteract() {
    // TODO
  }

  static class FakeMenuItem extends AbstractMenuItem {

    int initCount = 0;
    int interactCount = 0;
    int gameStartCount = 0;
    int gameEndCount = 0;
    int abortCount = 0;

    protected FakeMenuItem(JavaPlugin plugin, byte slot, String key) {
      super(plugin, slot, key, Material.BORDURE_INDENTED_BANNER_PATTERN, Component.text(42));
    }

    @Override
    public void initPlayer(Player player) {
      initCount++;
    }

    @Override
    public void playerInteract(PlayerInteractEvent event) {
      interactCount++;
    }

    @Override
    public void onGameStart(Player player) {
      gameStartCount++;
    }

    @Override
    public void onGameEnd(Player player) {
      gameEndCount++;
    }

    @Override
    public void onGameStartAbort() {
      abortCount += 10;
    }

    @Override
    public void onGameStartAbort(Player player) {
      abortCount++;
    }
  }
}
