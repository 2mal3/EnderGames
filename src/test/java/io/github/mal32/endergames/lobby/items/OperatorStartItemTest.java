package io.github.mal32.endergames.lobby.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.plugin.PluginMock;

class OperatorStartItemTest {
  ServerMock server;
  PluginMock plugin;
  OperatorStartItem item;

  @BeforeEach
  void setUp() {
    server = MockBukkit.mock();
    plugin = MockBukkit.createMockPlugin();
    item = new OperatorStartItem(plugin);
  }

  @AfterEach
  void tearDown() {
    MockBukkit.unmock();
  }

  @Test
  void initPlayer_withOpPlayer_shouldGiveNetherStar() {
    PlayerMock opPlayer = server.addPlayer();
    opPlayer.setOp(true);

    item.initPlayer(opPlayer);

    ItemStack itemInSlot = opPlayer.getInventory().getItem(8);
    assertNotNull(itemInSlot, "OP player should receive an item in slot 8");
    assertEquals(
        Material.NETHER_STAR,
        itemInSlot.getType(),
        "OP player should receive a nether star on lobby join");
  }

  @Test
  void initPlayer_withNonOpPlayer_shouldNotGiveItem() {
    PlayerMock nonOpPlayer = server.addPlayer();
    nonOpPlayer.setOp(false);

    item.initPlayer(nonOpPlayer);

    ItemStack itemInSlot = nonOpPlayer.getInventory().getItem(8);
    assertNull(itemInSlot, "Non-OP player should not receive the nether star item");
  }

  @Test
  void onGameEnd_withOpPlayer_shouldGiveNetherStar() {
    PlayerMock opPlayer = server.addPlayer();
    opPlayer.setOp(true);

    item.onGameEnd(opPlayer);

    ItemStack itemInSlot = opPlayer.getInventory().getItem(8);
    assertNotNull(itemInSlot, "OP player should receive an item in slot 8");
    assertEquals(
        Material.NETHER_STAR,
        itemInSlot.getType(),
        "OP player should receive a nether star after game end");
  }

  @Test
  void onGameEnd_withNonOpPlayer_shouldNotGiveItem() {
    PlayerMock nonOpPlayer = server.addPlayer();
    nonOpPlayer.setOp(false);

    item.onGameEnd(nonOpPlayer);

    ItemStack itemInSlot = nonOpPlayer.getInventory().getItem(8);
    assertNull(itemInSlot, "Non-OP player should not receive the nether star item after game end");
  }

  @Test
  void onGameStartAbort_withOpPlayer_shouldGiveNetherStar() {
    PlayerMock opPlayer = server.addPlayer();
    opPlayer.setOp(true);

    item.onGameStartAbort(opPlayer);

    ItemStack itemInSlot = opPlayer.getInventory().getItem(8);
    assertNotNull(itemInSlot, "OP player should receive an item in slot 8");
    assertEquals(
        Material.NETHER_STAR,
        itemInSlot.getType(),
        "OP player should receive a nether star after game start abort");
  }

  @Test
  void onGameStartAbort_withNonOpPlayer_shouldNotGiveItem() {
    PlayerMock nonOpPlayer = server.addPlayer();
    nonOpPlayer.setOp(false);

    item.onGameStartAbort(nonOpPlayer);

    ItemStack itemInSlot = nonOpPlayer.getInventory().getItem(8);
    assertNull(
        itemInSlot, "Non-OP player should not receive the nether star item after game start abort");
  }
}
