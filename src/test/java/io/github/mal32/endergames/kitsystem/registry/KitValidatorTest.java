package io.github.mal32.endergames.kitsystem.registry;

import static org.junit.jupiter.api.Assertions.*;

import io.github.mal32.endergames.BaseMockBukkitTest;
import io.github.mal32.endergames.kitsystem.api.*;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class KitValidatorTest extends BaseMockBukkitTest {
  private KitService service;

  @Override
  public void onSetUp() {
    service = new KitService(plugin, new KitManager(plugin));
  }

  @Test
  void throwsWhenIdBlank() {
    DummyKit kit = new DummyKit(" ", service, plugin);
    assertThrows(IllegalStateException.class, () -> KitValidator.validate(kit));
  }

  @Test
  void throwsWhenNamespacedKeyInvalid() {
    KitWithInvalidKey kit = new KitWithInvalidKey(service, plugin);
    assertThrows(IllegalArgumentException.class, () -> KitValidator.validate(kit));
  }

  @Test
  void throwsWhenAdvancementMissing() {
    try (MockedStatic<Bukkit> mocked = Mockito.mockStatic(Bukkit.class)) {
      mocked.when(() -> Bukkit.getAdvancement(new NamespacedKey("test", "adv"))).thenReturn(null);

      KitWithMissingAdvancement kit = new KitWithMissingAdvancement(service, plugin);
      assertThrows(IllegalArgumentException.class, () -> KitValidator.validate(kit));
    }
  }

  @Test
  void passesWhenAdvancementExists() {
    Advancement adv = Mockito.mock(Advancement.class);

    try (MockedStatic<Bukkit> mocked = Mockito.mockStatic(Bukkit.class)) {
      mocked.when(() -> Bukkit.getAdvancement(new NamespacedKey("test", "adv"))).thenReturn(adv);

      KitWithValidAdvancement kit = new KitWithValidAdvancement(service, plugin);
      assertDoesNotThrow(() -> KitValidator.validate(kit));
    }
  }

  static class KitWithInvalidKey extends DummyKit implements KitUnlockAdvancement {
    KitWithInvalidKey(KitService s, JavaPlugin p) {
      super("Test", s, p);
    }

    public String getKitAdvancementKey() {
      return "invalid key";
    }
  }

  static class KitWithMissingAdvancement extends DummyKit implements KitUnlockAdvancement {
    KitWithMissingAdvancement(KitService s, JavaPlugin p) {
      super("Test", s, p);
    }

    public String getKitAdvancementKey() {
      return "test:adv";
    }
  }

  static class KitWithValidAdvancement extends DummyKit implements KitUnlockAdvancement {
    KitWithValidAdvancement(KitService s, JavaPlugin p) {
      super("Test", s, p);
    }

    public String getKitAdvancementKey() {
      return "test:adv";
    }
  }
}
