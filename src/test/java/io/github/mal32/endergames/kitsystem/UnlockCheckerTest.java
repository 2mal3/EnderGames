package io.github.mal32.endergames.kitsystem;

import static org.junit.jupiter.api.Assertions.*;

import io.github.mal32.endergames.BaseMockBukkitTest;
import io.github.mal32.endergames.kitsystem.api.*;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class UnlockCheckerTest extends BaseMockBukkitTest {
  private KitService service;

  @Override
  public void onSetUp() {
    service = new KitService(plugin, new KitManager(plugin));
  }

  @Test
  void returnsTrueWhenNoRequirement() {
    PlayerMock player = server.addPlayer();
    assertTrue(UnlockChecker.isUnlocked(player, new NoReqKit(service, plugin)));
  }

  @Test
  void returnsFalseWhenKeyInvalid() {
    PlayerMock player = server.addPlayer();
    assertFalse(UnlockChecker.isUnlocked(player, new InvalidKeyKit(service, plugin)));
  }

  @Test
  void returnsFalseWhenAdvancementMissing() {
    PlayerMock player = server.addPlayer();

    try (MockedStatic<Bukkit> mocked = Mockito.mockStatic(Bukkit.class)) {
      mocked.when(() -> Bukkit.getAdvancement(new NamespacedKey("test", "adv"))).thenReturn(null);

      assertFalse(UnlockChecker.isUnlocked(player, new MissingAdvKit(service, plugin)));
    }
  }

  @Test
  void returnsFalseWhenAdvancementNotDone() {
    Advancement adv = Mockito.mock(Advancement.class);
    AdvancementProgress progress = Mockito.mock(AdvancementProgress.class);

    try (MockedStatic<Bukkit> mocked = Mockito.mockStatic(Bukkit.class)) {
      mocked.when(() -> Bukkit.getAdvancement(new NamespacedKey("test", "adv"))).thenReturn(adv);

      Player player = Mockito.mock(Player.class);
      Mockito.when(player.getAdvancementProgress(adv)).thenReturn(progress);
      Mockito.when(progress.isDone()).thenReturn(false);

      assertFalse(UnlockChecker.isUnlocked(player, new AdvKit(service, plugin)));
    }
  }

  @Test
  void returnsTrueWhenAdvancementDone() {
    Advancement adv = Mockito.mock(Advancement.class);
    AdvancementProgress progress = Mockito.mock(AdvancementProgress.class);

    try (MockedStatic<Bukkit> mocked = Mockito.mockStatic(Bukkit.class)) {
      mocked.when(() -> Bukkit.getAdvancement(new NamespacedKey("test", "adv"))).thenReturn(adv);

      Player player = Mockito.mock(Player.class);
      Mockito.when(player.getAdvancementProgress(adv)).thenReturn(progress);
      Mockito.when(progress.isDone()).thenReturn(true);

      assertTrue(UnlockChecker.isUnlocked(player, new AdvKit(service, plugin)));
    }
  }

  static class NoReqKit extends AbstractKit {
    NoReqKit(KitService s, JavaPlugin p) {
      super(new KitDescription("A", org.bukkit.Material.STONE, "", "", Difficulty.EASY), s, p);
    }

    @Override
    public void initPlayer(org.bukkit.entity.Player player) {}
  }

  static class InvalidKeyKit extends NoReqKit implements KitUnlockAdvancement {
    InvalidKeyKit(KitService s, JavaPlugin p) {
      super(s, p);
    }

    public String getKitAdvancementKey() {
      return "invalid key";
    }
  }

  static class MissingAdvKit extends NoReqKit implements KitUnlockAdvancement {
    MissingAdvKit(KitService s, JavaPlugin p) {
      super(s, p);
    }

    public String getKitAdvancementKey() {
      return "test:adv";
    }
  }

  static class AdvKit extends NoReqKit implements KitUnlockAdvancement {
    AdvKit(KitService s, JavaPlugin p) {
      super(s, p);
    }

    public String getKitAdvancementKey() {
      return "test:adv";
    }
  }
}
