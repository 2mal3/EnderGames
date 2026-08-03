package io.github.mal32.endergames.game.game;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.mal32.endergames.EnderGames;
import io.github.mal32.endergames.services.PlayerInWorld;
import java.lang.reflect.Method;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

class PlayerSwapManagerTest {
  private ServerMock server;
  private PlayerSwapManager manager;

  @BeforeEach
  void setUp() {
    server = MockBukkit.mock();
    EnderGames plugin = mock(EnderGames.class);
    when(plugin.getServer()).thenReturn(server);
    manager = new PlayerSwapManager(plugin);
  }

  @AfterEach
  void tearDown() {
    MockBukkit.unmock();
  }

  private void switchIntoFightProtection(Player player1, Player player2) throws Exception {
    Method method =
        PlayerSwapManager.class.getDeclaredMethod(
            "switchIntoFightProtection", Player.class, Player.class);
    method.setAccessible(true);
    method.invoke(manager, player1, player2);
  }

  private PlayerMock addGamePlayer() {
    PlayerMock player = server.addPlayer();
    PlayerInWorld.GAME.set(player);
    return player;
  }

  @Test
  void nonFightingPlayerGetsResistanceWhenSwappedWithFightingPlayer() throws Exception {
    PlayerMock fighter = addGamePlayer();
    PlayerMock bystander = addGamePlayer();

    // The fighter was recently damaged by the bystander, so only the fighter is "in a fight".
    FightDetection.fakeDamage(fighter, bystander);
    assertTrue(FightDetection.playerIsInFight(fighter));
    assertFalse(FightDetection.playerIsInFight(bystander));

    switchIntoFightProtection(fighter, bystander);

    assertTrue(
        bystander.hasPotionEffect(PotionEffectType.RESISTANCE),
        "The player who was not in a fight should receive resistance protection after the swap");
    assertFalse(
        fighter.hasPotionEffect(PotionEffectType.RESISTANCE),
        "The player who was already fighting should not receive resistance protection");
  }

  @Test
  void protectionIsSymmetricRegardlessOfArgumentOrder() throws Exception {
    PlayerMock fighter = addGamePlayer();
    PlayerMock bystander = addGamePlayer();

    FightDetection.fakeDamage(fighter, bystander);

    // Argument order must not matter: the bystander is protected either way.
    switchIntoFightProtection(bystander, fighter);

    assertTrue(bystander.hasPotionEffect(PotionEffectType.RESISTANCE));
    assertFalse(fighter.hasPotionEffect(PotionEffectType.RESISTANCE));
  }

  @Test
  void noProtectionWhenBothPlayersAreFighting() throws Exception {
    PlayerMock a = addGamePlayer();
    PlayerMock b = addGamePlayer();

    FightDetection.fakeDamage(a, b);
    FightDetection.fakeDamage(b, a);
    assertTrue(FightDetection.playerIsInFight(a));
    assertTrue(FightDetection.playerIsInFight(b));

    switchIntoFightProtection(a, b);

    assertFalse(a.hasPotionEffect(PotionEffectType.RESISTANCE));
    assertFalse(b.hasPotionEffect(PotionEffectType.RESISTANCE));
  }

  @Test
  void noProtectionWhenNeitherPlayerIsFighting() throws Exception {
    PlayerMock a = addGamePlayer();
    PlayerMock b = addGamePlayer();

    assertFalse(FightDetection.playerIsInFight(a));
    assertFalse(FightDetection.playerIsInFight(b));

    switchIntoFightProtection(a, b);

    assertFalse(a.hasPotionEffect(PotionEffectType.RESISTANCE));
    assertFalse(b.hasPotionEffect(PotionEffectType.RESISTANCE));
  }
}
